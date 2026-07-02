## Context

`Company`/`Employee`/`Login` compartilham o mesmo padrão de domínio: `STATUS` é um cache sincronizado por trigger a partir de uma tabela `*_STATUS_HISTORY` imutável, e toda transição exige um motivo rastreável vindo de um catálogo de referência filtrado por `entityType`. O contrato público hoje só reflete parte disso (Company/Employee têm só 2 das 4 transições; Login tem as outras 2; nenhum tem `reasonId` nem histórico consultável). A camada de domínio JPA já foi sincronizada (`atualizacao-entidades-jpa-liquibase-v2`); esta change só toca o contrato OpenAPI (`etc/api/organization/*.yml`) — nenhum código Java é implementado aqui.

## Goals / Non-Goals

**Goals:**
- As mesmas 4 rotas de transição (`enable`/`disable`/`block`/`unblock`) nos 3 agregados, todas exigindo `reasonId` do catálogo certo e `observation` opcional
- Os 4 endpoints de criação (`createCompany`, `createEmployee`, `createLogin`, `createEmployeeLogin`) exigindo `reasonActivateId`
- Consulta paginada e somente-leitura ao histórico de status nos 3 agregados
- Nenhuma operação no contrato que viole o vocabulário fechado do banco ou a matriz de transições válidas

**Non-Goals:**
- Implementação de código (permissões, use case, domain, delegates, testes) — camada JPA já feita na rodada anterior; o restante fica fora de escopo desta change, entra em change futura separada
- Dados fiscais, histórico de cargo, jornada, catálogo de endereço/contato, outbox, perfil adicional de login — ideias/changes próprias, propostas em paralelo
- Migração de dados existentes — sistema sem produção

## Decisions

### Localização dos 4 catálogos `Reason*`
**Decisão**: arquivo dedicado `ScosOrganization_Reason.yml`, contendo os 4 schemas (`ReasonActivate`, `ReasonInactivate`, `ReasonDisable`, `ReasonEnable`) e seus paths (`/v1/reason-activate`, `/v1/reason-inactivate`, `/v1/reason-disable`, `/v1/reason-enable`), referenciado via `$ref` por `ScosOrganization_Company.yml`, `ScosOrganization_Employee.yml` e `ScosOrganization_Login.yml`.

**Alternativas descartadas**:
- Duplicar o schema em cada um dos 3 arquivos consumidores — rejeitado, gera 3 cópias divergentes do mesmo catálogo
- Colocar em `ScosComponents.yml` — rejeitado; `ScosComponents.yml` hoje contém só primitivas genéricas reaproveitadas em todo o contrato (paginação, respostas de erro, `Direction`), não conceitos de domínio específicos de um bounded context

**Motivo**: os 4 catálogos são consumidos por 3 arquivos diferentes (Company/Employee/Login) — mesmo padrão de "arquivo central evita duplicação" já adotado pela change irmã `catalogo-tipo-endereco-contato` para `AddressType`/`ContactType` (que também é compartilhado entre múltiplos consumidores). Um arquivo próprio, nomeado pelo conceito de domínio (`Reason`), é mais descobrível que enterrar 4 schemas de domínio dentro de um arquivo de primitivas genéricas.

### `entityType` do catálogo `Reason*` é um enum próprio, distinto do de `AddressType`/`ContactType`
**Decisão**: `ReasonActivate`/`ReasonInactivate`/`ReasonDisable`/`ReasonEnable.entityType` usa um enum de 3 valores (`COMPANY`/`EMPLOYEE`/`LOGIN`), confirmado contra o `CHECK` real do banco (`chk_reason_*_entity_type`, `checks.yml`). Este é um schema OpenAPI **distinto** do `entityType` de 2 valores (`COMPANY`/`EMPLOYEE`, sem `LOGIN`) usado por `AddressType`/`ContactType` na change irmã `catalogo-tipo-endereco-contato` — os dois não devem ser modelados como o mesmo enum reusado.

