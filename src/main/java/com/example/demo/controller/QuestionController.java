package com.example.demo.controller;

import com.example.demo.model.Question;
import com.example.demo.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/questions")
public class QuestionController {
    @Autowired
    QuestionRepository questionRepository;

    @GetMapping
    public List<Question> getAllQuestions() {
        return questionRepository.findAll();
    }

    @GetMapping("/category/{categoryId}")
    public List<Question> getQuestionsByCategory(@PathVariable Long categoryId) {
        return questionRepository.findByCategoryId(categoryId);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addQuestion(@RequestBody Question question) {
        questionRepository.save(question);
        return ResponseEntity.ok("Question added successfully!");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateQuestion(@PathVariable Long id, @RequestBody Question questionDetails) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found with id: " + id));

        question.setQuestion(questionDetails.getQuestion());
        question.setOption1(questionDetails.getOption1());
        question.setOption2(questionDetails.getOption2());
        question.setOption3(questionDetails.getOption3());
        question.setOption4(questionDetails.getOption4());
        question.setCorrectAnswer(questionDetails.getCorrectAnswer());
        question.setCategory(questionDetails.getCategory());

        questionRepository.save(question);
        return ResponseEntity.ok("Question updated successfully!");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteQuestion(@PathVariable Long id) {
        questionRepository.deleteById(id);
        return ResponseEntity.ok("Question deleted successfully!");
    }
}
