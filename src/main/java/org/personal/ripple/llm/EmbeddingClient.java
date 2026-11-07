package org.personal.ripple.llm;

import java.util.List;

public interface EmbeddingClient {
    List<float[]> embedDocuments(List<String> text);
    float[] embedQuery(String text);
}
