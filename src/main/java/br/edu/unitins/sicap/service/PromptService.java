package br.edu.unitins.sicap.service;

import br.edu.unitins.sicap.dto.ContextoSemantico;
import br.edu.unitins.sicap.dto.InconsistenciaRequest;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Monta o prompt estruturado em 3 blocos (seção 3.5.2 do TCC):
 * 1) papel e tarefa (fixo) 2) fontes de contexto 3) dados dinâmicos da
 * inconsistência, incluindo as regras relacionadas inferidas pela ontologia.
 */
@ApplicationScoped
public class PromptService {

    private static final String BLOCO_PAPEL_E_TAREFA = """
            Você é um especialista em contabilidade pública e regras de validação do SICAP Contábil.
            Sua tarefa é interpretar inconsistências contábeis e gerar explicações claras,
            técnicas e contextualizadas para o usuário final.

            Seu objetivo NÃO é apenas repetir a mensagem da regra, mas explicar:
            - o que aconteceu;
            - por que aconteceu;
            - quais podem ser as causas;
            - quais outras validações podem estar relacionadas;
            - quais impactos a inconsistência pode causar;
            - e como o problema pode ser corrigido.
            """;

    public String construirPrompt(InconsistenciaRequest request, ContextoSemantico contexto) {
        StringBuilder prompt = new StringBuilder();

        // Bloco 1 - papel e tarefa (fixo)
        prompt.append(BLOCO_PAPEL_E_TAREFA).append("\n\n");

        // Bloco 2 - fontes de contexto (semi-fixo, enriquecido pela ontologia)
        prompt.append("[Contexto da regra]\n");
        prompt.append("Código: ").append(contexto.codigoRegra).append("\n");
        prompt.append("Classe ontológica: ").append(contexto.classeRegra).append("\n");
        prompt.append("Descrição formal: ").append(contexto.descricaoRegra).append("\n");
        if (contexto.regrasRelacionadas != null && !contexto.regrasRelacionadas.isEmpty()) {
            prompt.append("Regras relacionadas (inferidas pela ontologia): ")
                    .append(String.join(", ", contexto.regrasRelacionadas)).append("\n");
        }
        prompt.append("\n");

        // Bloco 3 - dados dinâmicos da inconsistência recebida na requisição
        prompt.append("[Dados da inconsistência]\n");
        prompt.append("mensagemOriginal: ").append(request.mensagemOriginal).append("\n");
        prompt.append("dadosEncontrados: ").append(request.dadosEncontrados).append("\n");
        prompt.append("exercicio: ").append(request.exercicio).append("\n");
        prompt.append("bimestre: ").append(request.bimestre).append("\n");

        return prompt.toString();
    }
}
