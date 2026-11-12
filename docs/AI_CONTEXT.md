# Contexto de IA — Investview-API

> Memória persistente das nossas conversas. Mantido automaticamente pelo agente
> conforme as regras do `AGENTS.md`. Não apague o histórico; apenas acrescente.
>
> **Status: em desenvolvimento ativo (retomado em 2026-11-08).**
> Autenticação Google OAuth2 Resource Server, tratamento de exceções, proteção
> contra IDOR, controle de acesso RBAC (`ROLE_ADMIN` vs `ROLE_USER`) e grafo
> global de ativos por coocorrência de favoritos implementados.

## Resumo do projeto

API REST para gestão de investimentos, construída com Java 25 + Spring Boot
4.1.2 (Maven). Possui infraestrutura de persistência, autenticação Google
(OAuth2 Resource Server via JWT), entidades de domínio (`User` e `Favorite`),
controle de acesso por dono e RBAC (`ROLE_ADMIN` vs `ROLE_USER`).

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
> arquivo `.env` (na raiz, ignorado pelo Git). As variáveis necessárias estão documentadas na tabela abaixo.
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
| `APP_SECURITY_ADMIN_EMAILS` | emails de admins separados por vírgula        |
| `SPRING_PROFILES_ACTIVE`| profile ativo (`test` por padrão)                |
| `APP_OAUTH_GOOGLE_CLIENT_ID` | client ID do OAuth2/Google (audience do token)   |
| `APP_OAUTH_GOOGLE_ISSUER`    | issuer do JWT (`https://accounts.google.com`)    |
| `APP_OAUTH_GOOGLE_JWK_SET_URI` | JWK Set URI do Google (`https://www.googleapis.com/oauth2/v3/certs`) |
| `APP_CORS_ALLOWED_ORIGINS` | origens permitidas no CORS (separadas por vírgula) |

O Docker Compose carrega `.env` automaticamente. O Spring Boot lê o `.env` via
`spring.config.import=optional:file:.env[.properties]` em `application.properties`.

## Estrutura atual

