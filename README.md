# sicap-webservice

Esqueleto inicial do webservice do TCC II — Assistente Cognitivo baseado em
Ontologias para Validação de Dados Contábeis Municipais (SICAP/TCE-TO).

## O que já está pronto

- **`pom.xml`**: Quarkus 3.11 + OWL API 5.5.0 + reasoner HermiT.
- **`OntologyService`**: carrega `sicap-contabil.owl` no startup e usa o
  HermiT para inferir o contexto semântico de uma regra (descrição, classe,
  regras relacionadas).
- **`PromptService`**: monta o prompt em 3 blocos (papel/tarefa, contexto,
  dados dinâmicos), igual ao protótipo do capítulo 4 do seu TCC I.
- **`LlmService`**: **stub** — devolve uma resposta simulada, só pra validar
  o fluxo ponta a ponta. A integração real com GPT/Gemini/Grok/LLaMA/Mistral
  é a próxima etapa (Etapa 6 do roteiro).
- **`InconsistenciaResource`**: expõe `POST /inconsistencias/interpretar`.

## Antes de rodar

Este projeto foi montado num ambiente sem acesso ao Maven Central, então as
dependências **ainda não foram baixadas nem compiladas aqui**. Rode localmente:

```bash
mvn compile
```

e ajuste qualquer detalhe de assinatura de método da OWL API caso o Maven
resolva uma versão ligeiramente diferente da 5.5.0 (a API muda pouco entre
patch versions, mas vale conferir).

Se você tiver o JDK 25 instalado localmente (como citado no TCC), pode trocar
`maven.compiler.release` no `pom.xml` de `21` para `25` — deixei em 21 por
compatibilidade, já que é o que costuma estar disponível por padrão.

## Rodando em modo de desenvolvimento

```bash
mvn quarkus:dev
```

O Quarkus vai subir em `http://localhost:8080`.

## Testando o endpoint

```bash
curl -X POST http://localhost:8080/inconsistencias/interpretar \
  -H "Content-Type: application/json" \
  -d '{
    "codigoRegra": "BV3",
    "mensagemOriginal": "O SOMATORIO do movimentoContaDevedora deve ser igual ao SOMATORIO do movimentoContaCredora no Balancete de Verificacao (contas escrituradas S).",
    "dadosEncontrados": {
      "tipoBalancete": 1,
      "totalMovimentoDevedor": 1250000.00,
      "totalMovimentoCredor": 1198500.00,
      "diferenca": 51500.00
    },
    "exercicio": 2024,
    "bimestre": 3,
    "regrasRelacionadas": ["BV29"]
  }'
```

Resposta esperada (com o stub do LlmService): um JSON contendo `codigoRegra`,
uma `explicacao` começando com `[STUB] ...` (mostrando o prompt que foi
montado com base no contexto extraído da ontologia), `modeloUtilizado` e
`tempoProcessamento`.

Se esse fluxo funcionar (a explicação stub mostrar o contexto certo — a
descrição da BV3 e a BV29 como regra relacionada, inferida pelo reasoner),
significa que a arquitetura de ponta a ponta está funcionando, e o próximo
passo é trocar o `LlmService` por uma chamada real a um dos modelos.

## Próximos passos sugeridos

1. `mvn compile` e corrigir eventuais ajustes de API.
2. Testar o endpoint com os 3 códigos de regra (`BV3`, `BV29`, `BV32`) e
   conferir se o contexto semântico sai certo pra cada um.
3. Implementar a chamada real ao primeiro modelo (sugestão: comece pelo GPT
   via API REST, é o mais direto de integrar) dentro de `LlmService`.
4. Repetir para os demais modelos (Gemini, Grok, LLaMA/Mistral via Ollama).
5. Rodar os testes com os casos reais fornecidos pelo TCE/TO e aplicar a
   avaliação Likert descrita na seção 3.6 do TCC.
