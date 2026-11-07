package org.personal.ripple.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class GeminiEmbeddingClient implements EmbeddingClient {

    private final RestClient restClient;
    private final String model;
    private final int dimensions;

    public GeminiEmbeddingClient(@Value("${gemini.api-key}") String apiKey,
                                 @Value("${gemini.embedding-model}") String model,
                                 @Value("${gemini.embedding-dimensions}") int dimensions) {
        this.model = model;
        this.dimensions = dimensions;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(60000);

        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .defaultHeader("x-goog-api-key", apiKey)
                .requestFactory(factory)
                .build();
    }


    @Override
    public List<float[]> embedDocuments(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        List<Map<String, Object>> requests = texts.stream()
                .map(t -> Map.<String, Object>of(
                        "model", "models/" + model,
                        "content", Map.of("parts", List.of(Map.of("text", t))),
                        "taskType", "RETRIEVAL_DOCUMENT",
                        "outputDimensionality", dimensions
                ))
                .toList();

        Map<String, Object> body = Map.of("requests", requests);

        BatchResponse response = restClient.post()
                .uri("/models/" + model + ":batchEmbedContents")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(BatchResponse.class);

        return response.embeddings().stream().map(Embedding::values).toList();
    }

    @Override
    public float[] embedQuery(String text) {
        Map<String, Object> body = Map.of(
                "model", "models/" + model,
                "content", Map.of("parts", List.of(Map.of("text", text))),
                "taskType", "RETRIEVAL_QUERY",
                "outputDimensionality", dimensions
        );

        EmbedResponse response = restClient.post()
                .uri("/models/" + model + ":embedContent")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(EmbedResponse.class);

        return response.embedding().values();
    }

    private record Embedding(float[] values) {}
    private record EmbedResponse(Embedding embedding) {}
    private record BatchResponse(List<Embedding> embeddings) {}
}