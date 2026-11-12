# Prompt — Front-end de testes da Investview-API

> Copie o bloco abaixo inteiro para a IA que vai construir o front.
> O front é uma SPA separada; **não altera o backend**.

---

## Contexto para a IA

Você vai construir uma **SPA de testes manuais** para a **Investview-API**, uma API REST
de investimentos em Java 25 + Spring Boot. O objetivo é exercitar **todas as rotas** da API
por uma interface amigável: login com Google, dados do usuário, CRUD de favoritos,
simulação de carteira (cotas com quantidade), visualização do grafo de ativos e uma área
administrativa de usuários. O backend já está pronto; você **não deve alterá-lo**.

Entregue um projeto completo, compilando e rodando, com README de execução.

## Stack obrigatória

- **React 18+ + Vite + TypeScript**
- **React Router** para navegação
- **@react-oauth/google** para o login Google (Google Identity Services / ID token)
- **@xyflow/react (React Flow)** para renderizar o grafo (alternativa aceitável:
  `react-force-graph-2d`; escolha uma e justifique no README)
- **Tailwind CSS** ou CSS Modules (sem framework pesado de UI)
- Sem Redux; use React Context + hooks para auth e estado simples
- Sem chamadas de cotação no backend (a API não fornece cotações; se quiser, consulte a
  brapi **direto do navegador**, mas isso é opcional e não pode quebrar o app)

## Configuração de ambiente

Crie `.env.local` (não commitar valores reais):

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_GOOGLE_CLIENT_ID=SEU_CLIENT_ID.apps.googleusercontent.com
```

- `VITE_API_BASE_URL` deve ser configurável para alternar entre local e deploy.
- Deploy conhecido de smoke test: `https://investview-api-9abc6.containers.snapdeploy.app`
- **CORS:** a API só aceita as origens configuradas em `APP_CORS_ALLOWED_ORIGINS`.
  Para rodar em `http://localhost:5173`, essa origem precisa estar liberada no backend.
  Se aparecer `403 Invalid CORS request` (sem JSON), é configuração de CORS, não bug do front.
- **Google Cloud:** crie um OAuth Client ID do tipo **Web**, com a origem do front
  (`http://localhost:5173` e a origem do deploy) em *Authorized JavaScript origins*.
  O **ID token** gerado tem `audience` = esse client ID, que a API valida.

## Autenticação (obrigatória)

1. Tela de login com o botão **“Entrar com Google”** (`GoogleLogin` do
   `@react-oauth/google`).
2. Ao receber o `credential` (ID token), guarde-o e envie em **todas** as chamadas
   protegidas:
   ```http
   Authorization: Bearer <id_token>
   ```
3. Após o login, chame `GET /auth/me` para obter `{ id, name, email, role }` e use esse
   `id` nas rotas de favoritos.
4. Exiba no cabeçalho: nome, email e um badge com o `role` (`USER`/`ADMIN`).
5. Botão **Sair**: limpe o token/estado e volte para o login. Opcionalmente chame
   `google.accounts.id.disableAutoSelect()`.
6. Token expira (~1h). Se qualquer resposta for **401**, limpe o token e volte ao login
   com aviso “Sessão expirada”.
7. Guarde o token em memória e, no máximo, em `sessionStorage` (evite `localStorage`).
   Documente a escolha no README.
8. **Fallback de dev:** o profile de dev também aceita HTTP Basic `admin`/`admin`, porém
   `/auth/me`, favoritos e grafo dependem de JWT (provisionamento JIT). Portanto, use
   login Google para o fluxo completo; Basic serve apenas para testar `/users/**`.

## Contrato da API

Base: `VITE_API_BASE_URL`. Todas as rotas protegidas exigem `Authorization: Bearer`.
Enums:
- `Role`: `USER | ADMIN`

### Auth

| Método | Rota | Auth | Resposta |
| --- | --- | --- | --- |
| GET | `/auth/me` | JWT | 200 `UserResponse`; 400 token sem email verificado; 401 ausente/inválido |

```json
{ "id": 1, "name": "Jairo", "email": "jairo@exemplo.com", "role": "USER" }
```

### Usuários (somente `ADMIN`)

