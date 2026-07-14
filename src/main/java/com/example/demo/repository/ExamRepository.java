package com.example.demo.repository;

import com.example.demo.model.Exam;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findByUserOrderByCompletedAtDesc(User user);
}
