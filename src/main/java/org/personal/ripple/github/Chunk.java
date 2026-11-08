package org.personal.ripple.github;

public record Chunk(String filePath, int startLine, int endLine, String content) {
}
