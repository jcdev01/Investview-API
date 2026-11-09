# Contexto de IA — Investview-API

> Memória persistente das nossas conversas. Mantido automaticamente pelo agente
> conforme as regras do `AGENTS.md`. Não apague o histórico; apenas acrescente.
>
> **Status: em desenvolvimento ativo (retomado em 2026-11-08).**
> Autenticação Google OAuth2 Resource Server, tratamento de exceções, proteção
> contra IDOR e controle de acesso RBAC (`ROLE_ADMIN` vs `ROLE_USER`) implementados.

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
| `GOOGLE_CLIENT_ID`      | client ID do OAuth2/Google (audience do token)   |
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
│   └── FavoriteController.java     -> favoritos por usuário (Swagger documentado)
├── dto/
│   ├── UserCreateRequest.java      -> record de entrada (create) + validação
│   ├── UserUpdateRequest.java      -> record de entrada (update) + validação
│   ├── UserResponse.java           -> record de saída (sem senha)
│   ├── FavoriteRequest.java        -> record de entrada (symbol, type, name) + validação
│   └── FavoriteResponse.java       -> record de saída (id, symbol, type, name)
├── entity/
│   ├── User.java                   -> entidade JPA de usuário (1:N favorites, role USER/ADMIN)
│   ├── Role.java                   -> enum de perfis de acesso (USER, ADMIN)
│   ├── AssetType.java              -> enum (STOCK, FII, ETF, CRYPTO)
│   └── Favorite.java               -> favorito do usuário (user, symbol, type, name)
├── exception/
│   ├── ResourceNotFoundException.java -> recurso não encontrado (404)
│   ├── ConflictException.java      -> conflito de duplicidade (409)
│   ├── ErrorResponse.java          -> payload padrão de erro (timestamp/status/erro/mensagem/path/errors)
│   └── GlobalExceptionHandler.java -> @RestControllerAdvice com mapeamento de erros → HTTP
├── repository/
│   ├── UserRepository.java         -> JpaRepository<User, Long>
│   └── FavoriteRepository.java     -> JpaRepository<Favorite, Long>
└── service/
    ├── UserService.java            -> CRUD de usuário + hash de senha
    ├── GoogleUserService.java      -> provisiona/vincula usuário do Google por email
    └── FavoriteService.java        -> adicionar/listar/remover favoritos
src/main/resources/application.properties
src/test/java/jairo/investviewapi/
├── InvestviewApiApplicationTests.java -> teste de contexto
└── controller/
    ├── AuthControllerTest.java        -> testes de login Google via MockMvc + jwt()
    └── OwnerAuthorizationTest.java    -> testes de IDOR e RBAC (USER vs ADMIN)
docker-compose.yml                  -> PostgreSQL + Redis
README.md                           -> instruções gerais
AGENTS.md                           -> convenções para agentes de IA
docs/AI_CONTEXT.md                  -> este arquivo (memória)
docs/ONBOARDING.md                  -> resumo para novo colaborador
docs/GRAPH_PROMPT.md                -> prompt/especificação do grafo de ativos (Ideia 1)
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

Pendente:

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
  issuer (`https://accounts.google.com`) + audience (`GOOGLE_CLIENT_ID`) + timestamp.
  A dependência `spring-boot-starter-security-oauth2-client` foi trocada por
  `spring-boot-starter-security-oauth2-resource-server`.
- **Provisionamento JIT do usuário:** `GoogleUserSyncFilter` roda após o
  `BearerTokenAuthenticationFilter`; para cada request com `JwtAuthenticationToken`
  com `email` e `email_verified=true`, chama `GoogleUserService.provision`, que
  busca o `User` por email, cria se não existir (senha nula) e grava o `google_sub`.
  Isso mantém o vínculo com favoritos (FK `user_id`). `User.password` passou a ser
  nullable e foi adicionado `google_sub` (único, nullable).
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

### 2026-11-09
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
7. Implementar cache Redis de favoritos/usuário (quando fizer sentido).
8. Implementar o grafo global de ativos por coocorrência de favoritos (Ideia 1),
   conforme `docs/GRAPH_PROMPT.md` (endpoints `/graph`, BFS, componentes e testes).