```
src/main/java/jairo/investviewapi/
├── InvestviewApiApplication.java   -> classe principal
├── config/
│   ├── SecurityConfig.java         -> beans comuns (PasswordEncoder, JwtDecoder, CORS)
│   ├── DevSecurityConfig.java      -> chain dev/test (form login + Basic + JWT)
│   └── ProdSecurityConfig.java     -> chain prod (somente JWT resource server)
├── security/GoogleUserSyncFilter.java -> provisiona User (JIT) a partir do JWT do Google
├── security/CurrentUserProvider.java -> resolve o User do JWT e exige que seja o dono do recurso
├── controller/
│   ├── AuthController.java         -> /auth/me (usuário autenticado via Google)
│   ├── UserController.java         -> CRUD REST de usuário (/users)
│   ├── FavoriteController.java     -> favoritos por usuário (Swagger documentado)
│   ├── PortfolioController.java    -> carteira do usuário (/users/{userId}/portfolio)
│   └── GraphController.java        -> endpoints /graph (Swagger documentado)
├── dto/
│   ├── UserCreateRequest.java      -> record de entrada (create) + validação
│   ├── UserUpdateRequest.java      -> record de entrada (update) + validação
│   ├── UserResponse.java           -> record de saída (sem senha)
│   ├── FavoriteRequest.java        -> record de entrada (symbol, name) + validação
│   ├── FavoriteResponse.java       -> record de saída (id, symbol, name)
│   ├── GraphNode.java              -> nó do grafo (id = symbol, symbol, name)
│   ├── GraphEdge.java              -> aresta (source, target, weight)
│   ├── GraphResponse.java          -> { nodes, edges } do GET /graph
│   ├── NeighborResponse.java       -> vizinho com peso (id, symbol, name, weight)
│   ├── PathResponse.java           -> resultado do BFS (found, path, length)
│   ├── PortfolioResponse.java      -> carteira (id, name, items)
│   ├── PortfolioItemResponse.java  -> cota (id, symbol, name, quantity)
│   ├── PortfolioRenameRequest.java -> record de entrada (name) + validação
│   ├── PortfolioItemRequest.java   -> record de entrada (symbol, name, quantity) + validação
│   └── PortfolioItemUpdateRequest.java -> record de entrada (quantity, name) + validação
├── entity/
│   ├── User.java                   -> entidade JPA de usuário (1:N favorites, role USER/ADMIN)
│   ├── Role.java                   -> enum de perfis de acesso (USER, ADMIN)
│   ├── Favorite.java               -> favorito do usuário (user, symbol, name)
│   ├── Portfolio.java              -> carteira do usuário (1:1 com User, 1:N items)
│   └── PortfolioItem.java          -> cota da carteira (symbol, name, quantity)
├── exception/
│   ├── ResourceNotFoundException.java -> recurso não encontrado (404)
│   ├── ConflictException.java      -> conflito de duplicidade (409)
│   ├── ErrorResponse.java          -> payload padrão de erro (timestamp/status/erro/mensagem/path/errors)
│   └── GlobalExceptionHandler.java -> @RestControllerAdvice com mapeamento de erros → HTTP
├── repository/
│   ├── UserRepository.java         -> JpaRepository<User, Long>
│   ├── FavoriteRepository.java     -> JpaRepository<Favorite, Long> + findAllWithUserId (projeção)
│   ├── FavoriteView.java           -> projeção (userId, symbol, name) para montar o grafo
│   ├── PortfolioRepository.java    -> JpaRepository<Portfolio, Long> + findByUserId
│   └── PortfolioItemRepository.java -> JpaRepository<PortfolioItem, Long> + consultas por carteira/usuário
├── graph/
│   ├── Graph.java                  -> interface (addNode, addEdge, neighbors, edges, weight, shortestPath, components)
│   └── AdjacencyListGraph.java     -> implementação com Maps (BFS, DFS e EdgeKey normalizado)
└── service/
    ├── UserService.java            -> CRUD de usuário + hash de senha
    ├── GoogleUserService.java      -> provisiona/vincula usuário do Google por email
    ├── GraphService.java           -> monta o grafo a partir dos favoritos (agregados, sem expor usuários)
    ├── FavoriteService.java        -> adicionar/listar/remover favoritos
    └── PortfolioService.java       -> carteira: get/rename + adicionar/listar/atualizar/remover cotas
src/main/resources/application.properties
src/test/java/jairo/investviewapi/
├── InvestviewApiApplicationTests.java -> teste de contexto
├── controller/
│   ├── AuthControllerTest.java        -> testes de login Google via MockMvc + jwt()
│   ├── OwnerAuthorizationTest.java    -> testes de IDOR e RBAC (USER vs ADMIN)
│   └── PortfolioControllerTest.java   -> testes da carteira (CRUD de cotas, upsert, 400/401/403/404)
└── graph/
    └── GraphControllerTest.java       -> testes do grafo (nós/arestas/pesos, vizinhos, BFS, componentes, 400/404/401)
docker-compose.yml                  -> PostgreSQL + Redis
Dockerfile                          -> build multi-stage da API (só a app; banco/Redis externos via env)
.dockerignore                       -> exclui .env/target/docs do contexto de build
README.md                           -> instruções gerais
AGENTS.md                           -> convenções para agentes de IA
docs/AI_CONTEXT.md                  -> este arquivo (memória)
docs/ONBOARDING.md                  -> resumo para novo colaborador
docs/FRONT_TEST_PROMPT.md           -> prompt p/ IA construir front de testes das rotas
docs/GRAPH_PROMPT.md                -> prompt/especificação do grafo de ativos (Ideia 1)
docs/GRAPH_VIEW_PROMPT.md           -> prompt p/ outra IA criar front visual de testes do grafo
docs/GRAFO_EXPLICACAO.txt           -> explicação em texto puro dos conceitos (vértice, aresta, peso, vizinho, grau, caminho, componente)
docs/REQUISITOS.md                  -> requisitos funcionais, não funcionais e de domínio (RF/RNF/RD) consolidados
docs/USER_STORIES.md                -> requisitos funcionais como estórias de usuário (US01-13) com critérios de aceite e rastreabilidade
docs/BDD.md                          -> user stories transformadas em cenários BDD/Gherkin (Dado/Quando/Então) com rastreabilidade
docs/PLANILHA_DE_TESTES.md           -> planilha de teste estruturada (48 casos, TC-001..TC-048) por módulo, pronta p/ converter em Excel/Sheets
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
- [x] Favoritos simplificados: `Favorite` armazena diretamente `symbol` e `name` (opcional), com constraint unique `(user_id, symbol)`. Remoção da entidade `Asset` e de `AssetRepository`.
- [x] `FavoriteController`/`FavoriteService`: adicionar (normalização de symbol + prevenção de duplicidade 409), listar por usuário e remover (404); Swagger documentado com `@Tag` e `@Operation`.
- [x] Tratamento global de exceções (`GlobalExceptionHandler` + `ErrorResponse`) com respostas de erro padronizadas; email duplicado agora retorna 409 (`ConflictException`).
- [x] `GlobalExceptionHandler` agora estende `ResponseEntityExceptionHandler`, mapeando exceções padrão do Spring MVC (400/405/406/415) em vez de caírem no 500 genérico; inclui `ConstraintViolationException` → 400.
- [x] Autenticação com Google (OAuth2 Resource Server / JWT): `JwtDecoder` valida issuer + audience; `GoogleUserSyncFilter` provisiona/vincula `User` por email (JIT) com `google_sub`; `GET /auth/me`; chains separadas por profile (dev/test com form login + Basic; prod só JWT); CORS configurável.
- [x] `AuthControllerTest` (integração com MockMvc + `jwt()`): valida provisionamento via `/auth/me` e rejeição de token sem email verificado (2 testes passando).
- [x] Login Google validado ponta a ponta no front (ID token aceito pela API; usuário provisionado) em 2026-11-08.
- [x] Autorização por dono: `CurrentUserProvider` deriva o `User` do JWT (email verificado) e bloqueia com 403 o acesso a favoritos de outro usuário.
- [x] Controle de acesso baseado em papéis (RBAC): criado enum `Role` (`USER`, `ADMIN`), bloqueio de `/users/**` exclusivamente para `ADMIN` (impedindo que usuários comuns logados pelo Google deletem ou gerenciem usuários) e permissão de favoritos `/users/*/favorites/**` para `USER` e `ADMIN`. Configuração de admins via `APP_SECURITY_ADMIN_EMAILS`. Testes atualizados em `OwnerAuthorizationTest` (10 testes passando no total da suíte).
- [x] `GET /users` (listagem) mantido, porém restrito a `ADMIN` (o `UserController` inteiro é `@PreAuthorize("hasRole('ADMIN')")`); `UserService.findAll` permanece em uso.
- [x] Swagger documentado em todos os controllers (`AuthController`, `UserController` e `FavoriteController`) com `@Tag`, `@Operation` e `@ApiResponse`.
- [x] Favoritos: `FavoriteController` usa `requireOwnerOrAdmin`, alinhando com as security chains (que permitem `USER` e `ADMIN` em `/users/*/favorites/**`). Dono acessa os próprios favoritos; `ADMIN` pode acessar/gerenciar favoritos de qualquer usuário.
- [x] Grafo global de ativos por coocorrência de favoritos (Ideia 1): `GET /graph` (nós/arestas/pesos), `GET /graph/assets/{symbol}/neighbors` (peso desc), `GET /graph/path?from=SYMBOL&to=SYMBOL` (BFS) e `GET /graph/components` (componentes conexos). Pacote `graph` (`Graph`, `AdjacencyListGraph`, `GraphService`, `GraphController`) + projeção `FavoriteView` no `FavoriteRepository` (sem N+1). Sem mudança de schema e sem dependências novas. `GraphControllerTest` com 10 testes; suíte completa com 20 testes passando.
- [x] Carteira do usuário (simulação): `Portfolio` 1:1 `User` (default "Minha carteira") e `PortfolioItem` (`symbol`, `name`, `quantity` `BigDecimal` 19,8) com unique `(portfolio_id, symbol)`. Endpoints `/users/{userId}/portfolio` (GET/PUT) e `/items` (GET/POST/PUT/DELETE). `POST /items` soma a quantidade quando a cota já existe; `PUT` define a quantidade absoluta. Rotas liberadas para `USER`/`ADMIN` via `requireOwnerOrAdmin`. Sem cálculo de valor/cotação no backend. `PortfolioControllerTest` com 12 testes; suíte completa com 33 testes passando.

Pendente:

- [ ] Cache do grafo no Redis (hoje o grafo é recalculado em memória a cada request)
- [ ] Entidade `Favorite` evoluir com campos de negócio (se necessário)
- [ ] Migrations (Flyway/Liquibase) e remoção de `ddl-auto=update`
- [ ] Testes de integração dos demais endpoints (`/users`, favoritos) e do fluxo com token real

## Git

- Remote `origin` -> https://github.com/jcdev01/Investview-API.git
- Branch `main` com upstream configurado.
- Commits já enviados:
  - `88eb5a3` chore: configura projeto Spring Boot com Docker, Swagger e contexto de IA
  - `bf8ae87` docs: registra commit inicial e remote no contexto de IA
  - `f804dfa` docs: adiciona README do projeto
  - `fc6e782` feat:Implementação do crud da entity User
  - `3ee221e` feat: implementa gerenciamento de favoritos simplificado e documentação Swagger
  - `c99c050` feat: implementa autenticacao Google, controle RBAC e tratamento global de excecoes

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
- **Sem roles (SUPERADO em 2026-11-08):** decisão inicial de ter apenas usuário
  comum, sem papel/admin (`Role` descartado). Foi revertida pela decisão de RBAC
  (`ROLE_ADMIN` vs `ROLE_USER`) registrada abaixo, após constatar que qualquer
  usuário autenticado podia gerenciar `/users`. Mantida aqui apenas como histórico.
- **`User` 1:N `Favorite`:** `User.favorites` com `@OneToMany(mappedBy = "user",
  cascade = ALL, orphanRemoval = true)` e `List<Favorite>` inicializada. Do outro
  lado, `Favorite.user` com `@ManyToOne(fetch = LAZY, optional = false)` e
  `@JoinColumn(name = "user_id")`.
- **Favoritos simplificados (sem Asset nem chamadas externas):** O backend NÃO
  chama APIs externas e NÃO armazena cotações. Guarda apenas a identidade do ativo
  favorito (`symbol` normalizado com `trim().toUpperCase()` e `name` opcional para
  exibição). O frontend consulta as cotações diretamente na brapi usando o `symbol`.
  A API CoinGecko foi descartada, pois criptomoedas também são consultadas na brapi
  (`/api/v2/crypto?coin=BTC`). A entidade `Asset` e o `AssetRepository` foram
  removidos. A tabela `tb_favorites` guarda diretamente os dados do ativo com unique
  `(user_id, symbol)`.
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
- **Tratamento global de exceções:** `GlobalExceptionHandler`
  (`@RestControllerAdvice`) padroniza o corpo de erro via `ErrorResponse`
  (`timestamp`, `status`, `error`, `message`, `path`, `errors`). Mapeamentos:
  `ResourceNotFoundException`→404, `ConflictException`→409,
  `DataIntegrityViolationException`→409 (rede de segurança para unique/FK),
  `MethodArgumentNotValidException`→400 com lista de erros por campo,
  `IllegalArgumentException`→400, `HttpMessageNotReadableException`→400 (JSON/enum
  inválido), `NoResourceFoundException`→404 e `Exception`→500 (mensagem genérica,
  sem vazar detalhes). `UserService.create`/`update` passaram a lançar
  `ConflictException` para email duplicado (antes era `IllegalArgumentException`,
  que virava 500). As anotações `@ResponseStatus` das exceções foram mantidas
  (o handler tem precedência, mas o status é o mesmo).
- **Exceções padrão do Spring MVC:** `GlobalExceptionHandler` passou a estender
  `ResponseEntityExceptionHandler` (Spring 7) para mapear automaticamente
  `MethodArgumentTypeMismatch`→400, `HttpRequestMethodNotSupported`→405,
  `HttpMediaTypeNotSupported`→415, `HttpMediaTypeNotAcceptable`→406,
  `MissingServletRequestParameter`→400, entre outros, evitando que caiam no
  `@ExceptionHandler(Exception.class)` como 500. O corpo continua padronizado via
  `ErrorResponse` (override de `handleExceptionInternal`). Foram sobrescritos
  `handleMethodArgumentNotValid` (lista de erros por campo),
  `handleHttpMessageNotReadable` e `handleNoResourceFoundException` (mensagens em
  português). Adicionado handler de `ConstraintViolationException`→400 (para
  `@Validated` em parâmetros). Importante: não se deve ter `@ExceptionHandler`
  duplicado para tipos já cobertos pelo `ResponseEntityExceptionHandler` (gera
  mapeamento ambíguo); nesses casos, sobrescrever o método `handleXxx`.
  Em Spring Framework 7 a anotação de nulidade é `org.jspecify.annotations.Nullable`.
- **Autenticação com Google (Resource Server):** adotado OAuth2 Resource Server com
  JWT (o front faz login no Google e envia `Authorization: Bearer <id_token>`).
  A API não usa sessão/cookie em produção (`SessionCreationPolicy.STATELESS`).
  `JwtDecoder` usa `jwk-set-uri` do Google (sem chamada de rede no startup) e valida
  issuer (`https://accounts.google.com`) + audience (`APP_OAUTH_GOOGLE_CLIENT_ID`) + timestamp.
  A dependência `spring-boot-starter-security-oauth2-client` foi trocada por
  `spring-boot-starter-security-oauth2-resource-server`.
- **Provisionamento JIT do usuário:** `GoogleUserSyncFilter` roda após o
  `BearerTokenAuthenticationFilter`; para cada request com `JwtAuthenticationToken`
  com `email` e `email_verified=true`, chama `GoogleUserService.provision`, que
  busca o `User` por email, cria se não existir (senha nula) e grava o `google_sub`.
  Isso mantém o vínculo com favoritos (FK `user_id`). `User.password` passou a ser
  nullable e foi adicionado `google_sub` (único, nullable).
  **Correção (2026-11-09):** `provision` é atômico contra corrida de INSERT
  concorrente — executa via `TransactionTemplate` e, se violar o unique de email,
  refaz a busca em transação nova (padrão insert-then-reselect).
- **Profiles de segurança:** `DevSecurityConfig` (`@Profile("!prod")`) mantém
  `admin`/`admin` via `InMemoryUserDetailsManager` + form login + HTTP Basic + JWT,
  e libera H2/Swagger. `ProdSecurityConfig` (`@Profile("prod")`) aceita somente JWT,
  sem usuário em memória e sem H2. `SecurityConfig` guarda beans comuns
  (`PasswordEncoder`, `JwtDecoder`, `CorsConfigurationSource`).
- **CORS:** `CorsConfigurationSource` libera origens de `APP_CORS_ALLOWED_ORIGINS`
  (default `http://localhost:3000,http://localhost:5173`), métodos REST e headers,
  com `allowCredentials=true`. Necessário para a SPA chamar a API com Bearer token.
- **Autorização por dono (IDOR):** `CurrentUserProvider` (pacote `security`) resolve
  o `User` a partir do `JwtAuthenticationToken` no `SecurityContext`, exigindo
  `email` presente e `email_verified=true`, e `requireOwner(userId)` compara o `id`
  do usuário do token com o `userId` da rota. `FavoriteController` (todos os endpoints)
  chama `requireOwnerOrAdmin` antes do service: o dono acessa os próprios favoritos e
  `ADMIN` pode acessar/gerenciar os de qualquer usuário, alinhado às security chains.
  Violação lança `AccessDeniedException`, mapeada para 403 no `GlobalExceptionHandler`.
- **Controle de papéis (RBAC - `ROLE_ADMIN` vs `ROLE_USER`):** para impedir que qualquer
  usuário comum autenticado com Google acesse endpoints administrativos (`/users/**`) e
  possa deletar usuários, foi criado o enum `Role` (`USER`, `ADMIN`) na entidade `User`.
  Usuários provisionados via Google recebem por padrão `Role.USER`. O sistema permite
  configurar e-mails de administradores via `APP_SECURITY_ADMIN_EMAILS` (em `application.properties`
  e `.env`), atribuindo `Role.ADMIN` aos e-mails correspondentes. As security chains
  (`DevSecurityConfig` e `ProdSecurityConfig`) e o `UserController` exigem `hasRole('ADMIN')`
  para `/users/**`, enquanto `/users/*/favorites/**` permanece liberado para `USER` e `ADMIN`
  (com `requireOwner`). O `GoogleUserSyncFilter` injeta a autoridade `ROLE_<ROLE>` no
  `SecurityContextHolder`. O `UserResponse` passa a exportar o campo `role`.
- **Grafo global de ativos (Ideia 1):** grafo derivado apenas dos favoritos (backend não chama
  APIs externas; o front continua consultando cotações apenas na brapi). Nó = ativo com id
  igual ao `symbol`; aresta não direcionada = dois ativos favoritados pelo mesmo usuário
  (ordem dos ids normalizada em `EdgeKey`); peso = nº de usuários distintos do par
  (unique `(user_id, symbol)` garante pares únicos por usuário). Montagem no
  `GraphService` via projeção `FavoriteView` (`findAllWithUserId` ordenado por user/id,
  sem N+1), agrupamento por usuário em `LinkedHashMap` (ordem determinística) e combinação
  de pares; `name` do nó = primeiro não nulo encontrado. Algoritmos em Java puro (sem libs):
  BFS com predecessores para caminho mínimo, DFS iterativo para componentes conexos.
  Endpoints em `/graph` exigem apenas autenticação (caem no `anyRequest().authenticated()`)
  e expõem só agregados, nunca quem favoritou. Erros: `from`/`to` vazio → 400
  (`IllegalArgumentException` via `GlobalExceptionHandler`); ativo inexistente no grafo →
  404 (`ResourceNotFoundException`)
  — escolha documentada no Swagger; sem caminho → 200 com `found=false`. Vizinhos ordenados
  por peso desc (desempate por id); componentes ordenados por tamanho desc. Pacote `graph`
  separado (`Graph`, `AdjacencyListGraph`, `GraphService`, `GraphController`) conforme
  `docs/GRAPH_PROMPT.md`; DTOs em `dto/`. Cache no Redis é evolução futura.
- **Carteira do usuário (simulação):** `Portfolio` 1:1 `User` (unique `user_id`,
  criada sob demanda com nome padrão "Minha carteira") e `PortfolioItem`
  (`symbol`, `name`, `quantity` `BigDecimal` 19,8; unique `(portfolio_id, symbol)`).
  `POST /items` soma a quantidade quando a cota já existe (upsert), `PUT /items/{itemId}`
  define a quantidade absoluta. Rotas `/users/*/portfolio/**`
  liberadas para `USER` e `ADMIN` (dono ou admin) com `requireOwnerOrAdmin`,
  alinhadas às chains. Sem cálculo de valor: o backend não busca cotações; devolve
  apenas `symbol`, `name` e `quantity`. Deleção de usuário/carteira usa
  `@OnDelete(CASCADE)` no FK. Testes em `PortfolioControllerTest` (12).
- **Remoção do `AssetType` (2026-11-12):** a brapi não separa os ativos em
  FII/STOCK/ETF/CRYPTO, então o tipo deixou de ser útil. `AssetType` foi removido e
  a identidade do ativo passou a ser apenas o `symbol` em favoritos, carteira e
  grafo. Rotas do grafo simplificadas para `/graph/assets/{symbol}/neighbors` e
  `/graph/path?from=SYMBOL&to=SYMBOL`. Em bancos existentes, remover a coluna
  `type` manualmente (o `ddl-auto=update` não dropa colunas).

## Observações do ambiente

- **Migração 2026-11-12 (remoção de `type`):** em bancos já existentes, remover a
  coluna `type` de `tb_favorites` e `tb_portfolio_items` (o Hibernate não dropa
  colunas). Exemplo PostgreSQL: `ALTER TABLE tb_favorites DROP COLUMN type;` e
  `ALTER TABLE tb_portfolio_items DROP COLUMN type;`; reinicie a app para o
  Hibernate criar as constraints novas `(user_id, symbol)` e
  `(portfolio_id, symbol)`. Se houver símbolos duplicados por usuário/carteira,
  deduplicar antes.
- **Deploy de teste (2026-11-10):** `https://investview-api-9abc6.containers.snapdeploy.app`
  (snapdeploy), rodando com `SPRING_PROFILES_ACTIVE=test` (smoke test). H2 console em
  `/h2-console/` (exige `spring.h2.console.settings.web-allow-others=true` para acesso
  remoto, adicionado em 2026-11-10). JDBC `jdbc:h2:mem:investview`, usuário `sa`, senha vazia.
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

### 2026-11-12
- Releitura do projeto e do contexto de IA (pom, properties test/prod, Docker,
  segurança dev/prod, entidades, DTOs, repositórios, services, controllers,
  pacote `graph` e testes). Confirmado: 21 `@Test`; arquitetura em camadas e
  funcionalidades (Google OAuth2/JIT, RBAC, IDOR, favoritos, grafo) batem com
  este contexto. Divergências conhecidas seguem: `README.md`/`ONBOARDING.md`
  desatualizados (sem entidades/endpoints, citam OAuth2 Client e credenciais
  padrão embutidas); `spring.profiles.active` e `APP_CORS_ALLOWED_ORIGINS` sem
  default; `requireAdmin()` sem uso; Swagger público em `prod`; H2 exposto no
  profile `test`. `.env` não lido em conteúdo.
- Criado (a pedido) `docs/FRONT_TEST_PROMPT.md`: prompt completo para uma IA
  construir uma SPA de testes (React + Vite + TS) cobrindo login Google,
  `/auth/me`, favoritos, grafo (vizinhos, caminho e componentes), CRUD admin de
  usuários e uma página de diagnóstico de todas as rotas. Inclui contratos,
  regras de negócio, configuração de CORS/Google, formato de erro e checklist
  de aceite. Nenhuma alteração no backend.
- Implementada a entidade de carteira (a pedido): `Portfolio` 1:1 `User` +
  `PortfolioItem` (type/symbol/name/quantity `BigDecimal`). Endpoints
  `/users/{userId}/portfolio` (GET/PUT) e `/items` (GET/POST/PUT/DELETE);
  `POST` soma a quantidade de uma cota existente e `PUT` define a absoluta.
  Segurança `USER`/`ADMIN` + `requireOwnerOrAdmin`; `@OnDelete(CASCADE)` nos FKs.
  Criado `PortfolioControllerTest` (12 testes). Suíte total: 33 testes passando
  via `cmd.exe /c mvnw.cmd test`.
- Criado `docs/FRONT_TEST_CARTEIRA_PROMPT.md` com um prompt curto para adicionar
  a tela de carteira ao front de testes (contrato, UI e checklist).
- Removido o `AssetType` (a pedido): a brapi não separa FII/STOCK/ETF/CRYPTO.
  `Favorite` e `PortfolioItem` passaram a usar só `symbol` (unique
  `(user_id, symbol)` e `(portfolio_id, symbol)`); DTOs, repositórios, services e
  testes ajustados. Grafo agora usa o próprio `symbol` como id do nó, com rotas
  `/graph/assets/{symbol}/neighbors` e `/graph/path?from=SYMBOL&to=SYMBOL`.
  Prompts do front e contexto atualizados. Suíte: 33 testes passando.

### 2026-11-11
- Leitura completa do projeto (pom, properties, Docker, docs, todo o código-fonte
  e os 5 arquivos de teste) para carregar o contexto. Confirmado: 21 testes
  (`contextLoads` + 2 Auth + 7 Owner + 10 Graph + 1 concorrência). O `.env` não
  foi lido em conteúdo (apenas os nomes das variáveis), por conter segredos.
- Divergências observadas (docs desatualizados, sem alteração de código):
  `README.md` e `docs/ONBOARDING.md` ainda dizem que não há entidades/endpoints
  de negócio, citam "OAuth2 Client", credenciais padrão embutidas e testes que
  exigem PostgreSQL — o código atual usa OAuth2 Resource Server, H2 no profile
  `test` e já possui `User`/`Favorite`/grafo. `ONBOARDING.md` também lista
  defaults de env que não existem mais. Pendências já conhecidas seguem:
  `spring.profiles.active` e `APP_CORS_ALLOWED_ORIGINS` sem default,
  `requireAdmin()` sem uso, Swagger público em `prod` e H2 console exposto no
  profile `test`.

### 2026-11-10
- Orientado deploy no hosting: usuário havia subido com `SPRING_PROFILES_ACTIVE=test`
  (riscos: H2 volátil perdendo dados em restart, H2 console público, login dev
  form+Basic exigindo `APP_SECURITY_*`, CORS default localhost). Recomendado
  `prod` (ou apagar a variável para usar o default da imagem), `DB_*` no Postgres
  externo, `REDIS_*`, `APP_CORS_ALLOWED_ORIGINS` com a URL real do front e remoção
  das `APP_SECURITY_*` do painel. Esclarecido depois que o deploy em `test` é
  intencional (smoke test do hosting): aceito como uso legítimo, com ressalvas
  (dados voláteis por restart, H2 console público, CORS default cobre
  localhost:5173/3000 para testar do front local).
- Erro "Failed to fetch" no front (GET /auth/me) diagnosticado: backend local
  fora do ar (nada na 8080) e o front apontava para `localhost:8080` em vez da
  URL do hosting. Resolvido pelo usuário ao corrigir a URL base do front para
  o provedor. Lição para o front de testes: URL da API deve ser configurável
  (local vs deploy).
- Análise do projeto x contexto (a pedido): código e contexto batem em quase tudo
  (camadas, auth Google/JIT, RBAC, IDOR, favoritos, grafo, 21 testes). Divergências
  encontradas:
  - **Docs referenciados não existem no repositório:** a estrutura deste arquivo
    lista `docs/REQUISITOS.md`, `USER_STORIES.md`, `BDD.md`, `PLANILHA_DE_TESTES.md`,
    `GRAPH_PROMPT.md`, `GRAPH_VIEW_PROMPT.md` e `GRAFO_EXPLICACAO.txt`, mas `docs/`
    contém apenas `AI_CONTEXT.md` e `ONBOARDING.md` (nunca foram commitados ou se
    perderam). Precisa recriar ou remover as referências.
  - **CORS:** este arquivo ainda diz default `localhost:3000/5173`, porém
    `app.cors.allowed-origins=${APP_CORS_ALLOWED_ORIGINS:}` está sem default
    (lista vazia se a env faltar).
  - **Profile default:** este arquivo diz `test` por padrão, mas
    `spring.profiles.active=${SPRING_PROFILES_ACTIVE}` está sem default (falha no
    startup sem a env; a imagem Docker define `prod`).
  - **`.env.example`:** uma decisão diz que ele é versionado, mas outra registra
    sua remoção do Git (hoje ele não existe). Texto precisa ser unificado.
  - `requireAdmin()` em `CurrentUserProvider` está sem uso; Swagger é público
    também no profile `prod` (avaliar se é intencional).
  - WSL segue sem Java/Maven/Docker, então não foi possível rodar `./mvnw test`
    nesta análise (só leitura de código/contagem: 21 `@Test`).
- Diagnóstico "Failed to fetch" no front (`GET /auth/me`) no deploy:
  `https://investview-api-9abc6.containers.snapdeploy.app`. Backend está no ar
  (`/auth/me` sem token → 401; `/v3/api-docs` → 200), porém QUALQUER `Origin`
  (localhost:5173, localhost:3000, example.com) recebe **403 "Invalid CORS request"**
  sem `Access-Control-Allow-Origin`. Causa: `APP_CORS_ALLOWED_ORIGINS` vazio/ausente
  no painel do provedor (o default no código é vazio). Correção: definir
  `APP_CORS_ALLOWED_ORIGINS` com a(s) origem(ns) do front (ex.:
  `http://localhost:5173`) e reiniciar/redeploy; depois validar com
  `curl -H "Origin: ..."`.
- H2 console no deploy: `GET /h2-console/` abre (200), mas o login (`POST
  /h2-console/login.do`) retorna **403 "Invalid CORS request"**. Causa: o browser
  envia `Origin: https://investview-api-9abc6.containers.snapdeploy.app` no POST e
  essa origem não está em `APP_CORS_ALLOWED_ORIGINS` (não tem a ver com o
  `jsessionid`/URL mudar). Validado: POST sem `Origin` → 200; POST com `Origin` do
  deploy → 403. **Decisão do usuário:** isentar o H2 do CORS em vez de adicionar a
  URL da API à lista. Implementado em `SecurityConfig`: registro dedicado de
  `/h2-console/**` no `CorsConfigurationSource` com origens/métodos/headers
  permissivos (`allowedOriginPatterns=*`, `allowCredentials=false`), mantendo o
  `/**` restrito a `APP_CORS_ALLOWED_ORIGINS`. Requer redeploy para valer; WSL sem
  Java, então não compilado/testado aqui.
- Informada a URL do provedor de deploy: `https://investview-api-9abc6.containers.snapdeploy.app`.
  Ao tentar acessar `/h2-console/`, o H2 respondeu "Sorry, remote connections
  ('webAllowOthers') are disabled on this server." (HTTP 200). Corrigido adicionando
  `spring.h2.console.settings.web-allow-others=true` em `application-test.properties`
  (só profile test, deploy de smoke test). Requer rebuild/redeploy da imagem para valer.
- Criado (a pedido) `docs/REQUISITOS.md` consolidando os principais requisitos
  do projeto: 17 funcionais (auth Google/JIT, `/auth/me`, CRUD admin de
  `/users`, favoritos com 409/404, grafo completo), 16 não funcionais
  (segurança stateless/RBAC/IDOR, segredos em `.env`, sem N+1, camadas,
  profiles test/prod, Docker, testes) e 12 de domínio (enum `AssetType`,
  identidade `(type, symbol)`, normalização de symbol, peso = usuários
  distintos, BFS não ponderado, anonimato no grafo, papéis USER/ADMIN).
  Requisitos derivados do código/décisões existentes; seção de pendências
  relacionadas (cache Redis, Flyway, testes) incluída ao final.
- Criado (a pedido) `docs/USER_STORIES.md` com os principais requisitos
  funcionais reescritos como 13 estórias de usuário (US01-13) em 5 épicos
  (autenticação/identidade, usuários-admin, favoritos, grafo, experiência do
  desenvolvedor), formato "Como/Quero/Para" com critérios de aceite
  verificáveis e tabela de rastreabilidade US → RF → endpoint.
- Criado (a pedido) `docs/PLANILHA_DE_TESTES.md`: planilha de teste em
  markdown estruturada para conversão posterior em Excel/Sheets (tabelas por
  aba: Auth, Users, Favorites, Security/IDOR, Graph, Docs, Resumo). 48 casos
  (TC-001..TC-048) com colunas ID, caso, rastreabilidade (US/RF), pré-condições,
  passos/dados, resultado esperado, prioridade, status ("Não executado") e obs.
  Baseada nos cenários BDD; inclui massa de teste sugerida para o grafo,
  legenda de status/prioridade e resumo executivo com contadores.
- Criados (a pedido) `Dockerfile` e `.dockerignore` para hospedar **somente a
  API** (o banco ficará hospedado em outro lugar): build multi-stage com
  `maven:3.9-eclipse-temurin-25` (build) e runtime `eclipse-temurin:25-jre-alpine`
  (usuário não-root `spring`, `EXPOSE 8080`, `JAVA_OPTS` opcional em runtime).
  A imagem define `SPRING_PROFILES_ACTIVE=prod` como padrão (overridável com
  `-e`); `JAVA_OPTS` não é declarado como `ENV` para o serviço de hospedagem
  não exigi-lo — se informado no painel/`docker run -e`, o ENTRYPOINT o aplica
  (ex.: `-XX:MaxRAMPercentage=75.0`). Conexões
  com Postgres/Redis externos entram por env (`DB_URL`, `DB_USERNAME`,
  `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `APP_OAUTH_GOOGLE_CLIENT_ID`,
  `APP_CORS_ALLOWED_ORIGINS`, `APP_SECURITY_ADMIN_EMAILS`). O `.dockerignore`
  garante que `.env`, `target`, `.git`, `docs` etc. não entrem na imagem.
  Build/run a validar no Windows (Docker Desktop): `docker build -t investview-api .`
  e `docker run -p 8080:8080 --env-file .env.docker investview-api` (com as env
  apontando para o banco externo). Não executável no WSL (sem Docker CLI).
- Configuração OAuth do Google movida para o ambiente (a pedido do usuário):
  `application.properties` agora apenas referencia `APP_OAUTH_GOOGLE_ISSUER`,
  `APP_OAUTH_GOOGLE_JWK_SET_URI` e `APP_OAUTH_GOOGLE_CLIENT_ID` — sem defaults
  embutidos e sem os antigos nomes `GOOGLE_*` (nomes derivados das props
  `app.oauth.google.*`, exigidos pelo serviço de hospedagem). O `.env` local foi
  atualizado para os novos nomes (valor do client ID preservado; issuer
  `https://accounts.google.com`; JWK Set
  `https://www.googleapis.com/oauth2/v3/certs`; corrigida colagem acidental de
  linha por falta de newline no final do arquivo). Env vars do hosting têm
  precedência sobre o `application.properties`; sem `.env`/env vars a
  aplicação falha no startup (fail fast), alinhado à convenção de "sem
  defaults embutidos". No hosting também são obrigatórias `DB_*`,
  `REDIS_HOST/PORT` e `APP_CORS_ALLOWED_ORIGINS` (URL do front real).
  Validado com `cmd.exe /c mvnw.cmd test`: 21 testes passando.
- Criado (a pedido) `docs/BDD.md` com as user stories transformadas em
  comportamento BDD (Gherkin em português: Funcionalidade/Cenário/
  Dado/Quando/Então): 6 funcionalidades (auth Google + JIT, CRUD admin,
  favoritos, proteção IDOR, grafo com Contexto de dados de exemplo, dev
  experience), ~35 cenários cobrindo caminhos felizes e erros (401/403/404/
  409/400), rastreabilidade BDD → US → RF. Documentação apenas; nenhum teste
  automatizado de Gherkin implementado.

### 2026-11-09
- Implementado o grafo global de ativos por coocorrência de favoritos (Ideia 1 do
  `docs/GRAPH_PROMPT.md`): pacote `graph` com `Graph` (interface), `AdjacencyListGraph`
  (Maps + BFS + DFS), `GraphService` (montagem via projeção `FavoriteView`, sem N+1) e
  `GraphController` (`GET /graph`, `GET /graph/assets/{type}/{symbol}/neighbors`,
  `GET /graph/path`, `GET /graph/components`, todos autenticados, Swagger documentado).
  DTOs `GraphNode`, `GraphEdge`, `GraphResponse`, `NeighborResponse`, `PathResponse`.
  Sem mudança de schema e sem dependências novas; backend segue sem chamar APIs
  externas (cotações continuam só na brapi, no front). Erros: formato `TYPE:SYMBOL`
  inválido → 400; ativo inexistente no grafo → 404; sem caminho → 200 com `found=false`.
  Criado `GraphControllerTest` (10 testes: grafo vazio, nós/arestas/pesos somados por
  usuários distintos, ativo isolado, vizinhos por peso desc, BFS, desconexão, 404, 400,
  componentes, 401). Suíte completa: 20 testes passando via `cmd.exe /c mvnw.cmd test`.
- Reposicionados `GraphService` → `service/` e `GraphController` → `controller/`
  para padronizar com a arquitetura em camadas do resto do projeto (o
  `docs/GRAPH_PROMPT.md` sugeria tudo em `graph/`). O pacote `graph/` ficou apenas
  com a estrutura de dados e algoritmos (`Graph`, `AdjacencyListGraph`), que não tem
  camada equivalente (não são entity/dto/service/controller). Imports ajustados;
  20 testes continuam passando.
- Criado (a pedido) o prompt `docs/GRAPH_VIEW_PROMPT.md` para outra IA gerar um
  front visual de testes do grafo: SPA única React+Vite+TS com login Google
  (Bearer) ou Basic (dev), painel para popular usuários/favoritos, grafo
  renderizado (cores por `AssetType`, espessura/label por peso), vizinhos ao
  clicar no nó, menor caminho com highlight, componentes, tratamento de
  401/400/404/409 e checklist de aceite. Cotações brapi opcionais. Nenhuma
  alteração no backend.
- Criado `docs/GRAFO_EXPLICACAO.txt` (a pedido) com a explicação em texto puro
  do funcionamento do grafo: vértice, aresta, peso (usuários distintos com o
  par), vizinho, grau, menor caminho (BFS) e componente conexo.
- **Fix de condição de corrida no provisionamento JIT (Google):** o front
  disparava requests simultâneos (ex.: `/auth/me` + `/graph` no login) e duas
  threads passavam pelo `findByEmail` antes de qualquer commit; ambas tentavam
  INSERT e uma violava o unique de `email` em `tb_users`
  (`DataIntegrityViolationException` 500, estourando no `GoogleUserSyncFilter`,
  fora do alcance do `GlobalExceptionHandler`). Correção em
  `GoogleUserService.provision`: sem `@Transactional` no método público; a
  lógica roda via `TransactionTemplate` e, em caso de
  `DataIntegrityViolationException`, repete a busca/insert em transação NOVA
  (quem perde a corrida acha a linha de quem ganhou). Teste novo
  `GoogleUserServiceConcurrencyTest` (8 threads simultâneas no mesmo email,
  sem duplicar/falhar). Suíte: 21 testes passando.
- Corrigidas divergências entre o código e este contexto (análise do projeto):
  - `UserController` documentado no Swagger (`@Tag`, `@Operation`, `@ApiResponse`
    em todos os endpoints). A pendência de documentação de `AuthController` já
    estava concluída antes.
  - `FavoriteController` passou a usar `requireOwnerOrAdmin` (antes `requireOwner`),
    alinhando com as security chains que permitem `USER` e `ADMIN` em
    `/users/*/favorites/**`. Assim o `ADMIN` deixa de receber 403 ao acessar
    favoritos de outro usuário.
  - Contexto corrigido: `GET /users` e `UserService.findAll` **não** foram removidos
    (a entrada de 2026-11-08 estava desatualizada); a listagem existe e é restrita a
    `ADMIN`. `UserController` é protegido por RBAC (`@PreAuthorize`), e não por
    `requireOwner` como constava. Referência ao inexistente `docs/FRONT_TEST_PROMPT.md`
    removida da estrutura. Decisão "Sem roles" marcada como superada.
  - Adicionado teste `shouldAllowAdminToAccessAnotherUserFavorites` em
    `OwnerAuthorizationTest`, cobrindo o acesso de `ADMIN` aos favoritos de outro
    usuário. Suíte com 10 testes passando.
  - Validação via Windows (`cmd.exe /c mvnw.cmd test`): `compile` OK e 10 testes
    passando (1 contexto + 2 Auth + 7 autorização).
- Discutida a implementação de uma estrutura de grafos no projeto. Escolhida a
  **Ideia 1 — grafo global de ativos por coocorrência de favoritos**: nós = ativos
  (`TYPE:SYMBOL`), arestas entre ativos favoritados pelo mesmo usuário e peso = nº de
  usuários distintos que favoritaram o par. Sem mudança de schema e sem dependências
  novas. Gerado um prompt detalhado de implementação (contratos dos endpoints `/graph`,
  BFS para caminho mínimo, componentes conexos e testes), salvo em `docs/GRAPH_PROMPT.md`.

### 2026-11-08
- Removido o arquivo `.env.example` do repositório Git e enviado push para o GitHub a
  pedido do usuário, mantendo a documentação das variáveis apenas no contexto de IA.
- Implementado controle de papéis (RBAC: `ROLE_ADMIN` vs `ROLE_USER`) para resolver o
  problema em que qualquer usuário autenticado com Google tinha acesso às rotas de `/users`
  e podia deletar usuários. Criado enum `Role` (`USER`, `ADMIN`), adicionado campo `role`
  na entidade `User` e em `UserResponse`. O `GoogleUserService` atribui `Role.ADMIN` caso
  o e-mail conste em `APP_SECURITY_ADMIN_EMAILS` (ou `Role.USER` como padrão). O
  `GoogleUserSyncFilter` popula `ROLE_<ROLE>` nas authorities do `SecurityContext`.
  `UserController` foi anotado com `@PreAuthorize("hasRole('ADMIN')")`, e as chains de
  segurança foram configuradas para exigir `hasRole('ADMIN')` em `/users/**` e permitir
  `hasAnyRole('USER', 'ADMIN')` em `/users/*/favorites/**`. O `OwnerAuthorizationTest`
  foi atualizado validando que usuários comuns recebem 403 Forbidden ao tentar deletar ou
  acessar `/users/**`, enquanto administradores conseguem gerenciar e deletar usuários.
  Todos os 9 testes passando com sucesso.
- Implementada autorização por dono (correção de IDOR): criado `CurrentUserProvider`
  que resolve o `User` do JWT (email + `email_verified=true`) e `requireOwner(userId)`
  para comparar com o `id` da rota. Aplicado em `UserController` (GET/PUT/DELETE
  `/users/{id}`) e `FavoriteController` (adicionar/listar/remover). Adicionado handler
  de `AccessDeniedException`→403 no `GlobalExceptionHandler`. Removido `GET /users`
  (listagem de todos) e `UserService.findAll`, por expor dados de todos e não haver
  admin. Criado `OwnerAuthorizationTest` (MockMvc + `jwt()`) com 6 testes: acesso ao
  próprio perfil/favoritos OK, acesso a outro usuário/favoritos 403, PUT/DELETE de
  outro 403 e token sem email verificado 403. `mvnw.cmd test` no Windows: 9 testes
  passando (1 contexto + 2 Auth + 6 autorização). `docs/FRONT_TEST_PROMPT.md`
  atualizado (sem `GET /users`, com 403).
- Implementada autenticação com Google via OAuth2 Resource Server (JWT), conforme
  decisões do usuário: front envia `Bearer <id_token>`; usuários persistidos em
  `tb_users` (provisionamento JIT por email + `google_sub`); login de dev
  `admin`/`admin` mantido apenas fora de `prod`. Adicionados: `GoogleUserService`,
  `GoogleUserSyncFilter`, `AuthController` (`GET /auth/me`), `DevSecurityConfig`,
  `ProdSecurityConfig`, `JwtDecoder` com issuer+audience, CORS configurável e
  `.env.example` (`GOOGLE_CLIENT_ID`, `APP_CORS_ALLOWED_ORIGINS`). Removido o
  starter `oauth2-client`. `User.password` agora é nullable. Compilado e testado
  (`mvnw.cmd compile`/`test`) com sucesso. Adicionado `GOOGLE_CLIENT_ID` ao `.env`
  (client_id do projeto `api-investview`); o `client_secret` **não** foi armazenado
  e deve ser rotacionado (foi exposto no chat). Criado `AuthControllerTest` (MockMvc
  + `jwt()`): 2 testes passando (provisionamento e rejeição de email não verificado).
- `GlobalExceptionHandler` refatorado para estender `ResponseEntityExceptionHandler`:
  exceções padrão do Spring MVC passam a retornar 400/405/406/415 corretamente (antes
  caíam no 500 genérico do `@ExceptionHandler(Exception.class)`). Overrides de
  `handleExceptionInternal` (corpo `ErrorResponse`), `handleMethodArgumentNotValid`
  (erros por campo), `handleHttpMessageNotReadable` e `handleNoResourceFoundException`
  (mensagens em PT). Adicionado `ConstraintViolationException`→400. Compilado e
  testado via `mvnw.cmd` no Windows (WSL sem Java): `compile` e `test` OK.
- Discutido o papel do Redis: hoje só provisionado (dependência + `REDIS_*`), sem
  uso. Papel previsto: cache de leitura (favoritos/usuário) via Spring Cache +
  Redis, cache de cotações com TTL curto (só se o backend passar a chamar a
  brapi) e sessão/segurança quando a auth migrar para OAuth2/banco. Confirmado
  que Redis é server-side: se o front chama a brapi direto, o cache é no browser,
  não no Redis.
- Discutido atributo de "logado/ativo" no `User`: "logado" é estado de sessão
  (não deve ir para a entidade; se quiser "online", usar Redis com TTL). "Ativo"
  (`enabled`) só faz sentido junto com a migração da auth para o banco, pois hoje
  a auth é `InMemoryUserDetailsManager`. Decisão adiada.
- Implementado tratamento global de exceções: criados `ErrorResponse` (record) e
  `GlobalExceptionHandler` (`@RestControllerAdvice`). `UserService.create`/`update`
  agora lançam `ConflictException` (409) em email duplicado. Handlers para 400
  (Bean Validation, JSON inválido, `IllegalArgumentException`), 404, 409
  (inclui `DataIntegrityViolationException`) e 500 genérico.
- Não foi possível compilar/testar no WSL (sem Java/Maven); validar no IntelliJ
  com `./mvnw test`.
- Criado (a pedido) um prompt para gerar um front-end **mínimo** de testes
  (React + Vite + TypeScript, tela única) cobrindo login Google via ID token
  (Bearer), `/auth/me`, CRUD básico de `/users` e favoritos, com contrato da API,
  configuração de CORS/Google Cloud e checklist de aceite. Salvo em
  `docs/FRONT_TEST_PROMPT.md`. Nenhuma alteração no backend.
- **Login Google validado ponta a ponta:** front (OAuth Client ID tipo Web +
  `http://localhost:5173` como origem autorizada) obteve o ID token e a API o
  aceitou (issuer/audience), provisionando o usuário. Auth via Google confirmada
  como funcionando.

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

1. ~~Criar credenciais no Google Cloud Console e validar o login no front.~~
   **Concluído em 2026-11-08:** login Google validado ponta a ponta.
2. ~~Implementar autorização por dono e RBAC (`ROLE_ADMIN` vs `ROLE_USER`).~~
   **Concluído em 2026-11-08:** `CurrentUserProvider` protege favoritos contra IDOR e
   `/users/**` é restrito a administradores (usuários normais recebem 403 Forbidden).
3. ~~Escrever testes de integração com JWT mockado.~~
   **Concluído em 2026-11-08:** 9 testes automatizados cobrindo provisionamento, token
   inválido, controle de dono e isolamento administrativo. **Atualizado em 2026-11-09:**
   10 testes (adicionado `shouldAllowAdminToAccessAnotherUserFavorites`).
4. ~~Commitar as alterações acumuladas.~~
   **Concluído em 2026-11-08:** commit `c99c050`.
5. ~~Documentar `UserController` e `AuthController` no Swagger.~~
   **Concluído em 2026-11-09:** todos os controllers (`AuthController`, `UserController`,
   `FavoriteController`) documentados com `@Tag`, `@Operation` e `@ApiResponse`.
6. Avaliar Flyway/Liquibase e remover `ddl-auto=update` antes de produção.
7. Implementar cache Redis de favoritos/usuário (quando fizer sentido); incluir o grafo
   (`/graph/**`) como candidato ao cache, já que hoje é recalculado a cada request.
8. ~~Implementar o grafo global de ativos por coocorrência de favoritos (Ideia 1),
    conforme `docs/GRAPH_PROMPT.md` (endpoints `/graph`, BFS, componentes e testes).~~
    **Concluído em 2026-11-09:** endpoints `/graph`, `/graph/assets/{type}/{symbol}/neighbors`,
    `/graph/path` e `/graph/components` implementados e testados (20 testes passando).
9. Consumir a carteira no front de testes: seção adicionada ao
   `docs/FRONT_TEST_PROMPT.md` e prompt curto em
   `docs/FRONT_TEST_CARTEIRA_PROMPT.md` (2026-11-12).
