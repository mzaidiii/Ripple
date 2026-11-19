package org.personal.ripple.dto;

import java.util.List;

public record ChatResponse(
        String answer,
        List<Citation> citations,
        String model,
        boolean contextFound
) {}
