## Context

As migrations Liquibase em `scos-organization-boot/src/main/resources/db/changelog/v1.0.0/tables/` foram criadas antes do domain model estar estabilizado. O domain model (`etc/database/domain_model.md`) evoluiu para incluir um modelo de permissões multi-sistema, integração Keycloak com retry, e constraints corretas — mas as migrations não acompanharam. O gap analysis completo está documentado em `etc/doc/ideia/20260606_adequacao-liquibase-domain-model.md`.

O sistema está em desenvolvimento, sem dados em produção. O banco é recriado a cada ciclo de desenvolvimento.

## Goals / Non-Goals

**Goals:**
- Schema físico gerado por `liquibase update` idêntico ao domain model
- Todas as constraints (nullability, UK, FK, tipos, tamanhos) corretas
- Remoção completa do modelo antigo de permissões (`SCOS_PERMISSION`)
- IDs `BIGINT` gerados via `GENERATED ALWAYS AS IDENTITY` (padrão SQL, sem sequences explícitas)
- Índices de performance criados junto com as tabelas

**Non-Goals:**
- Alteração de entidades JPA, código Java ou configuração de aplicação
- Migração de dados
- Alteração do domain model

## Decisions

### 1. Reescrita completa vs. migrations incrementais
**Escolha**: Reescrita completa dos arquivos v1.0.0.
**Motivo**: Sistema sem produção. Migrations incrementais (`ALTER TABLE`, `ADD COLUMN`) criariam histórico ruidoso para um schema em definição. A reescrita produz migrations limpas, legíveis e sem acúmulo de dívida.

### 2. GENERATED ALWAYS AS IDENTITY para PKs BIGINT
**Escolha**: `GENERATED ALWAYS AS IDENTITY` (SQL:2003, PostgreSQL 10+) via `autoIncrement: true` no Liquibase.
**Alternativa descartada**: Sequences explícitas (`SEQ_*` com `defaultValueSequenceNext`).
**Motivo**: Padrão de mercado para PostgreSQL moderno. PostgreSQL gerencia internamente sem exposição de sequence no schema. Sem configuração de `incrementBy` para sincronizar com Hibernate.
**Atenção**: PKs UUID usam `gen_random_uuid()`. PKs textuais (`SCOS_CONFIGURATION`) e compostas não usam auto-increment.

### 3. Remoção de SCOS_PERMISSION
**Escolha**: Deletar `scos_permission.yml` completamente.
**Alternativa descartada**: Manter e adicionar novas tabelas em paralelo.
**Motivo**: `SCOS_PERMISSION` não existe no domain model. O novo modelo (`SCOS_SYSTEM` + `SCOS_RESOURCE` + `SCOS_PROFILE_RESOURCE`) é incompatível com o antigo — mantê-los juntos criaria ambiguidade arquitetural.

### 4. PK composta em tabelas de endereço
**Escolha**: `(COMPANY_ID_ADDRESS, COMPANY_ID)` e `(EMPLOYEE_ID_ADDRESS, EMPLOYEE_ID)` como PKs compostas, sem surrogate key e sem sequence.
**Alternativa descartada**: PK surrogate BIGINT + UK composta (atual no Liquibase).
**Motivo**: O domain model especifica explicitamente PKs compostas com referência externa gerenciada pela aplicação. O `*_ID_ADDRESS` vem de sistema externo de endereços — não deve ser gerado aqui.

### 5. Índices criados no mesmo changeSet da tabela
**Escolha**: `createIndex` dentro do mesmo changeSet que `createTable`.
**Motivo**: Atomicidade — tabela e seus índices de FK/performance são criados ou revertidos juntos. Segue o padrão já adotado nas migrations existentes.

### 6. Ordem das migrations por dependência FK
**Escolha**: Ordem estrita por dependência: sem FK antes de com FK.
```
DEPARTMENT → POSITION → COMPANY → COMPANY_ADDRESS → COMPANY_CONTACT
→ EMPLOYEE → EMPLOYEE_ADDRESS → EMPLOYEE_CONTACT
→ PROFILE → SYSTEM → RESOURCE → PROFILE_RESOURCE
→ LOGIN → CONFIGURATION
→ INTEGRATION_KEYCLOAK → INTEGRATION_KEYCLOAK_LOG → INTEGRATION_MESSAGE_INVALID
```
**Motivo**: PostgreSQL valida FKs no momento da criação. Sem deferrable constraints — ordem garante que a tabela referenciada sempre exista antes.

## Risks / Trade-offs

- **Checksum Liquibase**: Reescrita de changeSets existentes invalida checksums. → Banco deve ser recriado do zero (`liquibase update` em banco limpo). Se necessário em banco existente: `liquibase clearCheckSums` antes.
- **GENERATED ALWAYS AS IDENTITY + Hibernate**: Requer `@GeneratedValue(strategy = GenerationType.IDENTITY)` nas entidades. → Escopo separado (JPA), mas deve ser alinhado após esta mudança.
- **INSERT explícito bloqueado**: `GENERATED ALWAYS AS IDENTITY` rejeita INSERT com valor manual sem `OVERRIDING SYSTEM VALUE`. → Scripts de seed devem usar `OVERRIDING SYSTEM VALUE` ou omitir a coluna PK.
- **SCOS_CONFIGURATION PK textual**: INSERT sempre requer fornecer o valor da chave. → Sem risco em runtime — a aplicação conhece as chaves. Risco em seeds manuais.
