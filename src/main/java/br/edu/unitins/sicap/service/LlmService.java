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
 * Suporta quatro provedores, escolhidos pela propriedade "llm.provider" no
 * application.properties: openai | gemini | grok | ollama.
 *
 * openai, gemini e grok são modelos proprietários acessados via API REST;
 * ollama roda modelos open-source (LLaMA, Mistral) localmente, sem custo.
 * Isso cobre os 5 modelos citados na seção 3.6.2 do TCC (3 proprietários +
 * 2 open-source locais via Ollama, apenas trocando "ollama.model").
 *
 * Trocar de modelo = trocar "llm.provider" (e "ollama.model" se for o
 * caso) no application.properties e reiniciar. Nenhum outro código muda,
 * o que deixa a comparação entre modelos (seção 3.6.2) direta de rodar.
 */
@ApplicationScoped
public class LlmService {

    private static final Logger LOG = Logger.getLogger(LlmService.class);

    @ConfigProperty(name = "llm.provider", defaultValue = "gemini")
    String provider;

    // --- OpenAI ---
    @ConfigProperty(name = "openai.api.key")
    Optional<String> openaiApiKeyOpt;

    @ConfigProperty(name = "openai.model", defaultValue = "gpt-4o-mini")
    String openaiModel;

    @ConfigProperty(name = "openai.api.url", defaultValue = "https://api.openai.com/v1/chat/completions")
    String openaiApiUrl;

    // --- Gemini ---
    @ConfigProperty(name = "gemini.api.key")
    Optional<String> geminiApiKeyOpt;

    @ConfigProperty(name = "gemini.model", defaultValue = "gemini-3.5-flash-lite")
    String geminiModel;

    @ConfigProperty(name = "gemini.api.url", defaultValue = "https://generativelanguage.googleapis.com/v1beta/models")
    String geminiApiUrl;

    // --- Grok (xAI) ---
    // API compatível com o formato da OpenAI (mesmo corpo de requisição e
    // resposta), então reaproveitamos o mesmo parsing do gerarExplicacaoOpenAi.
    @ConfigProperty(name = "grok.api.key")
    Optional<String> grokApiKeyOpt;

    @ConfigProperty(name = "grok.model", defaultValue = "grok-4.5")
    String grokModel;

    @ConfigProperty(name = "grok.api.url", defaultValue = "https://api.x.ai/v1/chat/completions")
    String grokApiUrl;

    // --- Ollama (local, LLaMA/Mistral - sem custo, sem chave) ---
    @ConfigProperty(name = "ollama.model", defaultValue = "llama3.2")
    String ollamaModel;

    @ConfigProperty(name = "ollama.api.url", defaultValue = "http://localhost:11434/api/chat")
    String ollamaApiUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String getModeloUtilizado() {
        return switch (provider.toLowerCase()) {
            case "gemini" -> geminiModel;
            case "openai" -> openaiModel;
            case "grok" -> grokModel;
            case "ollama" -> ollamaModel;
            default -> throw new IllegalStateException("llm.provider desconhecido: " + provider);
        };
    }

    public String gerarExplicacao(String prompt) {
        return switch (provider.toLowerCase()) {
            case "gemini" -> gerarExplicacaoGemini(prompt);
            case "openai" -> gerarExplicacaoOpenAi(prompt);
            case "grok" -> gerarExplicacaoGrok(prompt);
            case "ollama" -> gerarExplicacaoOllama(prompt);
            default -> throw new IllegalStateException(
                    "llm.provider desconhecido: '" + provider + "'. Use 'openai', 'gemini', 'grok' ou 'ollama'.");
        };
    }

    // ==================== OpenAI ====================

    private String gerarExplicacaoOpenAi(String prompt) {
        String apiKey = openaiApiKeyOpt.orElse("");
        if (apiKey.isBlank()) {
            throw new IllegalStateException(
                    "openai.api.key não configurada. Defina a variável de ambiente OPENAI_API_KEY " +
                    "antes de subir a aplicação (ex.: $env:OPENAI_API_KEY = \"sk-...\" no PowerShell).");
        }
        return chamarApiEstiloOpenAi(openaiApiUrl, apiKey, openaiModel, prompt, "GPT");
    }

