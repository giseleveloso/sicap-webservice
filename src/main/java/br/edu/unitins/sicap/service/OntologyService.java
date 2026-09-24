package br.edu.unitins.sicap.service;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.search.EntitySearcher;

import br.edu.unitins.sicap.dto.ContextoSemantico;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * Serviço Ontológico descrito na seção 3.5 do TCC: carrega a ontologia OWL
 * (sicap-contabil.owl), instancia o reasoner HermiT e expõe o contexto
 * semântico de cada regra para o PromptService consumir.
 *
 * OBS: este código ainda não foi compilado/testado com Maven neste ambiente
 * (sandbox sem acesso ao Maven Central). Rode "mvn compile" localmente antes
 * de seguir — pequenos ajustes de assinatura podem ser necessários dependendo
 * da versão exata da OWL API que o Maven resolver.
 */
@ApplicationScoped
public class OntologyService {

    @ConfigProperty(name = "sicap.ontology.path")
    String ontologyPath;

    @ConfigProperty(name = "sicap.ontology.namespace")
    String namespace;

    private OWLOntology ontology;
    private OWLReasoner reasoner;
    private OWLDataFactory factory;

    void onStart(@Observes StartupEvent event) {
        try {
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            factory = manager.getOWLDataFactory();

            try (InputStream in = getClass().getClassLoader().getResourceAsStream(ontologyPath)) {
                if (in == null) {
                    throw new IllegalStateException(
                            "Arquivo de ontologia não encontrado no classpath: " + ontologyPath);
                }
                ontology = manager.loadOntologyFromOntologyDocument(in);
            }

            ReasonerFactory reasonerFactory = new ReasonerFactory();
            reasoner = reasonerFactory.createReasoner(ontology);
            reasoner.precomputeInferences();

            System.out.println("[OntologyService] Ontologia carregada e reasoner HermiT inicializado. "
                    + "Classes: " + ontology.getClassesInSignature().size()
                    + " | Indivíduos: " + ontology.getIndividualsInSignature().size());
        } catch (Exception e) {
            throw new RuntimeException("Falha ao carregar a ontologia SICAP Contábil", e);
        }
    }

    /**
     * Retorna o contexto semântico de uma regra a partir do seu código
     * (ex.: "BV3"), já considerando as inferências do reasoner.
     */
        public ContextoSemantico obterContextoSemantico(String codigoRegra) {
        OWLNamedIndividual individuo = buscarIndividuoPorCodigo(codigoRegra);
        if (individuo == null) {
            throw new IllegalArgumentException("Regra não encontrada na ontologia: " + codigoRegra);
        }

        String descricao = obterValorDataProperty(individuo, "descricaoRegra");
        String classeRegra = obterClasse(individuo);
        Map<String, String> relacionadas = obterRegrasRelacionadasComDescricao(individuo);

        return new ContextoSemantico(codigoRegra, descricao, relacionadas, classeRegra);
    }

    private OWLNamedIndividual buscarIndividuoPorCodigo(String codigoRegra) {
        IRI iri = IRI.create(namespace + codigoRegra);
        if (!ontology.containsIndividualInSignature(iri)) {
            return null;
        }
        return factory.getOWLNamedIndividual(iri);
    }

    private String obterValorDataProperty(OWLNamedIndividual individuo, String nomePropriedade) {
        OWLDataProperty propriedade = factory.getOWLDataProperty(IRI.create(namespace + nomePropriedade));
        Collection<OWLLiteral> valores = EntitySearcher.getDataPropertyValues(individuo, propriedade, ontology)
                .collect(Collectors.toList());
        return valores.isEmpty() ? null : valores.iterator().next().getLiteral();
    }

    private String obterClasse(OWLNamedIndividual individuo) {
        return reasoner.getTypes(individuo, true).entities()
                .filter(c -> !c.isOWLThing())
                .map(c -> c.getIRI().getShortForm())
                .findFirst()
                .orElse(null);
    }

        private Map<String, String> obterRegrasRelacionadasComDescricao(OWLNamedIndividual individuo) {
        OWLObjectProperty relacionadaCom = factory.getOWLObjectProperty(IRI.create(namespace + "relacionadaCom"));
        Map<String, String> resultado = new LinkedHashMap<>();
        EntitySearcher.getObjectPropertyValues(individuo, relacionadaCom, ontology)
                .filter(OWLIndividual::isNamed)
                .map(OWLIndividual::asOWLNamedIndividual)
                .forEach(relacionado -> {
                    String codigo = relacionado.getIRI().getShortForm();
                    String descricao = obterValorDataProperty(relacionado, "descricaoRegra");
                    resultado.put(codigo, descricao != null ? descricao : "(sem descrição cadastrada)");
                });
        return resultado;
    }
}
