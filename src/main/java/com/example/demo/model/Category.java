package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes = 10;

    @Column(name = "question_count")
    private Integer questionCount = 10;

    @Column(name = "passing_percentage")
    private Integer passingPercentage = 50;
}
