package org.personal.ripple.repository;

public interface ChunkHit {

    String getFilePath();
    int getStartLine();
    int getEndLine();
    String getContent();
    double getScore();
}
