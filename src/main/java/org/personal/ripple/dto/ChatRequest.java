package org.personal.ripple.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank(message = "Question must not be blank")
        String question,
        Integer topK,
        Double minScore
) {
    public int effectiveTopK() {
        if (topK == null || topK <= 0) {
            return 5;
        }
        return Math.min(topK, 15);
    }

    public double effectiveMinScore() {
        if (minScore == null || minScore <= 0.0) {
            return 0.55;
        }
        return minScore;
    }
}
