# Contexto de IA — Investview-API

> Memória persistente das nossas conversas. Mantido automaticamente pelo agente
> conforme as regras do `AGENTS.md`. Não apague o histórico; apenas acrescente.
>
> **Status: projeto pausado em 2026-11-05.** Ao retomar, ler este arquivo
> inteiro antes de agir.

## Resumo do projeto

API REST para gestão de investimentos, construída com Java 25 + Spring Boot
4.1.2 (Maven). Está no estágio inicial: a **base de infraestrutura, segurança de
desenvolvimento e documentação** está pronta, mas ainda **não existem entidades
de domínio nem endpoints de negócio**. O próximo marco é a modelagem de domínio.

- **Repositório:** https://github.com/jcdev01/Investview-API.git
- **Branch principal:** `main`
- **Pacote base:** `jairo.investviewapi`
- **Porta padrão da aplicação:** `8080`

## Stack

- Java 25
- Spring Boot 4.1.2 (Maven)
- Spring Web MVC (REST)
- Spring Data JPA (PostgreSQL / H2 em dev)
- Spring Data Redis
- Spring Security + OAuth2 Client
- Bean Validation
- SpringDoc OpenAPI (Swagger UI)
- Lombok
- Testes: JUnit 5 + Spring Boot Test
- Infra local: Docker Compose (PostgreSQL 17 + Redis 7)

## Como rodar e comandos

```bash
docker compose up -d          # sobe PostgreSQL + Redis (necessário no perfil prod)
./mvnw spring-boot:run        # roda a aplicação em http://localhost:8080 (perfil test/H2)
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod   # perfil prod (Postgres)
./mvnw test                   # testes (perfil test/H2 em memória)
./mvnw package                # empacota
./mvnw compile                # compila
```

URLs úteis:

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Credenciais e variáveis de ambiente

> Os valores sensíveis **não** ficam no código/documentação. Ficam apenas no
> arquivo `.env` (na raiz, ignorado pelo Git). O modelo fica em `.env.example`.
> Não usar os valores de dev em produção.

Variáveis esperadas (obrigatórias; sem defaults embutidos):

| Variável                | Uso                                              |
| ----------------------- | ------------------------------------------------ |
| `POSTGRES_DB`           | nome do banco do container Postgres              |
| `POSTGRES_USER`         | usuário do container Postgres                    |
| `POSTGRES_PASSWORD`     | senha do container Postgres                      |
| `DB_URL`                | JDBC do Postgres (perfil `prod`)                 |
| `DB_USERNAME`           | usuário do banco (perfil `prod`)                 |
| `DB_PASSWORD`           | senha do banco (perfil `prod`)                   |
| `REDIS_HOST`            | host do Redis                                    |
| `REDIS_PORT`            | porta do Redis                                   |
| `APP_SECURITY_USERNAME` | usuário do login de dev                          |
| `APP_SECURITY_PASSWORD` | senha do login de dev                            |
| `SPRING_PROFILES_ACTIVE`| profile ativo (`test` por padrão)                |

O Docker Compose carrega `.env` automaticamente. O Spring Boot lê o `.env` via
`spring.config.import=optional:file:.env[.properties]` em `application.properties`.

## Estrutura atual

