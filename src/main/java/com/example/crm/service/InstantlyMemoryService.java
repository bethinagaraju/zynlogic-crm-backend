package com.example.crm.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class InstantlyMemoryService {

    private final String apiKey;
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();

    private final List<JsonNode> memory = Collections.synchronizedList(new ArrayList<>());

    public InstantlyMemoryService(@Value("${instantly.api.key}") String apiKey,
            @Value("${instantly.api.base-url:https://api.instantly.ai}") String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public List<JsonNode> getMemorySnapshot() {
        synchronized (memory) {
            return new ArrayList<>(memory);
        }
    }

    public int syncAllEmails() throws Exception {
        memory.clear();

        String nextToken = null;
        int page = 0;
        do {
            page++;
            URI uri = URI.create(baseUrl + "/api/v2/emails?limit=100&email_type=received&category=others"
                    + (nextToken != null ? "&starting_after=" + nextToken : ""));

            HttpRequest req = HttpRequest.newBuilder(uri)
                    .GET()
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                throw new IllegalStateException("Instantly API returned status " + resp.statusCode() + ": "
                        + resp.body());
            }

            JsonNode root = mapper.readTree(resp.body());

            // Find first array node containing items
            JsonNode itemsNode = null;
            if (root.isArray()) {
                itemsNode = root;
            } else {
                if (root.has("data")) {
                    itemsNode = root.get("data");
                } else if (root.has("emails")) {
                    itemsNode = root.get("emails");
                } else {
                    // search for any array field
                    Iterator<JsonNode> it = root.elements();
                    while (it.hasNext()) {
                        JsonNode n = it.next();
                        if (n.isArray()) {
                            itemsNode = n;
                            break;
                        }
                    }
                }
            }

            if (itemsNode != null && itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    memory.add(item);
                }
            }

            // Look for next token in common fields
            String token = null;
            if (root.has("next_starting_after")) {
                token = root.path("next_starting_after").asText(null);
            }
            if (token == null && root.has("next")) {
                token = root.path("next").path("starting_after").asText(null);
            }
            if (token == null && root.has("starting_after")) {
                token = root.path("starting_after").asText(null);
            }

            nextToken = (token == null || token.isEmpty()) ? null : token;

            // stop if no token or no items returned
            if (nextToken == null) {
                break;
            }

        } while (true);

        return memory.size();
    }
}
