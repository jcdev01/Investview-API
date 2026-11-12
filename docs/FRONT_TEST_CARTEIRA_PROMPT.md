# Prompt — adicionar a Carteira ao front de testes

> Cole este bloco na IA que construiu a SPA de testes da Investview-API.
> O backend já está pronto; **não altere a API**.

## Objetivo

Adicionar ao front existente uma página **Carteira** para simular a carteira de
investimentos do usuário: listar, adicionar, editar e remover cotas (ativo +
quantidade). Não há cálculo de preço/valor: exiba apenas símbolo, nome e
quantidade.

## API (Bearer token do Google)

Base: `/users/{userId}/portfolio` (use o `id` de `GET /auth/me`).

| Método | Rota | Corpo | Resposta |
| --- | --- | --- | --- |
| GET | `/users/{userId}/portfolio` | — | 200 `{ id, name, items[] }` (cria vazia no 1º acesso) |
| PUT | `/users/{userId}/portfolio` | `{ "name": "..." }` | 200 carteira |
| GET | `/users/{userId}/portfolio/items` | — | 200 `Cota[]` |
| POST | `/users/{userId}/portfolio/items` | `CotaRequest` | 201 cota + `Location` |
| PUT | `/users/{userId}/portfolio/items/{itemId}` | `{ "quantity": 5, "name": "..." }` | 200 cota |
| DELETE | `/users/{userId}/portfolio/items/{itemId}` | — | 204 |

```json
// CotaRequest
{ "symbol": "PETR4", "name": "Petrobras", "quantity": 10 }
// CotaResponse
{ "id": 7, "symbol": "PETR4", "name": "Petrobras", "quantity": 10 }
// CarteiraResponse
{ "id": 1, "name": "Minha carteira", "items": [ /* CotaResponse[] */ ] }
```

Regras: `quantity` positivo e decimal (ex.: `0.5`); `symbol` é normalizado para
maiúsculas no backend; `POST` no mesmo `symbol` **soma** a quantidade; `PUT` define
a quantidade absoluta. Erros
usam o `ErrorResponse` padrão (`timestamp, status, error, message, path, errors[]`).

## UI

- Nova rota/página **Carteira** na navegação (visível para todos os autenticados).
- Cabeçalho com o nome da carteira + botão renomear (`PUT`).
- Formulário: `symbol`, `name` (opcional) e `quantity`
  (number, `step="0.00000001"`).
- Tabela/cards com símbolo, nome, quantidade e ações editar/remover.
- Aviso de que reenviar a mesma cota soma a quantidade.
- Estado vazio com CTA; loading e toasts de sucesso/erro.
- Admin: seletor de `userId` para inspecionar a carteira de outro usuário
  (usar `GET /users`), igual à tela de favoritos.
- Tratar `400` (validação), `401` (logout), `403` (sem permissão) e `404`.

## Arquivos sugeridos

- `types.ts`: `Portfolio`, `PortfolioItem`, `PortfolioItemRequest`.
- `api/endpoints.ts`: funções `getPortfolio`, `renamePortfolio`, `listItems`,
  `addItem`, `updateItem`, `removeItem`.
- `pages/PortfolioPage.tsx` + item de navegação no layout.

## Aceite

- [ ] Página lista a carteira e cria vazia no primeiro acesso.
- [ ] Adicionar, editar e remover cotas atualiza a lista.
- [ ] Adicionar cota repetida soma a quantidade.
- [ ] Renomear a carteira persiste após recarregar.
- [ ] Sem cálculo de preço; apenas quantidade/nome/símbolo.
