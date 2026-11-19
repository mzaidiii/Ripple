package org.personal.ripple.dto;

public record Citation(
        String filePath,
        int startLine,
        int endLine,
        double score,
        String snippetPreview
) {}
