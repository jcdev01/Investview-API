# Investview-API

API REST para gestão de investimentos, construída com Java 25 e Spring Boot 4.1.2.

## Stack

- Java 25
- Spring Boot 4.1.2 (Maven)
- Spring Web MVC (REST)
- Spring Data JPA (PostgreSQL)
- Spring Data Redis
- Spring Security + OAuth2 Client
- Bean Validation
- SpringDoc OpenAPI (Swagger UI)
- Lombok

## Pré-requisitos

- JDK 25
- Docker e Docker Compose

## Infraestrutura local

O projeto inclui um `docker-compose.yml` com PostgreSQL 17 e Redis 7.

```bash
docker compose up -d
```

Serviços expostos:

| Serviço    | Porta | Credenciais padrão                  |
| ---------- | ----- | ----------------------------------- |
| PostgreSQL | 5432  | db `investview`, user/senha `investview` |
| Redis      | 6379  | sem autenticação                    |

As credenciais podem ser sobrescritas por variáveis de ambiente
(`POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `REDIS_*`).
Para encerrar: `docker compose down` (use `-v` para remover os volumes).

## Como rodar

```bash
./mvnw spring-boot:run
```

A aplicação sobe em `http://localhost:8080`.

A conexão com banco e Redis é configurada em
`src/main/resources/application.properties` e pode ser sobrescrita por
variáveis de ambiente (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`,
`REDIS_PORT`).

## Documentação da API

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

As rotas do Swagger são públicas. As demais exigem autenticação.

Credenciais de desenvolvimento (via form login ou HTTP Basic):

- **Usuário:** `admin`
- **Senha:** `admin`

Podem ser alteradas por `APP_SECURITY_USERNAME` / `APP_SECURITY_PASSWORD`.
Essas credenciais fixas são apenas para desenvolvimento e devem ser
substituídas antes de produção.

## Testes

```bash
./mvnw test
```

Os testes sobem o contexto Spring e exigem o PostgreSQL em execução
(`docker compose up -d`).

## Estrutura do projeto

```
src/main/java/jairo/investviewapi/
├── InvestviewApiApplication.java
└── config/SecurityConfig.java
src/main/resources/application.properties
src/test/java/jairo/investviewapi/
docker-compose.yml
docs/AI_CONTEXT.md
```

## Build

```bash
./mvnw package
```