```
src/main/java/jairo/investviewapi/
├── InvestviewApiApplication.java   -> classe principal
├── config/SecurityConfig.java      -> segurança de dev + Swagger público
├── controller/
│   ├── UserController.java         -> CRUD REST de usuário (/users)
│   └── FavoriteController.java     -> favoritos por usuário (Swagger documentado)
├── dto/
│   ├── UserCreateRequest.java      -> record de entrada (create) + validação
│   ├── UserUpdateRequest.java      -> record de entrada (update) + validação
│   ├── UserResponse.java           -> record de saída (sem senha)
│   ├── FavoriteRequest.java        -> record de entrada (symbol, type, name) + validação
│   └── FavoriteResponse.java       -> record de saída (id, symbol, type, name)
├── entity/
│   ├── User.java                   -> entidade JPA de usuário (1:N favorites)
│   ├── AssetType.java              -> enum (STOCK, FII, ETF, CRYPTO)
│   └── Favorite.java               -> favorito do usuário (user, symbol, type, name)
├── exception/
│   ├── ResourceNotFoundException.java -> recurso não encontrado (404)
│   └── ConflictException.java      -> conflito de duplicidade (409)
├── repository/
│   ├── UserRepository.java         -> JpaRepository<User, Long>
│   └── FavoriteRepository.java     -> JpaRepository<Favorite, Long>
└── service/
    ├── UserService.java            -> CRUD de usuário + hash de senha
    └── FavoriteService.java        -> adicionar/listar/remover favoritos
src/main/resources/application.properties
src/test/java/jairo/investviewapi/InvestviewApiApplicationTests.java
docker-compose.yml                  -> PostgreSQL + Redis
README.md                           -> instruções gerais
AGENTS.md                           -> convenções para agentes de IA
docs/AI_CONTEXT.md                  -> este arquivo (memória)
docs/ONBOARDING.md                  -> resumo para novo colaborador
```

## Estado atual

Pronto:

- [x] Projeto Spring Boot criado (Maven, Java 25)
- [x] Dependências configuradas no `pom.xml`
- [x] Classe principal `InvestviewApiApplication`
- [x] Teste de carregamento de contexto
- [x] Infra local com Docker (`docker-compose.yml`) para PostgreSQL 17 e Redis 7
- [x] Configuração de conexão com banco e Redis no `application.properties`
- [x] Segurança de dev (`SecurityConfig`) com Swagger público
- [x] README, `AGENTS.md`, `docs/AI_CONTEXT.md` e `docs/ONBOARDING.md`
- [x] Repositório Git com `origin` e commits enviados para `main`
- [x] Estrutura de pacotes em camadas criada (`entity`, `repository`, `service`,
  `controller`, `dto`, `exception`)
- [x] Entidade `User` (JPA) criada
- [x] Relacionamento `User` 1:N `Favorite` (bidirecional) criado
- [x] `UserRepository` (Spring Data JPA)
- [x] `FavoriteRepository` (Spring Data JPA)
- [x] `UserService` com CRUD e hash de senha (BCrypt via `PasswordEncoder`)
- [x] `ResourceNotFoundException` para recurso inexistente
- [x] DTOs `record` de usuário (create/update/response) com Bean Validation
- [x] `UserController` com CRUD REST em `/users`
- [x] H2 em memória para dev + console em `/h2-console`
- [x] Favoritos simplificados: `Favorite` armazena diretamente `symbol`, `type` (`STOCK, FII, ETF, CRYPTO`) e `name` (opcional), com constraint unique `(user_id, type, symbol)`. Remoção da entidade `Asset` e de `AssetRepository`.
- [x] `FavoriteController`/`FavoriteService`: adicionar (normalização de symbol + prevenção de duplicidade 409), listar por usuário e remover (404); Swagger documentado com `@Tag` e `@Operation`.

Pendente:

- [ ] Tratamento global de exceções (handler para email duplicado etc.)
- [ ] Entidade `Favorite` evoluir com campos de negócio (se necessário)
- [ ] Migrations (Flyway/Liquibase) e remoção de `ddl-auto=update`
- [ ] Evoluir autenticação para OAuth2/banco (remover usuário em memória)
- [ ] Testes de integração dos endpoints
- [ ] Documentação dos endpoints no Swagger

## Git

- Remote `origin` -> https://github.com/jcdev01/Investview-API.git
- Branch `main` com upstream configurado.
- Commits já enviados:
  - `88eb5a3` chore: configura projeto Spring Boot com Docker, Swagger e contexto de IA
  - `bf8ae87` docs: registra commit inicial e remote no contexto de IA
  - `f804dfa` docs: adiciona README do projeto
  - `fc6e782` feat:Implementação do crud da entity User
  - `bc06fa8` feat: implementa gerenciamento de favoritos simplificado e documentação Swagger

## Decisões

- **Contexto de IA:** usar `AGENTS.md` para instruções do projeto e
  `docs/AI_CONTEXT.md` como memória viva, atualizada automaticamente a cada
  tarefa relevante.
