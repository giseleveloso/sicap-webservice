package br.edu.unitins.sicap.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Serviço de IA Generativa (seção 3.5 do TCC).
 *
 * Suporta dois provedores por enquanto, escolhidos pela propriedade
 * "llm.provider" (openai | gemini) no application.properties:
 *  - OpenAI (GPT), via chat completions
 *  - Google Gemini, via generateContent (free tier no Google AI Studio)
 *
 * Isso já deixa a base pronta pra comparação entre modelos da seção 3.6.2
 * do TCC: basta trocar "llm.provider" (e reiniciar) pra rodar o mesmo caso
 * de teste em outro modelo. Os demais modelos citados no TCC (Grok,
 * LLaMA/Mistral via Ollama) seguem o mesmo padrão - um novo bloco no
 * switch de gerarExplicacao().
 */
@ApplicationScoped
public class LlmService {

    private static final Logger LOG = Logger.getLogger(LlmService.class);

    @ConfigProperty(name = "llm.provider", defaultValue = "openai")
    String provider;

    // --- OpenAI ---
    // Optional<String> porque a chave costuma estar vazia quando esse
    // provedor não está em uso (llm.provider=gemini) - String "pura" com
    // defaultValue="" faz o Quarkus falhar no startup (SRCFG00040).
    @ConfigProperty(name = "openai.api.key")
    Optional<String> openaiApiKeyOpt;

    @ConfigProperty(name = "openai.model", defaultValue = "gpt-4o-mini")
    String openaiModel;

    @ConfigProperty(name = "openai.api.url", defaultValue = "https://api.openai.com/v1/chat/completions")
    String openaiApiUrl;

    // --- Gemini ---
    // Chave gerada em https://aistudio.google.com/app/apikey (grátis, sem
    // cartão). Optional pelo mesmo motivo do openaiApiKeyOpt acima.
    @ConfigProperty(name = "gemini.api.key")
    Optional<String> geminiApiKeyOpt;

    @ConfigProperty(name = "gemini.model", defaultValue = "gemini-2.5-flash")
    String geminiModel;

    @ConfigProperty(name = "gemini.api.url", defaultValue = "https://generativelanguage.googleapis.com/v1beta/models")
    String geminiApiUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String getModeloUtilizado() {
        return switch (provider.toLowerCase()) {
            case "gemini" -> geminiModel;
            case "openai" -> openaiModel;
            default -> throw new IllegalStateException("llm.provider desconhecido: " + provider);
        };
    }

    public String gerarExplicacao(String prompt) {
        return switch (provider.toLowerCase()) {
            case "gemini" -> gerarExplicacaoGemini(prompt);
            case "openai" -> gerarExplicacaoOpenAi(prompt);
            default -> throw new IllegalStateException(
                    "llm.provider desconhecido: '" + provider + "'. Use 'openai' ou 'gemini'.");
        };
    }

    // ==================== OpenAI ====================

    private String gerarExplicacaoOpenAi(String prompt) throws IllegalStateException {
        String openaiApiKey = openaiApiKeyOpt.orElse("");
        if (openaiApiKey.isBlank()) {
            throw new IllegalStateException(
                    "openai.api.key não configurada. Defina a variável de ambiente OPENAI_API_KEY " +
                    "antes de subir a aplicação (ex.: $env:OPENAI_API_KEY = \"sk-...\" no PowerShell).");
        }

        try {
            ObjectNode raiz = objectMapper.createObjectNode();
            raiz.put("model", openaiModel);
            raiz.put("temperature", 0.3);
            ArrayNode mensagens = raiz.putArray("messages");
            ObjectNode mensagemUsuario = mensagens.addObject();
            mensagemUsuario.put("role", "user");
            mensagemUsuario.put("content", prompt);

            String corpoRequisicao = objectMapper.writeValueAsString(raiz);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(openaiApiUrl))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + openaiApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(corpoRequisicao))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                LOG.errorf("Erro na chamada ao GPT (HTTP %d): %s", response.statusCode(), response.body());
                throw new RuntimeException("Erro ao chamar a API do GPT (HTTP " + response.statusCode() + "): "
                        + response.body());
            }

            JsonNode raizResposta = objectMapper.readTree(response.body());
            JsonNode choices = raizResposta.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new RuntimeException("Resposta da API do GPT sem 'choices': " + response.body());
            }
            return choices.get(0).path("message").path("content").asText();

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Falha ao chamar a API do GPT", e);
        }
    }

    // ==================== Gemini ====================

    private String gerarExplicacaoGemini(String prompt) throws IllegalStateException {
        String geminiApiKey = geminiApiKeyOpt.orElse("");
        if (geminiApiKey.isBlank()) {
            throw new IllegalStateException(
                    "gemini.api.key não configurada. Defina a variável de ambiente GEMINI_API_KEY " +
                    "antes de subir a aplicação (ex.: $env:GEMINI_API_KEY = \"AIza...\" no PowerShell). " +
                    "Gere a chave em https://aistudio.google.com/app/apikey (grátis, sem cartão).");
        }

        try {
            ObjectNode raiz = objectMapper.createObjectNode();
            ArrayNode contents = raiz.putArray("contents");
            ObjectNode content = contents.addObject();
            ArrayNode parts = content.putArray("parts");
            ObjectNode part = parts.addObject();
            part.put("text", prompt);

            String corpoRequisicao = objectMapper.writeValueAsString(raiz);

            String url = geminiApiUrl + "/" + geminiModel + ":generateContent?key=" + geminiApiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(corpoRequisicao))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                LOG.errorf("Erro na chamada ao Gemini (HTTP %d): %s", response.statusCode(), response.body());
                throw new RuntimeException("Erro ao chamar a API do Gemini (HTTP " + response.statusCode() + "): "
                        + response.body());
            }

            JsonNode raizResposta = objectMapper.readTree(response.body());
            JsonNode candidates = raizResposta.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                throw new RuntimeException("Resposta da API do Gemini sem 'candidates': " + response.body());
            }
            return candidates.get(0).path("content").path("parts").get(0).path("text").asText();

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Falha ao chamar a API do Gemini", e);
        }
    }
}