package com.example.demo.controller;

import com.example.demo.model.SystemSetting;
import com.example.demo.repository.SystemSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    @Autowired
    SystemSettingRepository systemSettingRepository;

    @GetMapping
    public ResponseEntity<SystemSetting> getSettings() {
        SystemSetting settings = systemSettingRepository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    SystemSetting defaultSettings = new SystemSetting();
                    defaultSettings.setAllowRegistration(true);
                    defaultSettings.setShowAnswersToStudents(true);
                    return systemSettingRepository.save(defaultSettings);
                });
        return ResponseEntity.ok(settings);
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SystemSetting> updateSettings(@RequestBody SystemSetting settingsDetails) {
        SystemSetting settings = systemSettingRepository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    SystemSetting defaultSettings = new SystemSetting();
                    defaultSettings.setAllowRegistration(true);
                    defaultSettings.setShowAnswersToStudents(true);
                    return systemSettingRepository.save(defaultSettings);
                });

        if (settingsDetails.getAllowRegistration() != null) {
            settings.setAllowRegistration(settingsDetails.getAllowRegistration());
        }
        if (settingsDetails.getShowAnswersToStudents() != null) {
            settings.setShowAnswersToStudents(settingsDetails.getShowAnswersToStudents());
        }

        SystemSetting updatedSettings = systemSettingRepository.save(settings);
        return ResponseEntity.ok(updatedSettings);
    }
}
