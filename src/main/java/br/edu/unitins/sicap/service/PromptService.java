package br.edu.unitins.sicap.service;

import java.util.Map;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import br.edu.unitins.sicap.dto.ContextoSemantico;
import br.edu.unitins.sicap.dto.InconsistenciaRequest;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Monta o prompt estruturado em 3 blocos (seção 3.5.2 do TCC):
 * 1) papel e tarefa (fixo) 2) fontes de contexto 3) dados dinâmicos da
 * inconsistência, incluindo as regras relacionadas inferidas pela ontologia.
 *
 * Inclui um bloco de restrição anti-alucinação (lista fechada de regras
 * válidas + proibição explícita de especular sobre relações fora da lista
 * fornecida pela ontologia), adicionado após observar 3 padrões distintos de
 * alucinação nos testes: (1) invenção de código de regra inexistente,
 * (2) descrição incorreta da função de uma regra relacionada real, e
 * (3) especulação sobre relações reais-porém-inexistentes, mesmo com
 * ressalva de incerteza. As descrições das regras relacionadas agora são
 * enviadas junto ao prompt (não só o código), justamente para mitigar o
 * padrão (2).
 */
@ApplicationScoped
public class PromptService {

    // Escopo fechado da prova de conceito: regras modeladas na ontologia.
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
            prompt.append("Regras relacionadas (inferidas pela ontologia, com suas descrições oficiais):\n");
            for (Map.Entry<String, String> entrada : contexto.regrasRelacionadas.entrySet()) {
                prompt.append("- ").append(entrada.getKey()).append(": ")
                        .append(entrada.getValue()).append("\n");
            }
        } else {
            prompt.append("Regras relacionadas (inferidas pela ontologia): nenhuma.\n");
        }
        prompt.append("\n");

        // Bloco 2.1 - restrição anti-alucinação
        prompt.append("[Restrição importante]\n");
        prompt.append("As únicas regras de validação existentes neste sistema são: ")
                .append(regrasValidas).append(".\n");
        prompt.append("NÃO mencione, cite ou invente nenhum código de regra fora dessa lista, ")
                .append("mesmo que pareça plausível.\n");
        prompt.append("No item sobre 'quais outras validações podem estar relacionadas', mencione ")
                .append("APENAS os códigos que aparecem explicitamente na lista 'Regras relacionadas' ")
                .append("acima. NÃO especule sobre relações adicionais, mesmo com ressalvas como ")
                .append("'pode estar relacionado', 'possivelmente' ou 'indiretamente', se a lista ")
                .append("estiver vazia ou não contiver uma regra específica, diga explicitamente que ")
                .append("não há regras relacionadas identificadas pela ontologia, em vez de sugerir ")
                .append("outras por conta própria.\n");
        prompt.append("Ao explicar uma regra relacionada, baseie-se ESTRITAMENTE na descrição oficial ")
                .append("fornecida acima para ela, não invente nem infira uma função diferente da que ")
                .append("foi descrita. Para cada regra relacionada listada, explique também POR QUE ela ")
                .append("se relaciona com a regra principal (").append("código ").append("informado no ")
                .append("bloco [Dados da inconsistência]), comparando as duas descrições oficiais e ")
                .append("destacando o vínculo conceitual entre elas, por exemplo, se envolvem a mesma ")
                .append("entidade contábil, se uma depende do resultado da outra, se verificam aspectos ")
                .append("complementares do mesmo processo, etc. Essa explicação do vínculo deve se basear ")
                .append("EXCLUSIVAMENTE nas duas descrições fornecidas acima, não invente detalhes, ")
                .append("dados ou motivos que não decorram diretamente delas.\n");
        prompt.append("Se não tiver certeza sobre alguma informação, diga explicitamente que não possui ")
                .append("dados suficientes em vez de inventar. Use apenas terminologia de contabilidade ")
                .append("PÚBLICA MUNICIPAL brasileira (ex.: TCE, prestação de contas, Lei 4.320/1964, ")
                .append("MCASP), não use vocabulário de contabilidade empresarial/privada (ex.: ")
                .append("investidores, acionistas).\n\n");

        // Bloco 3 - dados dinâmicos da inconsistência recebida na requisição
        prompt.append("[Dados da inconsistência]\n");
        prompt.append("mensagemOriginal: ").append(request.mensagemOriginal).append("\n");
        prompt.append("dadosEncontrados: ").append(request.dadosEncontrados).append("\n");
        prompt.append("exercicio: ").append(request.exercicio).append("\n");
        prompt.append("bimestre: ").append(request.bimestre).append("\n");

        return prompt.toString();
    }
}