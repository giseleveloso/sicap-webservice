package br.edu.unitins.sicap.dto;

import java.util.List;
import java.util.Map;

/**
 * Corpo da requisição recebida em POST /inconsistencias/interpretar.
 * Espelha a estrutura definida no capítulo de metodologia do TCC
 * (seção 3.5.1 - Estrutura da requisição e da resposta).
 */
public class InconsistenciaRequest {

    /** Código oficial da regra violada no SICAP (ex.: "BV3"). */
    public String codigoRegra;

    /** Mensagem técnica original retornada pelo Analisador SICAP Contábil. */
    public String mensagemOriginal;

    /** Valores contábeis envolvidos na inconsistência (totais, diferenças, contas etc.). */
    public Map<String, Object> dadosEncontrados;

    /** Ano do exercício contábil da remessa. */
    public Integer exercicio;

    /** Bimestre de referência da remessa. */
    public Integer bimestre;

    /** Códigos de outras regras semanticamente relacionadas (opcional, pode vir vazio). */
    public List<String> regrasRelacionadas;
}
