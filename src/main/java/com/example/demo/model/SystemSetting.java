package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "system_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "allow_registration", nullable = false)
    private Boolean allowRegistration = true;

    @Column(name = "show_answers_to_students", nullable = false)
    private Boolean showAnswersToStudents = true;
}
