package br.edu.unitins.sicap.dto;

import java.util.List;

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
     * Códigos das regras relacionadas, inferidas pelo reasoner a partir das
     * propriedades relacionadaCom / implicaViolacaoDe (ex.: BV29 -> [BV3]).
     */
    public List<String> regrasRelacionadas;

    /** Nome da classe OWL à qual a regra pertence (ex.: RegraEquilibrioMovimento). */
    public String classeRegra;

    public ContextoSemantico() {
    }

    public ContextoSemantico(String codigoRegra, String descricaoRegra,
                              List<String> regrasRelacionadas, String classeRegra) {
        this.codigoRegra = codigoRegra;
        this.descricaoRegra = descricaoRegra;
        this.regrasRelacionadas = regrasRelacionadas;
        this.classeRegra = classeRegra;
    }
}
