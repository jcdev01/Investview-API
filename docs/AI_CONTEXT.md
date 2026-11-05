# Contexto de IA — Investview-API

> Memória persistente das nossas conversas. Mantido automaticamente pelo agente
> conforme as regras do `AGENTS.md`. Não apague o histórico; apenas acrescente.

## Resumo do projeto

API REST de investimentos construída com Java 25 + Spring Boot 4.1.2.
Ainda em estágio inicial: apenas a classe de inicialização e o teste de contexto
existem. Stack preparada para JPA/PostgreSQL, Redis, Security/OAuth2, Validation,
OpenAPI (SpringDoc) e Lombok.

## Estado atual

- [x] Projeto Spring Boot criado (Maven, Java 25)
- [x] Dependências configuradas no `pom.xml`
- [x] Classe principal `InvestviewApiApplication`
- [x] Teste de carregamento de contexto
- [x] Infra local com Docker (`docker-compose.yml`) para PostgreSQL e Redis
- [x] Configuração de conexão com banco e Redis no `application.properties`
- [ ] Modelagem de domínio (entidades)
- [ ] Camadas de repositório, serviço e controllers
- [ ] Segurança/OAuth2
- [ ] Documentação de endpoints

## Decisões

- **Contexto de IA:** usar `AGENTS.md` para instruções do projeto e
  `docs/AI_CONTEXT.md` como memória viva das conversas, atualizada
  automaticamente a cada tarefa relevante.
- **Infra local:** PostgreSQL 17 e Redis 7 via `docker-compose.yml`, com
  volumes nomeados e healthchecks. Credenciais padrão de dev (`investview`)
  sobrescrevíveis por variáveis de ambiente (`POSTGRES_*`, `REDIS_*`, `DB_*`).
- **Conexão da app:** `application.properties` usa variáveis com fallback para
  `localhost`, mantendo o projeto portátil entre ambientes.
- **`ddl-auto=update`:** escolhido para facilitar o desenvolvimento inicial;
  deve ser revisto antes de produção (migrations com Flyway/Liquibase).
- **Segurança dev:** criada `SecurityConfig` com `InMemoryUserDetailsManager`
  (usuário `admin` / senha `admin`, sobrescrevíveis por
  `APP_SECURITY_USERNAME` / `APP_SECURITY_PASSWORD`). Swagger liberado em
  `/swagger-ui.html`, `/swagger-ui/**` e `/v3/api-docs/**`; demais rotas exigem
  autenticação (form login + basic). Credenciais fixas são só para dev; trocar
  por OAuth2/DB antes de produção.

## Histórico de conversas

### 2026-11-05
- Criado o contexto de IA do projeto (`AGENTS.md` + `docs/AI_CONTEXT.md`) com
  regras para atualização automática das informações das conversas.
- Adicionado `docker-compose.yml` com PostgreSQL 17 e Redis 7 (volumes,
  healthchecks e credenciais via env).
- Configurado `application.properties` para conectar ao PostgreSQL e Redis
  locais, com variáveis de ambiente e valores padrão.
- Infra Docker validada pelo usuário (`docker compose up -d` funcionou).
- App iniciado com sucesso no IntelliJ (Java 25, porta 8080, Postgres 17
  conectado). Não havia senha gerada pelo Spring Security no log; criada
  `SecurityConfig` com usuário de dev `admin`/`admin` e Swagger público.
- Login e acesso ao Swagger validados pelo usuário.
- Repositório Git inicializado: commit inicial na branch `main`, remote
  `origin` -> https://github.com/jcdev01/Investview-API.git. Push pendente de
  autenticação do GitHub no WSL (rodar no PowerShell/IntelliJ).

## Próximos passos

- Subir a infra: `docker compose up -d` (Docker Desktop com integração WSL).
- Concluir o `git push -u origin main` (autenticar no GitHub).
- Definir a modelagem de domínio inicial.
- Criar os primeiros endpoints REST.
- Avaliar Flyway/Liquibase e remover `ddl-auto=update` antes de produção.
