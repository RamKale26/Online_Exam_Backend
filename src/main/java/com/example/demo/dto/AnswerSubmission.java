package com.example.demo.dto;

import lombok.Data;
import java.util.List;

@Data
public class AnswerSubmission {
    private Long categoryId;
    private List<Long> questionIds;
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {
        private Long questionId;
        private String selectedAnswer;
    }
}
