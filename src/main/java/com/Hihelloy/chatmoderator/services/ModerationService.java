package com.Hihelloy.chatmoderator.services;

import com.Hihelloy.chatmoderator.ChatModeratorPlugin;
import com.Hihelloy.chatmoderator.config.ConfigManager;
import com.Hihelloy.chatmoderator.utils.ModerationResult;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class ModerationService {

    private final ChatModeratorPlugin plugin;
    private final ConfigManager configManager;

    private Client geminiClient;
    private OkHttpClient httpClient;

    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    public ModerationService(ChatModeratorPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        initClients();
    }

    public void initClients() {
        String provider = configManager.getPreferredAIProvider();

        this.geminiClient = null;
        this.httpClient = null;

        if ("gemini".equalsIgnoreCase(provider)) {
            String apiKey = configManager.getGeminiApiKey();
            if (apiKey != null && !apiKey.isEmpty() && !apiKey.equals("your-gemini-api-key-here")) {
                this.geminiClient = Client.builder().apiKey(apiKey).build();
            }
        } else if ("openai".equalsIgnoreCase(provider)) {
            String apiKey = configManager.getOpenAIApiKey();
            if (apiKey != null && !apiKey.isEmpty() && !apiKey.equals("your-openai-api-key-here")) {
                this.httpClient = new OkHttpClient();
            }
        }
    }

    public CompletableFuture<ModerationResult> checkAIModerationAsync(String message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String provider = configManager.getPreferredAIProvider();

                if ("gemini".equalsIgnoreCase(provider) && geminiClient != null) {
                    return checkWithGemini(message);
                } else if ("openai".equalsIgnoreCase(provider) && httpClient != null) {
                    return checkWithOpenAI(message);
                }

                return checkRules(message);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "[ChatMod] AI moderation call failed", e);
                return checkRules(message);
            }
        });
    }

    private ModerationResult checkWithGemini(String message) {
        String prompt =
                "Classify this chat message for content moderation. " +
                        "Reply with EXACTLY one word — no punctuation, no explanation.\n\n" +
                        "Labels:\n" +
                        "  SAFE       – nothing harmful\n" +
                        "  HATE       – hate speech or slurs\n" +
                        "  SEXUAL     – sexual or explicit content\n" +
                        "  VIOLENCE   – threats or graphic violence\n" +
                        "  SELF_HARM  – self-harm or suicide\n\n" +
                        "Message: \"" + message + "\"";

        try {
            GenerateContentResponse response = geminiClient.models
                    .generateContent(configManager.getGeminiModel(), prompt, null);

            if (response != null && response.text() != null) {
                String label = response.text().trim().toUpperCase(Locale.ROOT);

                switch (label) {
                    case "HATE":
                        return ModerationResult.block("AI (Gemini) flagged: HATE",
                                ModerationResult.ViolationType.HATE_SPEECH);
                    case "SEXUAL":
                        return ModerationResult.block("AI (Gemini) flagged: SEXUAL",
                                ModerationResult.ViolationType.SEXUAL);
                    case "VIOLENCE":
                        return ModerationResult.block("AI (Gemini) flagged: VIOLENCE",
                                ModerationResult.ViolationType.VIOLENCE);
                    case "SELF_HARM":
                        return ModerationResult.block("AI (Gemini) flagged: SELF_HARM",
                                ModerationResult.ViolationType.SELF_HARM);
                    case "SAFE":
                    default:
                        return ModerationResult.safe();
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[ChatMod] Gemini call failed, falling back to rules", e);
        }

        return checkRules(message);
    }

    private ModerationResult checkWithOpenAI(String message) {
        String apiKey = configManager.getOpenAIApiKey();
        String model = configManager.getOpenAIModel();
        Map<String, Double> thresholds = configManager.getModerationThresholds();

        JsonObject body = new JsonObject();
        body.addProperty("input", message);
        body.addProperty("model", model);

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/moderations")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON_TYPE))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                plugin.getLogger().warning("[ChatMod] OpenAI moderation returned HTTP " + response.code());
                return checkRules(message);
            }

            String responseBody = response.body() != null ? response.body().string() : "";
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

            JsonArray results = json.getAsJsonArray("results");
            if (results == null || results.size() == 0) return checkRules(message);

            JsonObject result = results.get(0).getAsJsonObject();
            boolean flagged = result.get("flagged").getAsBoolean();

            if (!flagged) return ModerationResult.safe();

            JsonObject categories = result.getAsJsonObject("categories");
            JsonObject categoryScores = result.getAsJsonObject("category_scores");

            String triggeredCategory = "policy violation";
            ModerationResult.ViolationType type = ModerationResult.ViolationType.WORD_FILTER;

            for (String cat : new String[]{
                    "hate", "harassment", "violence", "self-harm",
                    "sexual", "hate/threatening", "self-harm/intent",
                    "self-harm/instructions", "sexual/minors", "violence/graphic"}) {

                if (!categories.has(cat) || !categories.get(cat).getAsBoolean()) continue;

                double score = categoryScores.has(cat) ? categoryScores.get(cat).getAsDouble() : 1.0;
                double threshold = thresholds.getOrDefault(cat, 0.5);

                if (score >= threshold) {
                    triggeredCategory = cat;
                    type = mapCategory(cat);
                    break;
                }
            }

            return ModerationResult.block("AI (OpenAI) flagged: " + triggeredCategory, type);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "[ChatMod] OpenAI call failed, falling back to rules", e);
            return checkRules(message);
        }
    }

    private ModerationResult.ViolationType mapCategory(String cat) {
        switch (cat) {
            case "hate":
            case "hate/threatening":
                return ModerationResult.ViolationType.HATE_SPEECH;
            case "sexual":
            case "sexual/minors":
                return ModerationResult.ViolationType.SEXUAL;
            case "violence":
            case "violence/graphic":
                return ModerationResult.ViolationType.VIOLENCE;
            case "self-harm":
            case "self-harm/intent":
            case "self-harm/instructions":
                return ModerationResult.ViolationType.SELF_HARM;
            case "harassment":
                return ModerationResult.ViolationType.HARASSMENT;
            default:
                return ModerationResult.ViolationType.WORD_FILTER;
        }
    }

    private ModerationResult checkRules(String message) {
        String lower = message.toLowerCase(Locale.ROOT);

        if (phraseMatch(lower, "kys")
                || phraseMatch(lower, "kill yourself")
                || phraseMatch(lower, "gfys")) {
            return ModerationResult.block("Rule: self-harm/violence phrase detected",
                    ModerationResult.ViolationType.SELF_HARM);
        }

        if (wordMatch(lower, "nigger") || wordMatch(lower, "nigga")
                || wordMatch(lower, "faggot") || wordMatch(lower, "fag")
                || wordMatch(lower, "retard") || wordMatch(lower, "sped")
                || wordMatch(lower, "negro") || wordMatch(lower, "ngr")
                || wordMatch(lower, "nga")) {
            return ModerationResult.block("Rule: hate-speech word detected",
                    ModerationResult.ViolationType.HATE_SPEECH);
        }

        if (wordMatch(lower, "sex") || phraseMatch(lower, "blow job")
                || wordMatch(lower, "blowjob") || wordMatch(lower, "cum")
                || wordMatch(lower, "rape") || wordMatch(lower, "horny")
                || wordMatch(lower, "nude") || wordMatch(lower, "naked")
                || wordMatch(lower, "pubes") || phraseMatch(lower, "blow me")
                || phraseMatch(lower, "im horny")) {
            return ModerationResult.block("Rule: sexual content detected",
                    ModerationResult.ViolationType.SEXUAL);
        }

        return ModerationResult.safe();
    }

    private boolean wordMatch(String text, String word) {
        int idx = text.indexOf(word);
        while (idx != -1) {
            boolean startOk = (idx == 0) || !Character.isLetterOrDigit(text.charAt(idx - 1));
            boolean endOk = (idx + word.length() == text.length())
                    || !Character.isLetterOrDigit(text.charAt(idx + word.length()));
            if (startOk && endOk) return true;
            idx = text.indexOf(word, idx + 1);
        }
        return false;
    }

    private boolean phraseMatch(String text, String phrase) {
        return text.replace("'", "").contains(phrase);
    }
}