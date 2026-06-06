## Why

As migrations Liquibase existentes divergem do domain model definido em `etc/database/domain_model.md` — há tabelas ausentes, colunas com nullability errada, tamanhos incorretos, campos extras sem correspondência no modelo e um modelo de permissões obsoleto (`SCOS_PERMISSION`). Todo desenvolvimento parte de um schema físico incorreto. O sistema ainda não tem versão em produção, o que torna este o momento ideal para uma reescrita limpa.

> Referência completa do gap analysis e decisões técnicas: `etc/doc/ideia/20260606_adequacao-liquibase-domain-model.md`

## What Changes

- **BREAKING** — Remover `SCOS_PERMISSION` e substituir pelo modelo multi-sistema: `SCOS_SYSTEM` + `SCOS_RESOURCE` + `SCOS_PROFILE_RESOURCE`
- Criar migrations para `SCOS_SYSTEM`, `SCOS_RESOURCE`, `SCOS_PROFILE_RESOURCE` (ausentes)
- Reescrever `SCOS_LOGIN`: remover `PASSWORD`/`SALT`/`DATE_LAST_CHANGE_PASSWORD`, tornar `EMPLOYEE_ID` nullable, adicionar `TYPE`
- Reescrever `SCOS_CONFIGURATION`: PK textual `VARCHAR(50)`, estrutura chave-valor tipado
- Corrigir `SCOS_EMPLOYEE`: `COMPANY_ID`/`POSITION_ID` NOT NULL, adicionar `STATUS`, remover `ACTIVE`
- Corrigir `SCOS_COMPANY`: remover `ACTIVE`, ajustar tamanhos
- Corrigir `SCOS_PROFILE`: remover `FEATURES TEXT[]`, ajustar tamanhos
- Corrigir `SCOS_COMPANY_ADDRESS` e `SCOS_EMPLOYEE_ADDRESS`: PK composta com nomenclatura correta
- Corrigir `SCOS_COMPANY_CONTACT` e `SCOS_EMPLOYEE_CONTACT`: renomear PKs, adicionar UKs, remover campos extras
- Corrigir `SCOS_INTEGRATION_KEYCLOAK`: adicionar `RETRY_COUNT` e `MAX_RETRIES`
- Corrigir `SCOS_INTEGRATION_KEYCLOAK_LOG` e `SCOS_INTEGRATION_MESSAGE_INVALID`: remover `UPDATED_AT` (tabelas imutáveis)
- Substituir todas as sequences explícitas (`SEQ_*`) por `GENERATED ALWAYS AS IDENTITY` (padrão SQL/PostgreSQL 10+)
- Corrigir `UPDATED_AT NOT NULL` e `USER_AT VARCHAR(255) NOT NULL` em todas as tabelas de domínio
- Adicionar 6 índices de performance: `IDX_STATUS` em EMPLOYEE/COMPANY/INTEGRATION_KEYCLOAK, `IDX_KEYCLOAK_ID` em LOGIN/INTEGRATION_KEYCLOAK, `IDX_RESOURCE_ID` em PROFILE_RESOURCE

## Capabilities

### New Capabilities
- `schema-permissions-multisistema`: Modelo de permissões multi-sistema com SCOS_SYSTEM, SCOS_RESOURCE e SCOS_PROFILE_RESOURCE
- `schema-organization`: Schema correto para departamentos, cargos, empresas, filiais e funcionários
- `schema-access`: Schema correto para logins, perfis e autenticação via Keycloak (sem auth local)
- `schema-integration-keycloak`: Schema completo para rastreio de operações de integração Keycloak com retry
- `schema-configuration`: Schema chave-valor tipado para configurações do sistema

### Modified Capabilities
<!-- Nenhuma spec existente — primeiro setup do schema -->

## Impact

- **Liquibase**: 15 arquivos de migration reescritos, 3 novos, 1 removido, `tables.yml` reorganizado
- **Banco de dados**: Banco deve ser recriado do zero (`liquibase update` em banco limpo)
- **Fora de escopo**: JPA entities, código Java, YAML de aplicação
- **Escopo**: exclusivamente `scos-organization-boot/src/main/resources/db/changelog/`
