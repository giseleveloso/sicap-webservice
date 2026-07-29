package br.edu.unitins.sicap.service;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Serviço de IA Generativa (seção 3.5 do TCC). Por enquanto é um STUB —
 * devolve uma resposta fixa só para permitir testar o fluxo ponta a ponta
 * (API -> Serviço Ontológico -> PromptService -> aqui) antes de plugar as
 * chamadas reais aos modelos (GPT, Gemini, Grok via API; LLaMA/Mistral via
 * Ollama local), que é a Etapa 6 do roteiro combinado.
 *
 * Quando for integrar de verdade, troque o corpo de gerarExplicacao() por
 * uma chamada HTTP (java.net.http.HttpClient, por exemplo) ao endpoint do
 * modelo desejado, usando este mesmo prompt como "user message".
 */
@ApplicationScoped
public class LlmService {

    private static final String MODELO_STUB = "stub-local";

    public String getModeloUtilizado() {
        return MODELO_STUB;
    }

    public String gerarExplicacao(String prompt) {
        // TODO (Etapa 6): substituir pelo cliente HTTP real do modelo escolhido.
        return "[STUB] Explicação simulada gerada a partir do prompt abaixo:\n\n" + prompt;
    }
}