- **Infra local:** PostgreSQL 17 e Redis 7 via `docker-compose.yml`, com volumes
  nomeados e healthchecks. Credenciais padrão de dev sobrescrevíveis por env.
- **Ambientes por profile:** `application.properties` guarda o comum e ativa
  `test` por padrão (`SPRING_PROFILES_ACTIVE`). `application-test.properties` usa
  H2 em memória + console; `application-prod.properties` usa o Postgres principal
  via `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` (H2 console desligado). Motivo: separar
  desenvolvimento rápido (H2) do banco principal (Postgres) sem editar arquivos.
- **Conexão da app (test):** H2 em memória (`jdbc:h2:mem:investview`), console em
  `/h2-console` (liberado no `SecurityConfig`, frames `sameOrigin`). Redis
  configurado, porém ainda não utilizado.
- **H2:** dependência `com.h2database:h2` (runtime). URL em memória com
  `DB_CLOSE_DELAY=-1`; `ddl-auto=update`. Console H2 habilitado para inspecionar
  o schema sem banco externo.
- **H2 Console no Spring Boot 4:** o auto-config do console saiu do
  `spring-boot-autoconfigure` e virou o módulo `spring-boot-h2console`. Sem essa
  dependência, `spring.h2.console.enabled=true` é ignorado e `/h2-console` dá
  404. Adicionada em `pom.xml` (runtime).
- **`ddl-auto=update`:** escolhido para facilitar o desenvolvimento inicial;
  deve ser revisto antes de produção (preferir Flyway/Liquibase).
- **Segredos via `.env`:** `.env` (na raiz) centraliza credenciais de dev
  (`POSTGRES_*`, `DB_*`, `REDIS_*`, `APP_SECURITY_*`, `SPRING_PROFILES_ACTIVE`).
  O Docker Compose carrega `.env` automaticamente. O Spring Boot passa a ler o
  `.env` via `spring.config.import=optional:file:.env[.properties]`. O `.env` é
  ignorado pelo Git; só o `.env.example` é versionado.
- **Sem defaults sensíveis:** removidos os valores-padrão embutidos de
  `docker-compose.yml`, `application.properties`, `application-prod.properties` e
  `SecurityConfig` (`${VAR}` sem fallback). Assim os segredos existem apenas no
  `.env`; sem ele, Docker/Spring falham em vez de usar credenciais fixas.
- **Segurança dev:** `SecurityConfig` com `InMemoryUserDetailsManager`
  (`admin`/`admin`), CSRF desabilitado (API REST), form login + HTTP Basic.
  Swagger liberado em `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**` e
  H2 console em `/h2-console/**`; demais rotas exigem autenticação. Trocar por
  OAuth2/DB antes de produção.
- **OpenAPI:** SpringDoc expõe `/swagger-ui.html` e `/v3/api-docs` por padrão.
- **Arquitetura em camadas:** pacotes por responsabilidade (`entity`,
  `repository`, `service`, `controller`, `dto`, `exception`, `config`).
- **DTOs com `record`:** conversões entre entidade e DTO serão feitas via
  construtores/factories nos próprios `record`s. Por isso a pasta `mapper/` foi
  descartada (sem MapStruct nem mapeadores dedicados).
- **Entidade `User`:** tabela `tb_users`, PK `Long` gerada por
  `GenerationType.IDENTITY`, `name`, `email` único e `password` (hash). Por ora
  sem `enabled`/`role`/timestamps — serão avaliados depois. Anotações Lombok
  (`@Getter/@Setter/@Builder`).
- **Sem roles:** o sistema terá apenas usuário comum; não haverá papel/admin
  (`Role` foi descartado). Autenticação/autorização será revisitada quando a
  segurança evoluir.
- **`User` 1:N `Favorite`:** `User.favorites` com `@OneToMany(mappedBy = "user",
  cascade = ALL, orphanRemoval = true)` e `List<Favorite>` inicializada. Do outro
  lado, `Favorite.user` com `@ManyToOne(fetch = LAZY, optional = false)` e
  `@JoinColumn(name = "user_id")`.
