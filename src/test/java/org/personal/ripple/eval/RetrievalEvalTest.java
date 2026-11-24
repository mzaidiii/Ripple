package org.personal.ripple.eval;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.personal.ripple.llm.EmbeddingClient;
import org.personal.ripple.repository.ChunkHit;
import org.personal.ripple.repository.CodeChunkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Run with:  RUN_EVAL=true ./mvnw test -Dtest=RetrievalEvalTest
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RUN_EVAL", matches = "true")
class RetrievalEvalTest {

    // the repo you indexed through Ripple, as owner/name
    static final String REPO = "spring-projects/spring-petclinic";

    static final int MAX_K = 10;
    static final int[] TOP_KS = {3, 5, 10};
    static final double[] MIN_SCORES = {0.45, 0.55, 0.65};
    static final long PAUSE_MS = 700; // pace embedding calls for the API rate limit

    record Question(String text, List<String> expectedFiles) {}

    @Autowired EmbeddingClient embeddingClient;
    @Autowired CodeChunkRepository chunkRepo;

    List<ChunkHit> search(String question) {
        float[] vector = embeddingClient.embedQuery(question);
        return chunkRepo.findNearestHits(REPO, Arrays.toString(vector), MAX_K);
    }

    @Test
    void evaluate() throws Exception {
        List<Question> answerable = new ArrayList<>();
        List<String> offTopic = new ArrayList<>();
        loadQuestions(answerable, offTopic);

        // embed + search each question once, then slice results in memory per config
        List<List<ChunkHit>> answerableHits = new ArrayList<>();
        for (Question q : answerable) {
            answerableHits.add(search(q.text()));
            Thread.sleep(PAUSE_MS);
        }
        List<List<ChunkHit>> offTopicHits = new ArrayList<>();
        for (String q : offTopic) {
            offTopicHits.add(search(q));
            Thread.sleep(PAUSE_MS);
        }

        System.out.printf("%nRepo: %s | %d answerable, %d off-topic%n", REPO,
                answerable.size(), offTopic.size());
        System.out.println("topK | minScore | hit@1  | hit@5  | hit@topK | MRR   | off-topic refused");
        System.out.println("-----+----------+--------+--------+----------+-------+------------------");

        for (int k : TOP_KS) {
            for (double min : MIN_SCORES) {
                int hit1 = 0, hit5 = 0, hitK = 0;
                double rrSum = 0;

                for (int i = 0; i < answerable.size(); i++) {
                    List<ChunkHit> kept = answerableHits.get(i).stream()
                            .filter(h -> h.getScore() >= min)
                            .limit(k)
                            .toList();

                    int rank = firstRank(kept, answerable.get(i).expectedFiles()); // 0 = miss
                    if (rank == 1) hit1++;
                    if (rank >= 1 && rank <= 5) hit5++;
                    if (rank >= 1) { hitK++; rrSum += 1.0 / rank; }
                }

                int refused = 0;
                for (List<ChunkHit> hits : offTopicHits) {
                    boolean anyPasses = hits.stream().limit(k).anyMatch(h -> h.getScore() >= min);
                    if (!anyPasses) refused++;
                }

                int n = answerable.size();
                System.out.printf("%4d | %8.2f | %5.1f%% | %5.1f%% | %7.1f%% | %.3f | %d/%d%n",
                        k, min, 100.0 * hit1 / n, 100.0 * hit5 / n, 100.0 * hitK / n,
                        rrSum / n, refused, offTopic.size());
            }
        }
    }

    static int firstRank(List<ChunkHit> hits, List<String> expectedFiles) {
        for (int i = 0; i < hits.size(); i++) {
            String path = hits.get(i).getFilePath();
            for (String exp : expectedFiles) {
                if (path.endsWith(exp)) return i + 1;
            }
        }
        return 0;
    }

    // questions.txt format:
    //   question text | ExpectedFile.java, OtherFile.java
    //   [OFFTOPIC]            <- everything after this line is an off-topic question, one per line
    //   lines starting with # and blank lines are ignored
    void loadQuestions(List<Question> answerable, List<String> offTopic) throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/eval/questions.txt")) {
            if (in == null) throw new IllegalStateException("src/test/resources/eval/questions.txt not found");
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            boolean off = false;
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.equals("[OFFTOPIC]")) { off = true; continue; }
                if (off) { offTopic.add(line); continue; }
                int bar = line.lastIndexOf('|');
                String text = line.substring(0, bar).trim();
                List<String> files = Arrays.stream(line.substring(bar + 1).split(","))
                        .map(String::trim).filter(s -> !s.isEmpty()).toList();
                answerable.add(new Question(text, files));
            }
        }
    }
}