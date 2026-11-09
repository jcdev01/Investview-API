# Tarefa: Implementar grafo global de ativos por coocorrência de favoritos

> Prompt/especificação para implementar o "conceito de grafos" no Investview-API
> (Ideia 1: grafo de ativos por coocorrência de favoritos).

## Contexto do projeto

- API REST em Java 25 + Spring Boot 4.1.2 (Maven), pacote base `jairo.investviewapi`.
- Arquitetura em camadas: entity, repository, service, controller, dto, exception, config, security.
- Entidades existentes: `User` (`tb_users`) e `Favorite` (`tb_favorites`), onde `Favorite` tem
  `user` (ManyToOne), `symbol`, `type` (enum `AssetType`: STOCK, FII, ETF, CRYPTO) e `name` (opcional).
- Existe unique constraint `(user_id, type, symbol)` em `Favorite`.
- Segurança: OAuth2 Resource Server (JWT do Google), RBAC (`ROLE_USER`/`ROLE_ADMIN`) e
  `CurrentUserProvider`. Rotas fora de `/users/**` exigem apenas autenticação.
- Convenções: injeção por construtor (`@RequiredArgsConstructor`), DTOs como `record`,
  Lombok, SEM comentários no código, mensagens em português, Swagger com `@Tag`/`@Operation`/
  `@ApiResponse`. NÃO adicionar dependências novas.

## Objetivo

Implementar o "conceito de grafos": um grafo GLOBAL de ativos derivado dos favoritos de
todos os usuários, que o front-end possa desenhar e que permita consultas simples.

## Modelo do grafo

- Nó = ativo, id no formato `"<TYPE>:<SYMBOL>"` (ex.: `STOCK:PETR4`), contendo `symbol`,
  `type` e `name` (pode ser null).
- Aresta (não direcionada) = dois ativos favoritados pelo MESMO usuário.
- Peso = número de usuários distintos que favoritaram aquele par.
- Como cada usuário tem no máximo um favorito por `(type, symbol)`, os pares de um usuário
  são únicos; normalize a ordem dos dois ids para não duplicar a aresta.

## Como montar (GraphService)

1. Buscar todos os favoritos junto com o id do usuário, evitando N+1 (use `@Query` com
   projeção `(userId, type, symbol, name)` ou `join fetch` em `FavoriteRepository`).
2. Agrupar os favoritos por usuário.
3. Para cada usuário, gerar todas as combinações de pares dos seus ativos.
4. Incrementar +1 no peso de cada aresta (mapa `Map<PairKey, Integer>`).
5. Montar adjacência `Map<String, Set<String>>` e o mapa de pesos.
6. `name` do nó: primeiro nome não nulo encontrado para aquele ativo.

## Endpoints (todos autenticados; expõem apenas dados agregados, nunca quem favoritou)

1. `GET /graph` — grafo completo.
   Resposta:
   ```json
   {
     "nodes": [
       { "id": "STOCK:PETR4", "symbol": "PETR4", "type": "STOCK", "name": "Petrobras" },
       { "id": "STOCK:VALE3", "symbol": "VALE3", "type": "STOCK", "name": "Vale" },
       { "id": "CRYPTO:BTC", "symbol": "BTC", "type": "CRYPTO", "name": "Bitcoin" }
     ],
     "edges": [
       { "source": "STOCK:PETR4", "target": "STOCK:VALE3", "weight": 2 },
       { "source": "STOCK:VALE3", "target": "CRYPTO:BTC", "weight": 1 }
     ]
   }
   ```
2. `GET /graph/assets/{type}/{symbol}/neighbors` — vizinhos ordenados por peso desc.
   Resposta:
   ```json
   [
     { "id": "STOCK:VALE3", "symbol": "VALE3", "type": "STOCK", "name": "Vale", "weight": 2 }
   ]
   ```
3. `GET /graph/path?from=STOCK:PETR4&to=CRYPTO:BTC` — menor caminho via BFS.
   Resposta:
   ```json
   { "found": true, "path": ["STOCK:PETR4", "STOCK:VALE3", "CRYPTO:BTC"], "length": 2 }
   ```
   Quando não houver caminho: `{ "found": false, "path": [], "length": 0 }`.
4. (opcional) `GET /graph/components` — clusters (componentes conexos), retornando listas de ids.

## Algoritmos (Java puro, sem libs)

- BFS com `Queue` e `Map<String, String>` de predecessores para reconstruir o caminho.
- Componentes conexos com DFS (ou union-find).
- Grau = quantidade de vizinhos.

## Estrutura de pacotes sugerida

```
graph/Graph.java                -> interface (addNode, addEdge, neighbors, bfsPath, components)
graph/AdjacencyListGraph.java   -> implementação com Map
graph/GraphService.java         -> monta o grafo a partir do FavoriteRepository
graph/GraphController.java      -> endpoints /graph
dto/GraphResponse.java          -> { nodes, edges }
dto/GraphNode.java              -> { id, symbol, type, name }
dto/GraphEdge.java              -> { source, target, weight }
dto/NeighborResponse.java       -> { id, symbol, type, name, weight }
dto/PathResponse.java           -> { found, path, length }
```

## Segurança

- `/graph/**` exige autenticação (já coberto por `anyRequest().authenticated()`); não exige role.
- NÃO expor identidade de usuários; retornar apenas agregados (nós, arestas e pesos).

## Tratamento de erros

- `type` inválido ou `from`/`to` em formato errado: 400 usando `ErrorResponse`.
- Ativo inexistente no grafo: retornar 404 (`ResourceNotFoundException`) ou caminho vazio;
  escolha uma abordagem e documente.
- Use o `GlobalExceptionHandler` existente; não crie handlers duplicados.

## Testes (JUnit 5 + Spring Boot Test)

- Montar usuários/favoritos no H2 e validar:
  - nós e arestas corretos e pesos somados por usuários distintos;
  - `neighbors` ordenados por peso desc;
  - `path` via BFS correto e `found=false` quando não há caminho;
  - ativo isolado (sem arestas) e grafo vazio.
- Rodar `./mvnw test` e garantir a suíte completa passando.

## Critérios de aceite

- Sem alteração de schema e sem novas dependências.
- `GET /graph` retorna nós/arestas/pesos corretos.
- `neighbors` ordenados por peso; `path` via BFS correto.
- Endpoints documentados no Swagger.
- Todos os testes passando.

## Observações

- O cálculo é em memória a cada request; cache no Redis (já presente no projeto) é evolução futura.
- Não chamar APIs externas nem armazenar cotações; o grafo deriva apenas dos favoritos.
