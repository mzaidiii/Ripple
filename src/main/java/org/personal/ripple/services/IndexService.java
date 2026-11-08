package org.personal.ripple.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.personal.ripple.entity.CodeChunk;
import org.personal.ripple.github.Chunk;
import org.personal.ripple.github.Chunker;
import org.personal.ripple.github.GitHubClient;
import org.personal.ripple.github.Hashing;
import org.personal.ripple.github.RepoFile;
import org.personal.ripple.github.RepoZipReader;
import org.personal.ripple.llm.EmbeddingClient;
import org.personal.ripple.repository.CodeChunkRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class IndexService {

    private static final int BATCH_SIZE = 20;
    private static final long PAUSE_MS = 15000;
    private static final int MAX_CHUNKS = 2000;

    private final GitHubClient gitHubClient;
    private final RepoZipReader zipReader;
    private final Chunker chunker;
    private final EmbeddingClient embeddingClient;
    private final CodeChunkRepository chunkRepository;

    public int indexRepo(String repo, String ref) {
        String sha = gitHubClient.getCommitSha(repo, ref);

        List<Chunk> chunks = new ArrayList<>();
        for (RepoFile file : zipReader.read(gitHubClient.downloadZip(repo, sha))) {
            chunks.addAll(chunker.split(file));
        }
        if (chunks.size() > MAX_CHUNKS) {
            throw new IllegalArgumentException("Repository too large to index: " + chunks.size() + " chunks");
        }

        Map<String, CodeChunk> stored = new HashMap<>();
        for (CodeChunk c : chunkRepository.findByRepoFullName(repo)) {
            stored.put(key(c.getFilePath(), c.getStartLine(), c.getContentHash()), c);
        }

        Set<String> currentKeys = new HashSet<>();
        List<NewChunk> toEmbed = new ArrayList<>();
        for (Chunk chunk : chunks) {
            String hash = Hashing.sha256(chunk.content());
            String key = key(chunk.filePath(), chunk.startLine(), hash);
            currentKeys.add(key);
            if (!stored.containsKey(key)) {
                toEmbed.add(new NewChunk(chunk, hash));
            }
        }

        log.info("Indexing {} at {}: {} chunks total, {} new", repo, sha, chunks.size(), toEmbed.size());

        for (int i = 0; i < toEmbed.size(); i += BATCH_SIZE) {
            List<NewChunk> batch = toEmbed.subList(i, Math.min(i + BATCH_SIZE, toEmbed.size()));
            List<String> texts = batch.stream().map(n -> n.chunk().content()).toList();
            List<float[]> vectors = embeddingClient.embedDocuments(texts);

            if (vectors.size() != batch.size()) {
                throw new IllegalStateException("Expected " + batch.size() + " vectors but got " + vectors.size());
            }

            List<CodeChunk> rows = new ArrayList<>();
            for (int j = 0; j < batch.size(); j++) {
                Chunk chunk = batch.get(j).chunk();
                CodeChunk row = new CodeChunk();
                row.setRepoFullName(repo);
                row.setFilePath(chunk.filePath());
                row.setStartLine(chunk.startLine());
                row.setEndLine(chunk.endLine());
                row.setContent(chunk.content());
                row.setContentHash(batch.get(j).hash());
                row.setCommitSha(sha);
                row.setEmbedding(vectors.get(j));
                rows.add(row);
            }
            chunkRepository.saveAll(rows);
            log.info("Indexed {} of {} new chunks for {}", i + batch.size(), toEmbed.size(), repo);

            if (i + BATCH_SIZE < toEmbed.size()) {
                pause();
            }
        }

        List<CodeChunk> stale = stored.entrySet().stream()
                .filter(e -> !currentKeys.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .toList();
        chunkRepository.deleteAll(stale);

        log.info("Finished indexing {}: {} embedded, {} stale removed", repo, toEmbed.size(), stale.size());
        return toEmbed.size();
    }

    private String key(String filePath, int startLine, String hash) {
        return filePath + "|" + startLine + "|" + hash;
    }

    private void pause() {
        try {
            Thread.sleep(PAUSE_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Indexing interrupted", e);
        }
    }

    private record NewChunk(Chunk chunk, String hash) {}
}