# AGENTS.md — Contexto do projeto para agentes de IA

Este arquivo é carregado automaticamente pelo opencode em toda sessão.
Leia-o antes de começar qualquer tarefa e siga as regras abaixo.

## Visão geral do projeto

- **Nome:** Investview-API
- **Objetivo:** API REST de investimentos (em desenvolvimento).
- **Linguagem/versão:** Java 25
- **Framework:** Spring Boot 4.1.2 (Maven)
- **Dependências principais:**
  - Spring Web MVC (REST)
  - Spring Data JPA (PostgreSQL)
  - Spring Data Redis
  - Spring Security + OAuth2 Client
  - Bean Validation
  - SpringDoc OpenAPI (Swagger UI em `/swagger-ui.html`)
  - Lombok
- **Testes:** JUnit 5 + Spring Boot Test

## Estrutura

```
src/main/java/jairo/investviewapi/   -> código principal (pacote base jairo.investviewapi)
src/main/resources/application.properties
src/test/java/jairo/investviewapi/   -> testes
pom.xml                              -> dependências e build
docs/AI_CONTEXT.md                   -> memória das conversas e decisões
```

## Comandos úteis

- Compilar: `./mvnw compile`
- Rodar a aplicação: `./mvnw spring-boot:run`
- Rodar os testes: `./mvnw test`
- Empacotar: `./mvnw package`

Sempre execute os testes (`./mvnw test`) após alterações relevantes.

## Convenções de código

- Pacote base: `jairo.investviewapi`.
- Use as bibliotecas já presentes no `pom.xml`; não adicione dependências sem necessidade.
- Siga o estilo do código existente (Spring Boot idiomático, injeção por construtor).
- Não adicione comentários no código, a menos que solicitado.
- Nunca exponha ou faça commit de segredos/credenciais.

## REGRA OBRIGATÓRIA: atualizar o contexto automaticamente

O arquivo `docs/AI_CONTEXT.md` é a memória persistente das nossas conversas.

Após concluir qualquer tarefa relevante, tomar uma decisão de arquitetura,
ou quando o usuário informar uma preferência, **atualize automaticamente
`docs/AI_CONTEXT.md`** antes de finalizar a resposta. Atualizações incluem:

1. Adicionar uma entrada no **Histórico de conversas** com data e resumo do que foi feito.
2. Atualizar a seção **Estado atual** (funcionalidades prontas, pendências).
3. Registrar **Decisões** importantes e o motivo.
4. Atualizar **Próximos passos** quando aplicável.

Não é necessário pedir permissão para atualizar esse arquivo: isso faz parte
do fluxo normal de trabalho. Mantenha as entradas concisas e em português.