| Método | Rota | Corpo | Resposta |
| --- | --- | --- | --- |
| POST | `/users` | `UserCreateRequest` | 201 `UserResponse` + `Location` |
| GET | `/users` | — | 200 `UserResponse[]` |
| GET | `/users/{id}` | — | 200 `UserResponse`; 404 |
| PUT | `/users/{id}` | `UserUpdateRequest` | 200 `UserResponse`; 404; 409 email duplicado |
| DELETE | `/users/{id}` | — | 204; 404 |

```json
// UserCreateRequest
{ "name": "Fulano", "email": "fulano@exemplo.com", "password": "segredo123" }
// UserUpdateRequest (password opcional, min 6)
{ "name": "Fulano", "email": "fulano@exemplo.com", "password": "" }
```

Validação: `name` obrigatório (max 120), `email` válido (max 160), `password` min 6.
Não-admin recebe **403** em qualquer rota `/users/**`.

### Favoritos (dono ou `ADMIN`)

| Método | Rota | Corpo | Resposta |
| --- | --- | --- | --- |
| POST | `/users/{userId}/favorites` | `FavoriteRequest` | 201 `FavoriteResponse`; 400; 403; 404; 409 duplicado |
| GET | `/users/{userId}/favorites` | — | 200 `FavoriteResponse[]`; 403; 404 |
| DELETE | `/users/{userId}/favorites/{favoriteId}` | — | 204; 403; 404 |

```json
// FavoriteRequest
{ "symbol": "PETR4", "name": "Petrobras" }
// FavoriteResponse
{ "id": 10, "symbol": "PETR4", "name": "Petrobras" }
```

Regras: `symbol` é normalizado para maiúsculas (`trim().toUpperCase()`);
`name` é opcional (max 120); duplicidade `(userId, symbol)` → **409**.
Um usuário só acessa os próprios favoritos; `ADMIN` acessa os de qualquer usuário.

### Carteira (dono ou `ADMIN`)

Uma carteira por usuário (criada automaticamente vazia no primeiro acesso, nome
padrão "Minha carteira").

| Método | Rota | Corpo | Resposta |
| --- | --- | --- | --- |
| GET | `/users/{userId}/portfolio` | — | 200 `PortfolioResponse`; 403; 404 |
| PUT | `/users/{userId}/portfolio` | `{ "name": "..." }` | 200 `PortfolioResponse`; 400; 403; 404 |
| GET | `/users/{userId}/portfolio/items` | — | 200 `PortfolioItemResponse[]`; 403; 404 |
| POST | `/users/{userId}/portfolio/items` | `PortfolioItemRequest` | 201 `PortfolioItemResponse`; 400; 403; 404 |
| PUT | `/users/{userId}/portfolio/items/{itemId}` | `PortfolioItemUpdateRequest` | 200 `PortfolioItemResponse`; 400; 403; 404 |
| DELETE | `/users/{userId}/portfolio/items/{itemId}` | — | 204; 403; 404 |

```json
// PortfolioItemRequest
{ "symbol": "PETR4", "name": "Petrobras", "quantity": 10 }
// PortfolioItemUpdateRequest (quantity absoluta)
{ "quantity": 3, "name": "Petrobras PN" }
// PortfolioItemResponse
{ "id": 7, "symbol": "PETR4", "name": "Petrobras", "quantity": 10 }
// PortfolioResponse
{
  "id": 1,
  "name": "Minha carteira",
  "items": [
    { "id": 7, "symbol": "PETR4", "name": "Petrobras", "quantity": 10 }
  ]
}
```

Regras: `symbol` é normalizado para maiúsculas; `quantity` é `BigDecimal` positivo
(aceita fração, ex.: `0.5`); `name` opcional (max 120). `POST /items` **soma** a
quantidade quando o mesmo `symbol` já existe (upsert); `PUT` define a quantidade
absoluta. O backend **não calcula valor** nem busca cotação: retorne apenas
nome/símbolo e quantidade.

### Grafo (qualquer autenticado)

| Método | Rota | Resposta |
| --- | --- | --- |
| GET | `/graph` | 200 `{ nodes, edges }` |
| GET | `/graph/assets/{symbol}/neighbors` | 200 `NeighborResponse[]`; 404 ativo inexistente |
| GET | `/graph/path?from=SYMBOL&to=SYMBOL` | 200 `PathResponse`; 400 símbolo vazio; 404 ativo inexistente |
| GET | `/graph/components` | 200 `string[][]` (ids dos nós) |

