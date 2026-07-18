---
title: 'Reconciliação de Inputs — PRD vs. Documentos de Usecase'
status: draft
created: '2026-07-18'
related-prd: prd.md
related-inputs: etc/doc/usecase/00-indice-central.md a 07-mensagens-erro-pt-en.md
---

# Reconciliação de Inputs — PRD SawCunhaOS Fase 1 vs. `etc/doc/usecase/*`

## 1. Objetivo e Escopo

Este documento é o extract de **Input Reconciliation** do passo de finalize do PRD
(`prd-SawCunhaOS-Organization-2026-07-18/prd.md`). Verifica se o PRD final:

1. Se sustenta na promessa declarada em `§0` ("não duplica a especificação funcional
   detalhada... é referenciada, não repetida") sem contradizer nenhuma regra de negócio
   já documentada nos 8 arquivos de usecase.
2. Não deixa nenhum "Achado Crítico" da Seção 3 do documento `00-indice-central.md`
   sem tratamento ou menção.
3. Sinaliza explicitamente qualquer nova capacidade que reverta ou reinterprete uma
   decisão de design já registrada nos docs de usecase (em vez de mudá-la silenciosamente).

**Método:** leitura integral dos 8 documentos de usecase (com foco em regras de negócio,
efeitos colaterais e na Seção 3 "Achados Críticos" do documento 00), leitura integral do
`prd.md` (561 linhas), e verificação pontual contra o código-fonte atual
(`ScosOrganizationPermission.java`, `ScosOrganization_Employee.yml`) para confirmar se
achados citados como "não implementados" ainda são válidos nesta data.

## 2. Veredito Geral

A promessa de "referenciar, não duplicar" **se sustenta majoritariamente**: os 5 dos 8
achados críticos que exigiam uma decisão de produto (3.1 Company, 3.2 bug de perfil
adicional, 3.3 defasagem de 30 min, 3.7 proteção de DELETE físico, 3.8 fechamento de
posição) foram **corretamente absorvidos** no PRD, com referência cruzada explícita ao
documento 00 em quase todos os casos (via `[NOTE FOR PM: ...]`).

Porém, a auditoria encontrou **4 gaps/conflitos** que precisam de decisão ou correção
antes de Arquitetura/Épicos consumirem este PRD como insumo. O mais relevante (Gap 1) é
uma reversão de design não sinalizada como tal.

## 3. Rastreamento Achado Crítico → PRD (Seção 3 do doc 00)

| # | Achado (doc 00) | Tratado no PRD? | Onde |
| - | --- | --- | --- |
| 3.1 | Company: ciclo (004), última matriz (005), última empresa (006) não implementados | ✅ Sim, com referência cruzada explícita | FR-1, FR-2 (`NOTE FOR PM` citando os 3 códigos) |
| 3.2 | Bug: perfis adicionais não somam permissão em `vw_login_context` | ✅ Sim, com referência cruzada explícita | FR-7 (`NOTE FOR PM`) |
| 3.3 | Defasagem de até 30 min nas views de autoridade (`pg_cron`) | ✅ Sim, tratado extensivamente (mais que o mínimo) | FR-7, FR-8, §4.1 NFR, §5 NFRs Transversais, §9 Riscos |
| 3.4 | Resolução de Empresa/Filial em `GET /v1/logins/me` (COALESCE matriz/filial) | ⚪ Não mencionado | — |
| 3.5 | Defeito de contrato: `/v1/employees/rehire` com `GET`/`PUT` acidentais | ❌ **Não mencionado**, apesar de FR-5 alterar a regra de negócio do mesmo endpoint | Ver Gap 4 |
| 3.6 | Enum `ScosOrganizationPermission` desatualizado (nomenclatura antiga) | ⚪ Parcialmente stale — já corrigido no código nesta branch (confirmado via grep: `BLOCK_COMPANY`, `UNBLOCK_COMPANY`, `BLOCK_EMPLOYEE`, `REHIRE_EMPLOYEE`, `CREATE_LOGIN_PROFILE` etc. já existem no enum). Resta apenas a lacuna `BLOCK_LOGIN`/`UNBLOCK_LOGIN`, que o PRD **já** cobre. | FR-6 (`NOTE FOR PM`) |
| 3.7 | `fn_block_delete` — proteção genérica contra `DELETE` físico | ✅ Sim, usado como base do requisito para novas tabelas de histórico | §10 Trilha de Auditoria |
| 3.8 | `fn_close_previous_position` fecha todas as posições em aberto | ✅ Sim, restated consistentemente | FR-5, FR-14 |

Achado 3.4 não é um bug nem uma regra pendente — é documentação de comportamento já
correto. Sua ausência no PRD é aceitável dado o princípio de "referenciar, não repetir".
Achado 3.5 é o único achado crítico realmente órfão (ver Gap 4).

## 4. Gaps e Conflitos Identificados

### Gap 1 (Alto impacto) — Login `PENDING_APPROVAL`/`REJECTED` reverte, sem sinalizar, uma decisão de design já registrada

O documento 00 (§1) afirma, em tom de decisão definitiva já tomada:

> "**Login não tem mais `PENDING`.** A criação é fire-and-forget: nasce `ACTIVE`
> imediatamente; a Saga Keycloak sincroniza em background via Outbox."

O documento 04 (`POST /v1/logins`, `POST /v1/employees/{employeeId}/logins`) confirma
no nível de efeito colateral: **"Status → `ACTIVE` imediatamente (fire-and-forget)"**.
O modelo de status de Login documentado é `ACTIVE ⇄ INACTIVE ⇄ BLOCKED`, sem estado
`PENDING` algum (tabela da Seção 1 do doc 00).

O PRD (Glossário, FR-6, FR-23 a FR-27, UJ-6/8/10/11/12/13/17) **reintroduz** exatamente
o estado que a documentação registra como removido — `PENDING_APPROVAL → ACTIVE ⇄
INACTIVE ⇄ BLOCKED`, mais `REJECTED` como estado terminal alternativo — e o generaliza
também para reativação (FR-24), troca de perfil (FR-25) e Login sem Funcionário
(FR-26).

Isso **não é necessariamente um erro** — ler o PRD inteiro sugere que é uma decisão de
produto consciente (governança de acesso, four-eyes, SLA de aprovação são detalhados
com grande cuidado). O problema é que **em nenhum ponto do PRD isso é reconciliado
explicitamente contra o doc 00** — não há nenhum `NOTE FOR PM`/`ASSUMPTION` do tipo
"isto reverte a decisão registrada em `usecase/00` de que Login nasce `ACTIVE`
imediatamente; a partir desta rodada, Login sem Funcionário/Perfil aprovado nasce
`PENDING_APPROVAL`". Sem essa reconciliação expressa:
- Arquitetura pode tratar como bug do PRD em vez de mudança de escopo deliberada.
- O contrato OpenAPI de Login precisa de mudança breaking (novo valor de enum de
  status, novos endpoints de aprovação/rejeição, novas permissões `APPROVE_LOGIN`/
  `APPROVE_SYSTEM_ACCESS`) que hoje **não existe** em `ScosOrganization_Login.yml` nem
  no enum de permissões — e o PRD não trata isso como mudança de contrato, apesar da
  própria convenção do projeto exigir "contrato antes do código".

**Recomendação:** adicionar um `[NOTE FOR PM]` explícito no PRD (FR-23 ou no Glossário)
reconciliando a introdução de `PENDING_APPROVAL`/`REJECTED` contra a decisão registrada
no doc 00 §1, e marcar como mudança breaking de contrato a ser refletida no YAML antes
da Arquitetura.

### Gap 2 (Médio impacto) — Estado "pendente" de Perfil (FR-27) não reconciliado com o modelo documentado

O modelo de status de Perfil documentado (doc 00 §1) é o mais simples do sistema:
`ACTIVE=true ⇄ ACTIVE=false`, "sem catálogo de motivo". O doc 04 (`POST /v1/profiles`)
confirma: criação retorna `201` direto, "perfil sem recursos, não concede acesso" — sem
qualquer estado intermediário de aprovação.

FR-27 introduz um estado "pendente" adicional para Perfil (criação e edição de
recursos), da mesma família do Gap 1, e pelo mesmo motivo: é uma extensão razoável do
modelo, mas não é reconciliada explicitamente contra o `ACTIVE=true/false` já
documentado — nem contra o efeito colateral hoje descrito para `PUT /v1/profiles/{id}
/resources` (UC-075: "Substituição total em transação única", ou seja, aplicação
imediata e incondicional, sem gate). FR-27 muda esse efeito colateral para condicional
("Logins que já usam esse Perfil continuam com o conjunto... anterior... até a mudança
ser aprovada"), o que é uma mudança de comportamento do mesmo endpoint já especificado
em UC-075 — vale o mesmo tratamento de reconciliação explícita recomendado no Gap 1.

### Gap 3 (Médio impacto) — FR-28 aponta para o catálogo de Reason errado

FR-28 diz que os dois novos Motivos ("Férias", "Licença Médica") vão para os
**"catálogos de Reason (Disable/Enable)"**, associados à transição de **`disable`**
(`ACTIVE → INACTIVE`).

Isso não bate com o mapeamento ação → catálogo já documentado e cruzado contra o
código em todos os 3 documentos relevantes (00 §1; 01, PUT `/enable`/`/disable`/`/block`
/`/unblock`; 03, mesmas 4 transições de Employee):

| Ação (endpoint) | Transição | Catálogo usado (documentado) |
| --- | --- | --- |
| `enable` | `INACTIVE → ACTIVE` | `SCOS_REASON_ACTIVATE` |
| `disable` | `ACTIVE → INACTIVE` | `SCOS_REASON_INACTIVATE` |
| `block` | qualquer → `DISABLED` | `SCOS_REASON_DISABLE` |
| `unblock` | `DISABLED → ACTIVE` | `SCOS_REASON_ENABLE` |

FR-28 descreve exatamente a ação `disable` (Employee/Login saindo de `ACTIVE` para
`INACTIVE`, reversível, sem Kill Switch permanente) — o catálogo correto para essa
transição é `SCOS_REASON_INACTIVATE`, não `SCOS_REASON_DISABLE` (que é o catálogo da
ação `block`, usado para o desligamento/bloqueio mais severo → `DISABLED`). Se o time
de implementação seguir o texto literal de FR-28 e cadastrar "Férias"/"Licença Médica"
em `SCOS_REASON_DISABLE`/`SCOS_REASON_ENABLE`, a validação de `entityType`/`reasonId`
do endpoint `/disable` real (que exige FK em `SCOS_REASON_INACTIVATE`, doc 03 UC-040)
vai rejeitar esses motivos com `422` — o recurso simplesmente não funciona como
descrito na UJ-17.

**Recomendação:** corrigir o texto de FR-28 para "catálogos de Reason (Inactivate/
Activate)" — os dois motivos precisam existir em `SCOS_REASON_INACTIVATE` (para o
`disable`) e, se houver reativação por motivo específico, em `SCOS_REASON_ACTIVATE`
(para o `enable` de retorno) — nunca em `SCOS_REASON_DISABLE`/`SCOS_REASON_ENABLE`.

