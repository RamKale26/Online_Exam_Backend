package com.example.demo.controller;

import com.example.demo.dto.JwtResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.SignupRequest;
import com.example.demo.model.SystemSetting;
import com.example.demo.model.User;
import com.example.demo.repository.SystemSettingRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtUtils;
import com.example.demo.security.UserDetailsImpl;
import com.example.demo.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collections;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    SystemSettingRepository systemSettingRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    EmailService emailService;

    // OTP storage with expiry: email -> {otp, expiryTime}
    private static class OtpEntry {
        final String otp;
        final long expiryTime;
        OtpEntry(String otp) {
            this.otp = otp;
            this.expiryTime = System.currentTimeMillis() + 5 * 60 * 1000; // 5 minutes
        }
        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
    private final Map<String, OtpEntry> otpStorage = new ConcurrentHashMap<>();

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> request) {

        try {
            String email = request.get("email");

            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Email is required");
            }

            // Verify user exists before sending OTP
            if (!userRepository.existsByEmail(email)) {
                return ResponseEntity.badRequest().body("Error: No account found with this email!");
            }

            String otp = String.format("%06d", new Random().nextInt(999999));
            otpStorage.put(email, new OtpEntry(otp));

            // Send OTP asynchronously via @Async (proper Spring thread pool)
            emailService.sendOtp(email, otp);

            // Never return the OTP in the response for security
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("message", "OTP sent to your email address. Valid for 5 minutes.");
            return ResponseEntity.ok(responseBody);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body("OTP Error: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(
            @Valid @RequestBody LoginRequest loginRequest) {

        String email = loginRequest.getEmail();
        String otp = loginRequest.getOtp();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Error: User not found!"));

        String requestedRole = loginRequest.getRole();

        if (requestedRole != null && !requestedRole.isBlank()) {

            User.Role selectedRole =
                    requestedRole.equalsIgnoreCase("ADMIN")
                            ? User.Role.ADMIN
                            : User.Role.USER;

            if (user.getRole() != selectedRole) {
                return ResponseEntity.status(403)
                        .body("Error: Please use the correct login role for this account.");
            }
        }

        Authentication authentication;

        if (user.getRole() == User.Role.USER) {
            // USER role: authenticate via OTP only (no password required)
            OtpEntry entry = otpStorage.get(email);

            if (entry == null) {
                return ResponseEntity.badRequest().body("Error: No OTP found. Please request a new OTP.");
            }
            if (entry.isExpired()) {
                otpStorage.remove(email);
                return ResponseEntity.badRequest().body("Error: OTP has expired. Please request a new OTP.");
            }
            if (otp == null || !otp.equals(entry.otp)) {
                return ResponseEntity.badRequest().body("Error: Invalid OTP!");
            }

            otpStorage.remove(email);

            // Build authentication manually since USER authenticates via OTP, not password
            UserDetailsImpl userDetailsForOtp = UserDetailsImpl.build(user);
            authentication = new UsernamePasswordAuthenticationToken(
                    userDetailsForOtp,
                    null,
                    userDetailsForOtp.getAuthorities());

        } else {
            // ADMIN role: authenticate via password
            String password = loginRequest.getPassword();
            if (password == null || password.isBlank()) {
                return ResponseEntity.badRequest()
                        .body("Error: Password is required for admin login!");
            }

            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()));
        }

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails =
                (UserDetailsImpl) authentication.getPrincipal();

        List<String> roles = userDetails.getAuthorities()
                .stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                new JwtResponse(
                        jwt,
                        userDetails.getId(),
                        userDetails.getName(),
                        userDetails.getEmail(),
                        roles));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(
            @Valid @RequestBody SignupRequest signUpRequest) {

        SystemSetting settings =
                systemSettingRepository.findAll()
                        .stream()
                        .findFirst()
                        .orElse(null);

        if (settings != null && !settings.getAllowRegistration()) {

            return ResponseEntity.badRequest()
                    .body("Error: Registration is currently disabled!");
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {

            return ResponseEntity.badRequest()
                    .body("Error: Email is already in use!");
        }

        User user = new User();

        user.setName(signUpRequest.getName());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(
                encoder.encode(signUpRequest.getPassword()));

        String strRole = signUpRequest.getRole();

        if (strRole == null ||
                strRole.equalsIgnoreCase("USER")) {

            user.setRole(User.Role.USER);

        } else {

            user.setRole(User.Role.ADMIN);
        }

        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully!");
    }

    @GetMapping("/users")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public List<User> getAllUsers() {

        return userRepository.findAll();
    }
}