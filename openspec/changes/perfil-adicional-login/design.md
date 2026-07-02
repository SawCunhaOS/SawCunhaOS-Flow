## Context

`SCOS_LOGIN_PROFILE` é uma tabela N:N (PK composta `loginId`+`profileId`) introduzida no schema v2, sem contrato público hoje. O único endpoint de perfil existente, `PUT /logins/{id}/profile/{profileId}`, cobre um conceito diferente: o perfil **principal** (1:1, `Login.profileId`). Este design cobre só o contrato OpenAPI dos 3 endpoints novos — implementação de código fica para change futura separada.

## Goals / Non-Goals

**Goals:**
- Expor list/add/remove dos perfis adicionais de um login via `/v1/logins/{id}/profiles*` (plural, path distinto do singular já existente)
- Deixar explícito no contrato que perfil adicional não pode duplicar o perfil principal

**Non-Goals:**
- Alterar `PUT /logins/{id}/profile/{profileId}` (perfil principal) — fora de escopo, endpoint já existe e não muda
- Migração de dados — sistema sem produção
- Implementação de código (permissões, use case, domain, delegates, testes) — fora de escopo desta change; entra em change futura separada

## Decisions

- **Idempotência do `DELETE /v1/logins/{id}/profiles/{profileId}` quando a associação não existe: retorna `204` silencioso, não `404`.** Motivo: convenção REST mais comum para DELETE — o estado final desejado ("essa associação não existe") já é verdade, então repetir a chamada não deveria falhar. Verifiquei o padrão já usado no arquivo (`deleteLogin`, `deleteProfile` em `ScosOrganization_Login.yml`): ambos só documentam `204`/`4XX`/`5XX` sem branch explícito de "não encontrado", ou seja, o arquivo não tem um precedente forte que contrarie a idempotência silenciosa. Alternativa descartada: `404` explícito — mais estrito, mas quebra idempotência e não tem precedente no arquivo que justifique o desvio.
- **Rotas plural (`/profiles`) distinto do singular (`/profile/{profileId}`)** — já decidido na idea, mantido: reaproveita padrão REST direto, deixa inequívoco que é N:N.
- **Validação de perfil adicional == perfil principal fica no use case, não só na trigger `TRG_VALIDATE_LOGIN_PROFILE_NOT_PRIMARY`** — o contrato OpenAPI não consegue expressar essa regra (é uma comparação entre dois relacionamentos diferentes de FK), então ela é documentada como requisito de comportamento no spec, a ser implementada na camada de use case.
- **Sem schema de request em `POST`/`DELETE`** — a chave inteira (`id`, `profileId`) já vem nos path params; um body vazio só adicionaria ruído ao contrato.

## Risks / Trade-offs

- [Risco] Cliente confundir `/profile/{profileId}` (singular, substitui o principal) com `/profiles/{profileId}` (plural, adiciona um adicional) → Mitigação: nomenclatura já deliberadamente distinta (singular vs. plural) + `description` de cada `operationId` deve deixar isso explícito
- [Risco] `POST` sem validação client-side deixa vazar a exceção genérica da trigger do banco quando `profileId == Login.profileId` → Mitigação: spec exige validação explícita no use case antes de persistir, retornando `4XX` via `ScosException`
- [Trade-off] `204` idempotente no `DELETE` esconde do cliente se a remoção teve efeito real ou não → aceito, é o padrão REST mais comum e evita acoplamento do cliente ao estado prévio da associação