### Gap 4 (Baixo/Médio impacto) — Achado 3.5 (defeito de contrato no `/rehire`) órfão no PRD

Confirmado nesta data que o defeito ainda existe no YAML real
(`etc/api/organization/ScosOrganization_Employee.yml`, linha 114): o path
`/v1/employees/rehire` ainda declara um método `GET` com `summary: Get employee by id`
— cópia acidental do endpoint `/v1/employees/{id}`, exatamente como o doc 00 (§3.5)
e o doc 03 reportam.

O PRD (FR-5) **modifica diretamente a regra de negócio desse mesmo endpoint**
(estender `rehire` para aceitar origem `DISABLED`, não só `INACTIVE`) mas não menciona
em nenhum lugar que o contrato YAML do mesmo path tem entradas espúrias a remover.
Como a convenção do projeto exige "atualizar o YAML primeiro" para qualquer mudança de
endpoint, isso é risco concreto de a limpeza do defeito de contrato ser esquecida
justamente no momento em que alguém já está mexendo no mesmo arquivo/path por causa da
FR-5.

**Recomendação:** adicionar uma linha em FR-5 (`Consequences`) apontando a limpeza do
YAML (`remover os métodos `GET`/`PUT` espúrios de `/v1/employees/rehire`") como parte
do mesmo trabalho de implementação.

## 5. Mudança Consciente Bem Sinalizada (contraste positivo)

Vale registrar um contraste: a extensão de `rehire` para aceitar Funcionário
`DISABLED` como origem (FR-5) é uma mudança real em relação ao que os docs de usecase
documentam hoje (doc 03, UC-155, UC-E2: "Funcionário localizado tem status `ACTIVE` ou
`DISABLED` → `422` 'funcionário não está inativo'" — ou seja, hoje **só** `INACTIVE` é
aceito). Mas, diferente dos Gaps 1/2, o PRD sinaliza isso corretamente como decisão
consciente: *"decisão desta rodada, corrigindo uma lacuna identificada"* (FR-5), com
justificativa de negócio (UJ-14) e contraste explícito com o caminho `unblock`
existente. Esse é o padrão que os Gaps 1, 2 e 3 deveriam seguir.

## 6. Itens Verificados Sem Conflito

- FR-2 (regra de "não pode desativar Empresa com filial ativa" sem código de erro
  reservado) — confirmado contra doc 01 (UC-E4 de `PUT /disable`, mensagem sem código
  dedicado, diferente de `SCOS_COMPANY_007`). Achado correto do PRD.
- FR-4 (validação cronológica `startTime < lunchStart < lunchEnd < endTime`) —
  idêntica à regra em ordem dos docs 02/03.
- FR-13/FR-14 (Kill Switch não restaura sessões, reativação não reverte Logins) —
  consistente com a nota já registrada no doc 00 §1 e docs 01/03.
- §10 (necessidade de trigger `BEFORE UPDATE/DELETE` física para novas tabelas de
  histórico) — consistente com e generaliza corretamente o Achado 3.7.

## 7. Resumo para Ação

| Gap | Ação recomendada | Bloqueia Arquitetura? |
| --- | --- | --- |
| 1 — Login `PENDING_APPROVAL` não reconciliado | Adicionar nota explícita reconciliando contra doc 00 §1; tratar como contrato breaking | Sim — afeta shape do contrato de Login |
| 2 — Perfil "pendente" (FR-27) não reconciliado | Mesma nota, aplicada ao modelo de Profile | Sim — mesma razão |
| 3 — FR-28 aponta catálogo de Reason errado | Corrigir texto de FR-28 para Inactivate/Activate | Não bloqueia, mas deve ser corrigido antes de Épicos |
| 4 — Achado 3.5 (`/rehire` YAML) órfão | Adicionar linha em FR-5 sobre limpeza do YAML | Não bloqueia, baixo custo de correção |