```json
// GET /graph
{
  "nodes": [
    { "id": "PETR4", "symbol": "PETR4", "name": "Petrobras" }
  ],
  "edges": [
    { "source": "PETR4", "target": "HGLG11", "weight": 2 }
  ]
}
// neighbors
[ { "id": "HGLG11", "symbol": "HGLG11", "name": "CSHG Logística", "weight": 2 } ]
// path
{ "found": true, "path": ["PETR4", "HGLG11"], "length": 1 }
// components
[ ["HGLG11", "PETR4"], ["BTC"] ]
```

Regras do grafo:
- O id do nó é o próprio `symbol` (ex.: `PETR4`).
- Aresta é **não direcionada**; `weight` = nº de usuários distintos que favoritaram o par.
- `name` do nó pode ser `null`.
- `/neighbors` e `from`/`to` usam o `symbol` (normalizado para maiúsculas).
- Sem caminho → **200** com `{ "found": false, "path": [], "length": 0 }`.
- O grafo é global e **anônimo** (nunca mostra quem favoritou).
- Recarregue o grafo após adicionar/remover favoritos.

### Formato de erro (todas as rotas)

```json
{
  "timestamp": "2026-11-12T12:00:00",
  "status": 409,
  "error": "Conflict",
  "message": "Ativo já está nos favoritos do usuário: PETR4",
  "path": "/users/1/favorites",
  "errors": []
}
```

Em erros de validação (`400`), `errors` traz `[{ "field": "email", "message": "..." }]`.
Trate: `400`, `401`, `403`, `404`, `405`, `406`, `409`, `415`, `500`.
Em `401` → deslogar; em `403` → mensagem de permissão; em `409` → mensagem de duplicidade;
em `400` → exibir erros por campo; em `500` → mensagem genérica.

## Telas e funcionalidades

### 1. Login
- Página centralizada com logo/nome, botão Google e aviso de que é ambiente de testes.
- Mostra mensagens de erro de login (client ID, CORS, token inválido).
- Se já houver token válido, redireciona para a área principal.

### 2. Layout principal
- Cabeçalho: nome, email, badge de role, botão Sair, indicador da URL da API.
- Navegação: **Favoritos**, **Carteira**, **Grafo**, **Usuários** (só se `ADMIN`), **Diagnóstico**.
- Toasts para sucesso/erro; estados de carregamento e vazio.

### 3. Favoritos (tela principal)
- Usa o `id` de `/auth/me`.
- Formulário: `symbol` (texto) e `name` (opcional).
- Lista em tabela/cards com símbolo, nome e botão remover.
- Após criar/remover, atualiza a lista (e sinaliza para recarregar o grafo).
- Exibe `409` de duplicidade de forma clara; valida campos no cliente também.
- Estado vazio com call-to-action.
- Admin: permitir escolher um `userId` para inspecionar favoritos de outro usuário
  (usando `GET /users` para popular a lista). Usuário comum vê apenas os próprios.

### 4. Carteira (simulação)
- Usa o `id` de `/auth/me`; `GET /users/{userId}/portfolio` cria a carteira vazia
  no primeiro acesso.
- Mostra o nome da carteira com opção de renomear (`PUT /users/{userId}/portfolio`).
- Formulário para adicionar cota: `symbol`, `name` e `quantity`
  (aceita fração). Lista em tabela com símbolo, nome e quantidade.
- Editar quantidade/nome (`PUT /items/{itemId}`) e remover (`DELETE /items/{itemId}`).
- Deixar claro que reenviar o mesmo `symbol` **soma** a quantidade.
- Não exibir preço/valor calculado: o backend não fornece cotação.
- Estado vazio com call-to-action; atualizar a lista após cada operação.
- Admin: permitir escolher um `userId` para inspecionar a carteira de outro usuário.

### 5. Grafo
- Carrega `GET /graph` e renderiza nós/arestas.
- Destaque visual dos nós; rótulo da aresta com o `weight`; espessura
  proporcional ao peso.
