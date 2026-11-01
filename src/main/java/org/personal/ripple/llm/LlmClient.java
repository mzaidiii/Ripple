package org.personal.ripple.llm;

public interface LlmClient {
    String summarise(String diff);
    String modelName();
}
