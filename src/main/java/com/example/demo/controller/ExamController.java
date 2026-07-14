package com.example.demo.controller;

import com.example.demo.dto.AnswerSubmission;
import com.example.demo.model.Exam;
import com.example.demo.model.Question;
import com.example.demo.model.User;
import com.example.demo.repository.ExamRepository;
import com.example.demo.repository.QuestionRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.example.demo.service.EmailService;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/exam")
public class ExamController {
    @Autowired
    ExamRepository examRepository;

    @Autowired
    QuestionRepository questionRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    com.example.demo.repository.CategoryRepository categoryRepository;

    @Autowired
    com.example.demo.repository.AnswerRepository answerRepository;

    @Autowired
    EmailService emailService;

    @PostMapping("/submit")
    public ResponseEntity<?> submitExam(@RequestBody AnswerSubmission submission) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found. Please log in again."));

        int score = 0;
        List<AnswerSubmission.AnswerItem> submittedAnswers = submission.getAnswers() == null
                ? List.of()
                : submission.getAnswers();

        List<Long> submittedQuestionIds = new ArrayList<>();
        if (submission.getQuestionIds() != null && !submission.getQuestionIds().isEmpty()) {
            submittedQuestionIds.addAll(submission.getQuestionIds());
        } else {
            submittedQuestionIds.addAll(submittedAnswers.stream()
                    .map(AnswerSubmission.AnswerItem::getQuestionId)
                    .filter(id -> id != null)
                    .collect(Collectors.toList()));
        }

        submittedQuestionIds = new ArrayList<>(new LinkedHashSet<>(submittedQuestionIds));

        List<Question> questions;
        if (!submittedQuestionIds.isEmpty()) {
            questions = questionRepository.findAllById(submittedQuestionIds);
        } else if (submission.getCategoryId() != null) {
            questions = questionRepository.findByCategoryId(submission.getCategoryId());
        } else {
            questions = questionRepository.findAll();
        }

        Map<Long, String> selectedAnswersMap = submittedAnswers.stream()
                .filter(item -> item.getQuestionId() != null)
                .filter(item -> item.getSelectedAnswer() != null && !item.getSelectedAnswer().isBlank())
                .collect(Collectors.toMap(
                        AnswerSubmission.AnswerItem::getQuestionId,
                        AnswerSubmission.AnswerItem::getSelectedAnswer,
                        (existing, replacement) -> replacement));

        Exam exam = new Exam();
        exam.setUser(user);
        exam.setTotalQuestions(questions.size());
        exam.setCompletedAt(LocalDateTime.now());
        if (submission.getCategoryId() != null) {
            exam.setCategory(categoryRepository.findById(submission.getCategoryId()).orElse(null));
        }

        List<com.example.demo.model.Answer> savedAnswers = new ArrayList<>();
        for (Question question : questions) {
            String selectedAnswer = selectedAnswersMap.get(question.getId());
            if (selectedAnswer == null || selectedAnswer.isBlank()) {
                continue;
            }

            if (question.getCorrectAnswer().equals(selectedAnswer)) {
                score++;
            }

            com.example.demo.model.Answer answer = new com.example.demo.model.Answer();
            answer.setExam(exam);
            answer.setQuestion(question);
            answer.setUser(user);
            answer.setSelectedAnswer(selectedAnswer);
            savedAnswers.add(answer);
        }

        exam.setScore(score);
        exam.setAnswers(savedAnswers);
        Exam savedExam = examRepository.save(exam);

        final int finalScore = score;
        String categoryName = savedExam.getCategory() != null ? savedExam.getCategory().getName() : "General Assessment";
        emailService.sendResultEmail(user.getEmail(), user.getName(), categoryName, finalScore, savedExam.getTotalQuestions(), savedAnswers)
            .thenAccept(sent -> {
                if (sent) {
                    System.out.println("[EMAIL] Result email sent successfully to: " + user.getEmail());
                } else {
                    System.err.println("[EMAIL] Failed to send result email to: " + user.getEmail());
                }
            });

        return ResponseEntity.ok(savedExam);
    }

    @GetMapping("/result/{userId}")
    @PreAuthorize("#userId == principal.id or hasRole('ADMIN')")
    public List<Exam> getResults(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return examRepository.findByUserOrderByCompletedAtDesc(user);
    }
    
    @GetMapping("/results/all")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Exam> getAllResults() {
        return examRepository.findAll();
    }
}
