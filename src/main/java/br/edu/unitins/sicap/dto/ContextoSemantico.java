package br.edu.unitins.sicap.dto;

import java.util.Map;

/**
 * Contexto semântico retornado pelo Serviço Ontológico após a inferência do
 * reasoner sobre a regra informada. É esse objeto que o PromptService vai
 * consumir para montar o prompt estruturado enviado ao LLM.
 */
public class ContextoSemantico {

    /** Código da regra consultada (ex.: "BV3"). */
    public String codigoRegra;

    /** Descrição formal da regra, extraída da propriedade descricaoRegra do indivíduo. */
    public String descricaoRegra;

    /**
     * Regras relacionadas, inferidas pelo reasoner (relacionadaCom), com
     * código -> descrição de cada uma. Enviar a descrição junto (não só o
     * código) é o que permite ao LLM explicar a relação com precisão, em vez
     * de "adivinhar" com base no nome/prefixo do código.
     */
    public Map<String, String> regrasRelacionadas;

    /** Nome da classe OWL à qual a regra pertence (ex.: RegraEquilibrioMovimento). */
    public String classeRegra;

    public ContextoSemantico() {
    }

    public ContextoSemantico(String codigoRegra, String descricaoRegra,
                              Map<String, String> regrasRelacionadas, String classeRegra) {
        this.codigoRegra = codigoRegra;
        this.descricaoRegra = descricaoRegra;
        this.regrasRelacionadas = regrasRelacionadas;
        this.classeRegra = classeRegra;
    }
}