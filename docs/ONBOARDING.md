# Onboarding — Investview-API

Resumo do projeto para quem está entrando agora.

## O que é o projeto

API REST para gestão de investimentos, em desenvolvimento inicial.
A base (Spring Boot + infra local + segurança de dev + Swagger) está pronta;
ainda não há entidades de domínio nem endpoints de negócio.

- **Repositório:** https://github.com/jcdev01/Investview-API.git
- **Branch principal:** `main`

## Stack em uso

- Java 25 + Spring Boot 4.1.2 (Maven)
- Spring Web MVC (REST)
- Spring Data JPA (PostgreSQL)
- Spring Data Redis
- Spring Security + OAuth2 Client
- Bean Validation
- SpringDoc OpenAPI (Swagger UI)
- Lombok
- Docker Compose (PostgreSQL 17 + Redis 7)

## Estrutura atual

```
src/main/java/jairo/investviewapi/
├── InvestviewApiApplication.java   -> classe principal
└── config/SecurityConfig.java      -> segurança de dev + Swagger público
src/main/resources/application.properties
src/test/java/jairo/investviewapi/InvestviewApiApplicationTests.java
docker-compose.yml                  -> PostgreSQL + Redis
README.md                           -> instruções gerais
AGENTS.md                           -> convenções para agentes de IA
docs/AI_CONTEXT.md                  -> memória das conversas/decisões
```

## Como rodar

Pré-requisitos: JDK 25, Docker e Docker Compose.

1. Subir banco e cache:

   ```bash
   docker compose up -d
   ```

2. Rodar a aplicação:

   ```bash
   ./mvnw spring-boot:run
   ```

3. Acessar:

   - API: http://localhost:8080
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - OpenAPI JSON: http://localhost:8080/v3/api-docs
   - h2 http://localhost:8080/h2-console/

4. Rodar testes:

   ```bash
   ./mvnw test
   ```

   Os testes exigem o PostgreSQL rodando.

## Credenciais (somente desenvolvimento)

### Banco de dados (PostgreSQL)

| Item    | Valor       |
| ------- | ----------- |
| Host    | `localhost` |
| Porta   | `5432`      |
| Database| `investview`|
| Usuário | `investview`|
| Senha   | `investview`|

### Redis

| Item  | Valor       |
| ----- | ----------- |
| Host  | `localhost` |
| Porta | `6379`      |
| Senha | (nenhuma)   |

### Login da aplicação

- **Usuário:** `admin`
- **Senha:** `admin`

As rotas do Swagger são públicas; as demais exigem autenticação.

> Essas credenciais são fixas e apenas para desenvolvimento local.
> Em produção devem ser trocadas (variáveis de ambiente) e a autenticação
> migrada para OAuth2/banco, removendo o usuário em memória.

## Variáveis de ambiente aceitas

| Variável                | Padrão                                          |
| ----------------------- | ----------------------------------------------- |
| `DB_URL`                | `jdbc:postgresql://localhost:5432/investview`   |
| `DB_USERNAME`           | `investview`                                    |
| `DB_PASSWORD`           | `investview`                                    |
| `REDIS_HOST`            | `localhost`                                     |
| `REDIS_PORT`            | `6379`                                          |
| `APP_SECURITY_USERNAME` | `admin`                                         |
| `APP_SECURITY_PASSWORD` | `admin`                                         |
| `POSTGRES_DB`           | `investview`                                    |
| `POSTGRES_USER`         | `investview`                                    |
| `POSTGRES_PASSWORD`     | `investview`                                    |

## Estado atual

Pronto:

- Projeto Spring Boot configurado (Java 25, Maven)
- Infra local com Docker (PostgreSQL + Redis) e healthchecks
- Conexão da aplicação com banco e Redis via `application.properties`
- Segurança de dev (`SecurityConfig`) com Swagger público
- README e contexto do projeto documentados

Pendente / próximos passos:

- Modelagem de domínio (entidades)
- Camadas de repositório, serviço e controllers
- Primeiros endpoints REST
- Migrations (Flyway/Liquibase) e remoção de `ddl-auto=update`
- Evoluir a autenticação para OAuth2/banco
- Testes de integração dos endpoints

## Git

- Remote: `origin` -> https://github.com/jcdev01/Investview-API.git
- Branch: `main`
- Commits iniciais já enviados (base do projeto + README)