**Motivo**: os `CHECK`s do banco divergem entre as duas famílias de catálogo (`chk_address_type_entity_type`/`chk_contact_type_entity_type` só aceitam `COMPANY`/`EMPLOYEE`). Se o contrato reusasse um único enum de 3 valores para ambos, um cliente poderia enviar `entityType: LOGIN` num `AddressType` e passar na validação OpenAPI, falhando só depois — feio — no `CHECK` do Postgres, em vez de um `4XX` de validação de contrato.

### Rotas `disable`/`enable` mapeadas para `/block`/`/unblock`
**Decisão**: mantido o nome já usado pelo `Login` (`/block`, `/unblock`) para as transições de domínio `disable`/`enable` nos 3 agregados, mesmo o nome da rota não batendo 1:1 com o nome da transição.

**Alternativa descartada**: nomes próprios como `/deactivate-temporarily`, `/reactivate` — rejeitado por quebrar a consistência já estabelecida no `Login`.

**Motivo**: consistência entre os 3 agregados pesa mais que a correspondência exata rota↔nome-de-domínio; a divergência é documentada explicitamente na `description` de cada `operationId` para não confundir quem lê o contrato sem este design doc.

### `DELETE /companies/{id}`/`/employees/{id}` renomeados para `PUT .../block`
**Decisão**: renomear em vez de manter `DELETE` com body novo, ou duplicar rota.

**Motivo**: a implementação já era a transição `disable`→`DISABLED` (a própria `description` atual diz "Exclusão lógica. Registro permanece com status DISABLED") — só o verbo/rota estavam semanticamente errados. Manter `DELETE` faria o contrato mentir sobre o que a operação realmente faz; duplicar rota criaria duas portas para o mesmo destino.

## Risks / Trade-offs

- **[Risk]** Renomear `DELETE /companies/{id}`/`/employees/{id}` para `PUT .../block` é breaking change de verbo E rota — qualquer client que já chame `DELETE` quebra; `x-authorize` também migra (`DELETE_COMPANY`/`DELETE_EMPLOYEE` → `BLOCK_COMPANY`/`BLOCK_EMPLOYEE`), então permissões atribuídas a perfis existentes por código antigo deixam de valer. → **Mitigação**: aceito diretamente, sistema sem produção; a reatribuição de permissões fica registrada aqui como pendência para a change de implementação futura.
- **[Risk]** OpenAPI não valida FK — `reasonId` pertencer ao catálogo certo (`entityType` + tipo de transição) é regra de negócio, não de contrato; falha vira erro genérico de banco se não tratada explicitamente. → **Mitigação**: use case deve validar `reasonId` contra `entityType`+`active` antes de persistir, retornando `ScosException` amigável.
- **[Risk]** `/block` só é válido a partir de `ACTIVE` — `INACTIVE → DISABLED/BLOCKED` não existe na matriz de transições, mas a API não tem como impedir a chamada só pelo contrato. → **Mitigação**: use case precisa rejeitar explicitamente antes do banco (a trigger de histórico rejeitaria a combinação, mas com erro genérico de constraint, não um `4XX` amigável).
- **[Risk]** Não existe transição para o mesmo status (`ACTIVE→ACTIVE`, etc.) — chamar `/enable` num agregado já `ACTIVE` precisa ser rejeitado explicitamente pelo use case, mesma lógica do risco anterior.
- **[Risk]** `reasonActivateId` obrigatório nas 4 rotas de criação é fácil de esquecer na implementação, porque não parece intuitivamente uma "transição de status" — sem ele, o próprio `INSERT` de criação falha na trigger. → **Mitigação**: registrar isso como item explícito na change de implementação futura, não implícito dentro de "adicionar campos".

## Migration Plan

Não aplicável — sem migração de dados (sistema sem produção) e sem mudança de schema de banco. O rollout desta change é publicar o novo `ScosOrganization_Reason.yml` + os 3 arquivos consumidores atualizados de uma vez; comunicar a quebra de contrato (rotas `DELETE` removidas/renomeadas) antes de publicar. Implementação de código fica para change futura separada.

## Open Questions

Nenhuma — todas as decisões relevantes foram tomadas nesta rodada de design ou já estavam decididas na idea.