- Clique no nó → painel lateral com detalhes e vizinhos
  (`GET /graph/assets/{symbol}/neighbors`), ordenados por peso.
- Busca de menor caminho: dois campos `from`/`to` com o símbolo do ativo
  (ou selects com os nós existentes) → `GET /graph/path`; destacar nós/arestas do
  caminho; se `found=false`, avisar que não há caminho.
- Componentes conexos: `GET /graph/components`; exibir lista e/ou colorir grupos.
- Botão “Atualizar grafo”; estado vazio quando não há favoritos.
- Tratar `400` (símbolo vazio) e `404` (ativo não está no grafo).

### 6. Usuários (somente `ADMIN`)
- Tabela com `GET /users`; criar (`POST`), editar (`PUT`) e excluir (`DELETE`).
- Formulário de criação/edição com validação; senha opcional na edição.
- Confirmar exclusão; tratar `409` (email duplicado) e `404`.
- Se `role !== ADMIN`, esconder a rota e exibir 403 caso acessada por URL direta.

### 7. Diagnóstico (testar todas as rotas)
- Página que lista **todos** os endpoints com método, rota, campos de entrada e botão
  “Executar”.
- Mostra `status`, headers relevantes e o JSON de resposta (formatado) ou o erro.
- Inclui as rotas administrativas, de favoritos, da carteira e do grafo.
- Serve como checklist manual de testes da API.

## Estrutura sugerida

```
src/
├── main.tsx
├── App.tsx
├── config.ts                 // base URL, client ID, tipos
├── types.ts                  // DTOs e enums da API
├── auth/AuthContext.tsx      // token, usuário, login/logout, 401
├── api/client.ts             // fetch com Bearer, parsing de ErrorResponse
├── api/endpoints.ts          // funções para cada rota
├── components/               // Layout, Header, Toast, ConfirmDialog, etc.
└── pages/
    ├── LoginPage.tsx
    ├── FavoritesPage.tsx
    ├── PortfolioPage.tsx
    ├── GraphPage.tsx
    ├── UsersPage.tsx
    └── DiagnosticsPage.tsx
```

## Tratamento de erros e UX
- Cliente HTTP central que anexa o Bearer e converte erros no `ErrorResponse`.
- `401` → logout + redireciona ao login.
- `403` → mensagem “sem permissão” (destaque para CORS quando não houver JSON).
- `404` → recurso não encontrado; `409` → conflito/duplicidade.
- Loading (spinner/skeleton), empty states e retry.
- Responsivo (desktop e mobile) e acessível (labels, foco, contraste).

## Checklist de aceite
- [ ] `npm install && npm run dev` sobe a SPA sem erros de TypeScript.
- [ ] Login Google obtém ID token e `GET /auth/me` retorna o usuário.
- [ ] Token é enviado em todas as chamadas protegidas; logout limpa a sessão.
- [ ] Favoritos: criar, listar e remover funcionam; duplicidade mostra `409`.
- [ ] Carteira: adicionar, editar e remover cotas; reenviar a mesma cota soma a quantidade.
- [ ] Usuário comum não vê a área de usuários; `ADMIN` faz CRUD completo.
- [ ] Grafo renderiza nós/arestas com peso nas arestas.
- [ ] Vizinhos, menor caminho (com destaque) e componentes funcionam.
- [ ] Página de diagnóstico executa todas as rotas e mostra status + JSON.
- [ ] Erros 400/401/403/404/409/500 são exibidos de forma amigável.
- [ ] `VITE_API_BASE_URL` alterna entre local e deploy sem mudar código.
- [ ] README explica configuração do Google, CORS e execução.

## Entregáveis
1. Código-fonte da SPA completo.
2. `.env.example` com as variáveis (sem segredos).
3. `README.md` com pré-requisitos, configuração do Google Cloud, CORS, execução e
   limitações do fallback Basic.
4. Nenhuma alteração no backend.

## Observações finais
- Não invente campos que a API não retorna.
- Não use `localStorage` para o token em produção; em app de teste, `sessionStorage` é
  aceitável e deve ser documentado.
- Se o grafo estiver vazio, oriente o usuário a cadastrar favoritos primeiro.
- O backend não fornece cotações; qualquer cotação é opcional e feita direto na brapi
  pelo navegador.