    // ==================== Grok (xAI) ====================

    private String gerarExplicacaoGrok(String prompt) {
        String apiKey = grokApiKeyOpt.orElse("");
        if (apiKey.isBlank()) {
            throw new IllegalStateException(
                    "grok.api.key não configurada. Defina a variável de ambiente GROK_API_KEY " +
                    "antes de subir a aplicação (ex.: $env:GROK_API_KEY = \"xai-...\" no PowerShell). " +
                    "Gere a chave em https://console.x.ai (a xAI costuma dar créditos gratuitos pra novos devs).");
        }
        return chamarApiEstiloOpenAi(grokApiUrl, apiKey, grokModel, prompt, "Grok");
    }

    /**
     * OpenAI e Grok (xAI) usam o mesmo formato de requisição/resposta
     * (chat completions), então esse método atende os dois - só muda a
     * URL, a chave e o nome do modelo.
     */
    private String chamarApiEstiloOpenAi(String apiUrl, String apiKey, String modelo, String prompt, String nomeProvedor) throws IllegalStateException {
        try {
            ObjectNode raiz = objectMapper.createObjectNode();
            raiz.put("model", modelo);
            raiz.put("temperature", 0.3);
            ArrayNode mensagens = raiz.putArray("messages");
            ObjectNode mensagemUsuario = mensagens.addObject();
            mensagemUsuario.put("role", "user");
            mensagemUsuario.put("content", prompt);

            String corpoRequisicao = objectMapper.writeValueAsString(raiz);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(corpoRequisicao))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                LOG.errorf("Erro na chamada ao %s (HTTP %d): %s", nomeProvedor, response.statusCode(), response.body());
                throw new RuntimeException("Erro ao chamar a API do " + nomeProvedor + " (HTTP "
                        + response.statusCode() + "): " + response.body());
            }

            JsonNode raizResposta = objectMapper.readTree(response.body());
            JsonNode choices = raizResposta.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new RuntimeException("Resposta da API do " + nomeProvedor + " sem 'choices': " + response.body());
            }
            return choices.get(0).path("message").path("content").asText();

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Falha ao chamar a API do " + nomeProvedor, e);
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

    // ==================== Ollama (local) ====================

    private String gerarExplicacaoOllama(String prompt) throws IllegalStateException {
        try {
            ObjectNode raiz = objectMapper.createObjectNode();
            raiz.put("model", ollamaModel);
            raiz.put("stream", false);
            ArrayNode mensagens = raiz.putArray("messages");
            ObjectNode mensagemUsuario = mensagens.addObject();
            mensagemUsuario.put("role", "user");
            mensagemUsuario.put("content", prompt);

            String corpoRequisicao = objectMapper.writeValueAsString(raiz);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaApiUrl))
                    .timeout(Duration.ofSeconds(120)) // modelos locais podem ser mais lentos
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(corpoRequisicao))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                LOG.errorf("Erro na chamada ao Ollama (HTTP %d): %s", response.statusCode(), response.body());
                throw new RuntimeException("Erro ao chamar o Ollama local (HTTP " + response.statusCode() + "): "
                        + response.body() + ". Confirme que o Ollama está rodando (comando 'ollama serve' ou o "
                        + "serviço já ativo) e que o modelo '" + ollamaModel + "' foi baixado ('ollama pull "
                        + ollamaModel + "').");
            }

            JsonNode raizResposta = objectMapper.readTree(response.body());
            JsonNode conteudo = raizResposta.path("message").path("content");
            if (conteudo.isMissingNode()) {
                throw new RuntimeException("Resposta do Ollama sem 'message.content': " + response.body());
            }
            return conteudo.asText();

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Falha ao chamar o Ollama local. Confirme que ele está instalado e rodando em "
                    + ollamaApiUrl + " (baixe em https://ollama.com se ainda não tiver).", e);
        }
    }
}