- **Favoritos simplificados (sem Asset nem chamadas externas):** O backend NÃO
  chama APIs externas e NÃO armazena cotações. Guarda apenas a identidade do ativo
  favorito (`symbol` normalizado com `trim().toUpperCase()`, `type` do enum
  `STOCK, FII, ETF, CRYPTO` e `name` opcional para exibição). O frontend consulta as
  cotações diretamente na brapi usando o `symbol`. A API CoinGecko foi descartada,
  pois criptomoedas também são consultadas na brapi (`/api/v2/crypto?coin=BTC`).
  A entidade `Asset` e o `AssetRepository` foram removidos. A tabela `tb_favorites`
  guarda diretamente os dados do ativo com unique `(user_id, type, symbol)`.
  Duplicidade lança `ConflictException` (409) e exclusão/consulta inexistente lança
  `ResourceNotFoundException` (404). Endpoints em `/users/{userId}/favorites`
  estão documentados com anotações OpenAPI/Swagger.
- **Chaves primárias `Long`:** a partir de 2026-11-07 as PKs usam `Long` com
  `GenerationType.IDENTITY` (em vez de `UUID`), por simplicidade e performance em
  banco único.
- **`UserService`:** recebe/retorna DTOs `record`; usa `PasswordEncoder` (BCrypt)
  para hashear a senha na criação e somente re-hasheia no update se uma nova
  senha for informada. `create` valida email duplicado (`existsByEmail`).
  Escritas com `@Transactional`, leituras com `readOnly = true`. `getUser` lança
  `ResourceNotFoundException`. `UserResponse.from(User)` converte entidade→DTO e
  `UserCreateRequest.toEntity()` converte DTO→entidade.
- **DTOs de usuário:** `UserCreateRequest` (name/email/password obrigatórios,
  senha `min = 6`), `UserUpdateRequest` (senha opcional) e `UserResponse`
  (id/name/email, nunca expõe senha). Validação com Bean Validation.
- **`UserController`:** base `/users`. `POST` (201 + `Location`), `GET` (lista),
  `GET /{id}`, `PUT /{id}`, `DELETE /{id}` (204). Usa `@Valid` nos DTOs de
  entrada. `ResourceNotFoundException` anotada com `@ResponseStatus(NOT_FOUND)`
  para retornar 404.

## Observações do ambiente

- O desenvolvimento/execução acontece no **Windows (IntelliJ IDEA)**, com o
  projeto em `C:\Users\Jairo\IdeaProjects\Investview-API` refletido no WSL em
  `/mnt/c/...`.
- O **WSL não tem Java, Maven nem Docker CLI** configurados; por isso a app roda
  no IntelliJ e o Docker pelo Docker Desktop (integracao WSL opcional).
- O **push pelo WSL falha por falta de credenciais do GitHub** (`could not read
  Username`). O push foi feito no PowerShell. Para push pelo WSL, configurar o
  Git Credential Manager do Windows ou autenticar no WSL.
- A identidade Git do Windows (`Jairo` / `Jairocostanascimento@gmail.com`) não
  está configurada no WSL; usamos `git -c user.name=... -c user.email=...` por
  commit quando necessário.
- O log do Spring Boot 4.1.2 **não exibe** `Using generated security password`;
  por isso foi criada a `SecurityConfig` com credenciais fixas de dev.

## Histórico de conversas

### 2026-11-07
- Definida arquitetura em camadas por responsabilidade.
- Criados os pacotes vazios `entity`, `repository`, `service`, `controller`,
  `dto` e `exception` (sem código ainda). Pasta `mapper` descartada.
- Acordado que a primeira entidade de domínio será `User`.
- Acordado que os DTOs serão `record`s (conversão via construtores/factories).
- Criada a entidade `User` (tabela `tb_users`). Sem compilação no WSL
  (sem Java/Maven); validação deve ser feita no IntelliJ/Windows.
- Removido o enum `Role` e o campo `role`: sistema terá apenas usuário comum,
  sem admin.
- Removidos `enabled`, `createdAt` e `updatedAt` da entidade `User` (não
  necessários por enquanto). `User` ficou com `id`, `name`, `email`, `password`.
- Criada a entidade `Favorite` (tabela `tb_favorites`, `id` + `user`) e o
  relacionamento bidirecional `User` 1:N `Favorite`.
