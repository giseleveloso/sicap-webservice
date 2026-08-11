package br.edu.unitins.sicap.resource;

import br.edu.unitins.sicap.dto.ContextoSemantico;
import br.edu.unitins.sicap.dto.InconsistenciaRequest;
import br.edu.unitins.sicap.dto.InconsistenciaResponse;
import br.edu.unitins.sicap.service.LlmService;
import br.edu.unitins.sicap.service.OntologyService;
import br.edu.unitins.sicap.service.PromptService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Endpoint principal do webservice (seção 3.5.1 do TCC):
 * POST /inconsistencias/interpretar
 */
@Path("/inconsistencias")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InconsistenciaResource {

    @Inject
    OntologyService ontologyService;

    @Inject
    PromptService promptService;

    @Inject
    LlmService llmService;

    @POST
    @Path("/interpretar")
    public Response interpretar(InconsistenciaRequest request) {
        long inicio = System.currentTimeMillis();

        if (request.codigoRegra == null || request.codigoRegra.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Campo 'codigoRegra' é obrigatório.")
                    .build();
        }

        try {
            // 1. Serviço Ontológico: busca o contexto semântico via reasoner
            ContextoSemantico contexto = ontologyService.obterContextoSemantico(request.codigoRegra);

            // 2. PromptService: monta o prompt estruturado
            String prompt = promptService.construirPrompt(request, contexto);

            // 3. Serviço de IA Generativa: gera a explicação
            String explicacao = llmService.gerarExplicacao(prompt);

            long tempoProcessamento = System.currentTimeMillis() - inicio;

            InconsistenciaResponse response = new InconsistenciaResponse(
                    request.codigoRegra,
                    explicacao,
                    llmService.getModeloUtilizado(),
                    tempoProcessamento
            );

            return Response.ok(response).build();

        } catch (IllegalArgumentException e) {
            // Regra não encontrada na ontologia
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(e.getMessage())
                    .build();

        } catch (IllegalStateException e) {
            // Configuração ausente (ex.: OPENAI_API_KEY não definida)
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage())
                    .build();

        } catch (RuntimeException e) {
            // Falha na chamada ao LLM (rede, erro da API, resposta inesperada etc.)
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity("Falha ao gerar explicação via LLM: " + e.getMessage())
                    .build();
        }
    }
}
