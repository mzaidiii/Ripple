package org.personal.ripple.cli;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Scanner;

public class RippleChatCli {

    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final String RED = "\u001B[31m";
    private static final String GRAY = "\u001B[90m";

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private int topK = 5;
    private double minScore = 0.55;

    public RippleChatCli(String baseUrl) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper();
    }

    public static void main(String[] args) {
        String baseUrl = System.getenv().getOrDefault("RIPPLE_SERVER_URL", "http://localhost:8080");
        String repo = args.length > 0 ? args[0].trim() : null;

        RippleChatCli cli = new RippleChatCli(baseUrl);
        cli.run(repo);
    }

    public void run(String targetRepo) {
        Scanner scanner = new Scanner(System.in);
        printBanner();

        String repo = targetRepo;
        while (repo == null || repo.isBlank()) {
            System.out.print(CYAN + "Enter repository in owner/name format (e.g. octocat/Hello-World): " + RESET);
            if (!scanner.hasNextLine()) {
                return;
            }
            repo = scanner.nextLine().trim();
        }

        System.out.println(GRAY + "Connecting to Ripple server at " + baseUrl + "..." + RESET);
        if (!checkServerHealth()) {
            System.out.println(RED + "Cannot connect to Ripple server at " + baseUrl + "." + RESET);
            System.out.println(YELLOW + "Make sure Ripple is running ('./mvnw spring-boot:run')." + RESET);
            return;
        }

        ensureRepoReady(scanner, repo);

        System.out.println("\n" + GREEN + BOLD + "Ready! Ask any question about " + repo + "." + RESET);
        printHelp();

        while (true) {
            System.out.print("\n" + BOLD + "[" + repo + "] > " + RESET);
            if (!scanner.hasNextLine()) {
                break;
            }
            String line = scanner.nextLine().trim();
            if (line.isBlank()) {
                continue;
            }

            if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit") || line.equalsIgnoreCase(":q")) {
                System.out.println(GRAY + "Goodbye!" + RESET);
                break;
            } else if (line.equalsIgnoreCase(":help")) {
                printHelp();
            } else if (line.equalsIgnoreCase(":status")) {
                displayRepoStatus(repo);
            } else if (line.equalsIgnoreCase(":index")) {
                triggerIndex(scanner, repo);
            } else if (line.toLowerCase().startsWith(":topk")) {
                handleTopK(line);
            } else if (line.toLowerCase().startsWith(":minscore")) {
                handleMinScore(line);
            } else {
                askQuestion(repo, line);
            }
        }
    }

    private void printBanner() {
        System.out.println(CYAN + BOLD + """
        ============================================================
                       R I P P L E   C H A T   C L I
             Semantic Codebase Intelligence & Retrieval (RAG)
        ============================================================
        """ + RESET);
    }

    private void printHelp() {
        System.out.println(GRAY + "Commands:\n" +
                "  :status          - Check indexing status and commit SHA of this repo\n" +
                "  :index           - Trigger background indexing for this repo\n" +
                "  :topk <n>        - Set max number of chunks to retrieve (current: " + topK + ")\n" +
                "  :minscore <f>    - Set minimum similarity cutoff (current: " + minScore + ")\n" +
                "  :help            - Show this command reference\n" +
                "  exit / quit      - Exit the CLI\n" + RESET);
    }

    private boolean checkServerHealth() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/actuator/health"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private void ensureRepoReady(Scanner scanner, String repo) {
        JsonNode statusNode = getRepoStatus(repo);
        if (statusNode == null) {
            System.out.println(YELLOW + "Repository '" + repo + "' is untracked." + RESET);
            System.out.print("Would you like to trigger indexing now? (y/N): ");
            String ans = scanner.nextLine().trim();
            if (ans.equalsIgnoreCase("y") || ans.equalsIgnoreCase("yes")) {
                triggerIndex(scanner, repo);
            }
            return;
        }

        String status = statusNode.path("status").asText("UNKNOWN");
        if ("READY".equalsIgnoreCase(status)) {
            System.out.println(GREEN + "[✓] Repository status: READY (SHA: " + statusNode.path("lastIndexedSha").asText("N/A") + ")" + RESET);
        } else if ("INDEXING".equalsIgnoreCase(status)) {
            System.out.println(YELLOW + "[*] Repository is currently indexing. Waiting for completion..." + RESET);
            pollUntilReady(repo);
        } else {
            System.out.println(YELLOW + "[!] Repository status is " + status + "." + RESET);
            System.out.print("Would you like to trigger re-indexing? (y/N): ");
            String ans = scanner.nextLine().trim();
            if (ans.equalsIgnoreCase("y") || ans.equalsIgnoreCase("yes")) {
                triggerIndex(scanner, repo);
            }
        }
    }

    private JsonNode getRepoStatus(String repo) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/repos/" + repo))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                return mapper.readTree(resp.body());
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void displayRepoStatus(String repo) {
        JsonNode node = getRepoStatus(repo);
        if (node == null) {
            System.out.println(YELLOW + "Repository '" + repo + "' not found." + RESET);
            return;
        }
        System.out.println("\n" + CYAN + "--- Repository Status ---" + RESET);
        System.out.println("Full Name        : " + node.path("fullName").asText());
        System.out.println("Status           : " + node.path("status").asText());
        System.out.println("Last Indexed SHA : " + node.path("lastIndexedSha").asText("N/A"));
        System.out.println("Indexed At       : " + node.path("indexedAt").asText("N/A"));
        if (!node.path("lastError").isNull() && !node.path("lastError").asText().isBlank()) {
            System.out.println(RED + "Last Error       : " + node.path("lastError").asText() + RESET);
        }
    }

    private void triggerIndex(Scanner scanner, String repo) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/repos/" + repo + "/index"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 202) {
                System.out.println(GREEN + "Indexing started in background." + RESET);
                pollUntilReady(repo);
            } else {
                System.out.println(RED + "Failed to start indexing: HTTP " + resp.statusCode() + " - " + resp.body() + RESET);
            }
        } catch (Exception e) {
            System.out.println(RED + "Error triggering index: " + e.getMessage() + RESET);
        }
    }

    private void pollUntilReady(String repo) {
        System.out.print(YELLOW + "Indexing in progress");
        for (int i = 0; i < 40; i++) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {}
            System.out.print(".");
            JsonNode node = getRepoStatus(repo);
            if (node != null) {
                String status = node.path("status").asText();
                if ("READY".equalsIgnoreCase(status)) {
                    System.out.println(" " + GREEN + "[DONE]" + RESET);
                    return;
                } else if ("FAILED".equalsIgnoreCase(status)) {
                    System.out.println(" " + RED + "[FAILED]: " + node.path("lastError").asText() + RESET);
                    return;
                }
            }
        }
        System.out.println(YELLOW + "\nStill indexing in background. You can check status later via :status" + RESET);
    }

    private void handleTopK(String line) {
        try {
            String[] parts = line.split("\\s+");
            if (parts.length > 1) {
                int val = Integer.parseInt(parts[1]);
                if (val > 0 && val <= 20) {
                    topK = val;
                    System.out.println(GREEN + "topK set to " + topK + RESET);
                    return;
                }
            }
        } catch (Exception ignored) {}
        System.out.println(RED + "Usage: :topk <1-20>" + RESET);
    }

    private void handleMinScore(String line) {
        try {
            String[] parts = line.split("\\s+");
            if (parts.length > 1) {
                double val = Double.parseDouble(parts[1]);
                if (val >= 0.0 && val <= 1.0) {
                    minScore = val;
                    System.out.println(GREEN + "minScore cutoff set to " + minScore + RESET);
                    return;
                }
            }
        } catch (Exception ignored) {}
        System.out.println(RED + "Usage: :minscore <0.0-1.0>" + RESET);
    }

    private void askQuestion(String repo, String question) {
        System.out.println(GRAY + "Searching code chunks & generating grounded answer..." + RESET);
        try {
            String requestJson = mapper.writeValueAsString(new QueryPayload(question, topK, minScore));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/repos/" + repo + "/chat"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                System.out.println(RED + "Server returned HTTP " + resp.statusCode() + ": " + resp.body() + RESET);
                return;
            }

            JsonNode responseJson = mapper.readTree(resp.body());
            boolean contextFound = responseJson.path("contextFound").asBoolean(false);
            String answer = responseJson.path("answer").asText();
            String model = responseJson.path("model").asText();

            System.out.println("\n" + CYAN + "─".repeat(70) + RESET);
            System.out.println(BOLD + "Answer" + RESET + GRAY + " (" + model + "):" + RESET);
            System.out.println("─".repeat(70));
            System.out.println(answer);

            JsonNode citations = responseJson.path("citations");
            if (citations.isArray() && citations.size() > 0) {
                System.out.println("\n" + BOLD + "Citations (" + citations.size() + " references):" + RESET);
                int idx = 1;
                for (JsonNode cite : citations) {
                    String file = cite.path("filePath").asText();
                    int start = cite.path("startLine").asInt();
                    int end = cite.path("endLine").asInt();
                    double score = cite.path("score").asDouble();
                    String preview = cite.path("snippetPreview").asText("");

                    System.out.printf("  %s[%d]%s %s%s:%d-%d%s %s(similarity: %.3f)%s\n",
                            CYAN, idx++, RESET,
                            BOLD, file, start, end, RESET,
                            GRAY, score, RESET);
                    if (!preview.isBlank()) {
                        System.out.println("      " + GRAY + "\"" + preview + "\"" + RESET);
                    }
                }
            } else if (!contextFound) {
                System.out.println("\n" + YELLOW + "[!] No code chunks passed the similarity cutoff of " + minScore + "." + RESET);
            }
            System.out.println(CYAN + "─".repeat(70) + RESET);

        } catch (IOException | InterruptedException e) {
            System.out.println(RED + "Failed to get answer: " + e.getMessage() + RESET);
        }
    }

    private record QueryPayload(String question, Integer topK, Double minScore) {}
}