- Padronizadas as PKs como `Long` com `GenerationType.IDENTITY` (decisão do
  usuário, válida a partir de agora).
- Criados `UserRepository`, `UserService` (CRUD + hash BCrypt da senha) e
  `ResourceNotFoundException`.
- Criados os DTOs `record` `UserCreateRequest`, `UserUpdateRequest` e
  `UserResponse` (com Bean Validation) e adaptado o `UserService` para usá-los.
- Criado o `UserController` com CRUD REST em `/users`; `ResourceNotFoundException`
  passou a retornar 404 via `@ResponseStatus`.
- Adicionada dependência do H2 e trocado o `application.properties` para rodar em
  H2 em memória (console em `/h2-console`); Postgres/Docker ficam para depois.
  O Redis já estava como dependência e segue configurado, mas sem uso ainda.
- Separados os ambientes por profile: `test` (padrão, H2 em memória) e `prod`
  (Postgres principal). Comum em `application.properties`; sem editar arquivos
  para alternar.
- Criado `.env` com as credenciais de dev e `.env.example` como modelo;
  `.env` adicionado ao `.gitignore`.
- Removidos os defaults sensíveis de `docker-compose.yml`, `application.properties`,
  `application-prod.properties` e `SecurityConfig`; segredos agora só no `.env`.
- Fix do H2 Console: adicionado `spring-boot-h2console` (módulo separado no Boot
  4). Acessar com barra final: `/h2-console/`.
- Definido o modelo de favoritos: `Asset` (ação/cripto/fundo) + `Favorite` como
  vínculo User N:1 Asset, único por usuário. Implementados entity, repositórios,
  DTOs (`record`), `FavoriteService` e `FavoriteController`
  (`/users/{userId}/favorites`).
- Simplificado: sem brapi/CoinGecko no backend. `Asset` e `AssetRepository` removidos.
- Modelo de favoritos simplificado: `Favorite` armazena diretamente `symbol`, `type` (`STOCK, FII, ETF, CRYPTO`) e `name` (opcional), com constraint unique `(user_id, type, symbol)`.
- `FavoriteRequest` e `FavoriteResponse` ajustados (com validação Bean Validation).
- `FavoriteService` atualizado para normalizar `symbol` (`trim().toUpperCase()`) e lançar 409 em duplicidade.
- `FavoriteController` documentado com Swagger / OpenAPI annotations.

### 2026-11-05
- Criado o contexto de IA do projeto (`AGENTS.md` + `docs/AI_CONTEXT.md`) com
  regras para atualização automática das informações das conversas.
- Adicionado `docker-compose.yml` com PostgreSQL 17 e Redis 7 (volumes,
  healthchecks e credenciais via env).
- Configurado `application.properties` para conectar ao PostgreSQL e Redis
  locais, com variáveis de ambiente e valores padrão.
- Infra Docker validada pelo usuário (`docker compose up -d` funcionou).
- App iniciado com sucesso no IntelliJ (Java 25, porta 8080, Postgres 17
  conectado).
- Criada `SecurityConfig` com usuário de dev `admin`/`admin` e Swagger público.
- Login e acesso ao Swagger validados pelo usuário.
- Repositório Git inicializado, remote `origin` adicionado e branch renomeada
  para `main`; push concluído após autenticar no PowerShell.
- Criado `README.md` com stack, infra Docker, execução, Swagger e testes.
- Criado `docs/ONBOARDING.md` (resumo para novo colaborador, com stack,
  execução, credenciais de dev e próximos passos).
- Consolidada esta memória antes da pausa do projeto.

## Próximos passos

1. Commitar e enviar `docs/ONBOARDING.md` + `docs/AI_CONTEXT.md`.
2. Definir a modelagem de domínio inicial (ex.: usuário, ativo, carteira,
   transação).
3. Criar entidades JPA, repositórios, serviços e controllers.
4. Implementar os primeiros endpoints REST e documentar no Swagger.
5. Avaliar Flyway/Liquibase e remover `ddl-auto=update` antes de produção.
6. Evoluir a autenticação para OAuth2/banco.
7. Escrever testes de integração dos endpoints.
