package org.personal.ripple.github;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Chunker {

    private static final int LINES_PER_CHUNK = 40;
    private static final int OVERLAP = 5;
    private static final int MAX_CHARS = 8000;

    public List<Chunk> split(RepoFile file) {
        List<Chunk> chunks = new ArrayList<>();
        List<String> lines = file.content().lines().toList();

        int start = 0;
        while (start < lines.size()) {
            int end = Math.min(start + LINES_PER_CHUNK, lines.size());
            String text = String.join("\n", lines.subList(start, end));

            if (!text.isBlank() && text.length() <= MAX_CHARS) {
                chunks.add(new Chunk(file.path(), start + 1, end, text));
            }

            if (end == lines.size()) {
                break;
            }
            start = end - OVERLAP;
        }

        return chunks;
    }
}