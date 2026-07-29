package br.edu.unitins.sicap.dto;

/**
 * Corpo da resposta devolvida por POST /inconsistencias/interpretar.
 */
public class InconsistenciaResponse {

    public String codigoRegra;

    /** Texto em linguagem natural gerado pelo LLM. */
    public String explicacao;

    /** Identificação do modelo de linguagem que gerou a explicação. */
    public String modeloUtilizado;

    /** Tempo total de processamento da requisição, em milissegundos. */
    public long tempoProcessamento;

    public InconsistenciaResponse() {
    }

    public InconsistenciaResponse(String codigoRegra, String explicacao,
                                   String modeloUtilizado, long tempoProcessamento) {
        this.codigoRegra = codigoRegra;
        this.explicacao = explicacao;
        this.modeloUtilizado = modeloUtilizado;
        this.tempoProcessamento = tempoProcessamento;
    }
}
