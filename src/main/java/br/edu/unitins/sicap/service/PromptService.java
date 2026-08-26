package br.edu.unitins.sicap.service;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import br.edu.unitins.sicap.dto.ContextoSemantico;
import br.edu.unitins.sicap.dto.InconsistenciaRequest;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Monta o prompt estruturado em 3 blocos (seção 3.5.2 do TCC):
 * 1) papel e tarefa (fixo) 2) fontes de contexto 3) dados dinâmicos da
 * inconsistência, incluindo as regras relacionadas inferidas pela ontologia.
 *
 * Inclui também um bloco de restrição anti-alucinação (lista fechada de
 * regras válidas), adicionado depois de observar que modelos menores
 * (ex.: llama3.2 3B) tendem a inventar códigos de regra inexistentes
 * (ex.: "BV35") quando não recebem esse limite explícito.
 */
@ApplicationScoped
public class PromptService {

    // Escopo fechado da prova de conceito (seção 4.3 do TCC I): só estas
    // 3 regras estão modeladas na ontologia. Ajuste aqui se o escopo for
    // ampliado no TCC II.
    @ConfigProperty(name = "sicap.regras.validas", defaultValue = "BV3, BV29, BV32")
    String regrasValidas;

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

        // Bloco 2.1 - restrição anti-alucinação
        prompt.append("[Restrição importante]\n");
        prompt.append("As únicas regras de validação existentes neste sistema são: ")
                .append(regrasValidas).append(".\n");
        prompt.append("NÃO mencione, cite ou invente nenhum código de regra fora dessa lista, ")
                .append("mesmo que pareça plausível. Se não tiver certeza sobre alguma informação, ")
                .append("diga explicitamente que não possui dados suficientes em vez de inventar. ")
                .append("Use apenas terminologia de contabilidade PÚBLICA MUNICIPAL brasileira ")
                .append("(ex.: TCE, prestação de contas, Lei 4.320/1964, MCASP) - não use vocabulário ")
                .append("de contabilidade empresarial/privada (ex.: investidores, acionistas).\n\n");

        // Bloco 3 - dados dinâmicos da inconsistência recebida na requisição
        prompt.append("[Dados da inconsistência]\n");
        prompt.append("mensagemOriginal: ").append(request.mensagemOriginal).append("\n");
        prompt.append("dadosEncontrados: ").append(request.dadosEncontrados).append("\n");
        prompt.append("exercicio: ").append(request.exercicio).append("\n");
        prompt.append("bimestre: ").append(request.bimestre).append("\n");

        return prompt.toString();
    }
}