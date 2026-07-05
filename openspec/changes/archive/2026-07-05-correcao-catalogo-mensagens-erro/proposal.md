## Why

O catálogo bilíngue de mensagens de erro (`etc/doc/usecase/07-mensagens-erro-pt-en.md`) ficou defasado do código real: `scos_message_organization_en.properties` e `scos_message_validation_en.properties` têm entradas com texto em português colado por engano (não traduzido), traduções ausentes para códigos já existentes em PT, frases incompletas em inglês, e um bug de sentido — `SCOS_DEPARTMENT_003`/`SCOS_POSITION_003` usam "delete" em inglês onde o português diz "inativar", contradizendo a regra de domínio de que essas entidades não têm exclusão física/lógica irreversível. Há também um typo de digitação (`SCOS_VALIDATION_009`) e uma inversão de parâmetros `{1}`/`{2}` em `SCOS_VALIDATION_006` frente ao padrão usado no `SCOS_VALIDATION_008`.

## What Changes

- Completar `scos_message_organization_en.properties`: adicionar traduções reais (não PT colado) para `SCOS_DEPARTMENT_004/005/006`, `SCOS_POSITION_004/005`, `SCOS_AUTHORITY_001`, `SCOS_LOGIN_010/011`, `SCOS_CONFIGURATION_001/002`; corrigir `SCOS_COMPANY_003/006` (wording) e `SCOS_USER_001-004` (pontuação/caixa)
- Corrigir `SCOS_DEPARTMENT_003`/`SCOS_POSITION_003` em EN — troca de verbo "delete" → "disable" e inclusão do qualificador "active", alinhado à regra de domínio (sem exclusão física/lógica)
- Completar `scos_message_validation_en.properties`: traduções reais para `SCOS_VALIDATION_010/011/012`; corrigir frases incompletas em `SCOS_VALIDATION_002/004`
- Corrigir `scos_message_validation.properties` (PT): typo em `SCOS_VALIDATION_009`; inverter ordem de `{1}`/`{2}` em `SCOS_VALIDATION_006` para bater com `SCOS_VALIDATION_008`
- Reescrever `SCOS_COMPANY_003`/`006` (PT+EN) em `scos_message_organization.properties`: trocar "excluir"→"bloquear", remover referência a "departamentos" em `003` (sem vínculo real no schema) — nenhuma classe Java lança esses códigos hoje, é só correção de catálogo, documentada como alvo futuro
- Polir `SCOS_USER_001-004` (PT): pontuação final, minúscula em "usuário", "apagar"→"excluir" (consistência de verbo)
- Sincronizar `etc/doc/usecase/07-mensagens-erro-pt-en.md` com o texto final acordado (PT + EN) — já aplicado nesta ideia, serve de fonte de verdade para o patch de código
- **Fora de escopo**: implementar as regras de negócio ainda não codificadas (`SCOS_COMPANY_004/005/006`); editar o enum de validação genérica (`SCOS_VALIDATION_*` vive em `scos-foundation-utils`, dependência externa); `ScosOrganizationPermission.java` (nomenclatura de permissões, assunto separado); alterar numeração de qualquer código

## Capabilities

### New Capabilities

- `error-message-catalog`: Garante que toda mensagem de erro lançada pelo domínio (`ExceptionCodeError`) tenha texto correto e sincronizado em PT e EN, sem texto de um idioma colado no arquivo do outro, sem frases incompletas, e sem descrever comportamento (verbo/ação) que o código não implementa.

### Modified Capabilities

_Nenhuma — não existe spec de capability publicada para mensagens de erro em `openspec/specs/`; toda a correção é registrada como capability nova (`error-message-catalog`)._

## Impact

- `scos-organization-shared/src/main/resources/scos_message_organization.properties` (PT): `SCOS_COMPANY_003/006`, `SCOS_USER_001-004`
- `scos-organization-shared/src/main/resources/scos_message_organization_en.properties` (EN): `SCOS_DEPARTMENT_003/004/005/006`, `SCOS_POSITION_003/004/005`, `SCOS_COMPANY_003/006`, `SCOS_CONFIGURATION_001/002`, `SCOS_USER_001-004`, `SCOS_AUTHORITY_001`, `SCOS_LOGIN_010/011`
- `scos-organization-shared/src/main/resources/scos_message_validation.properties` (PT): `SCOS_VALIDATION_006/009`
- `scos-organization-shared/src/main/resources/scos_message_validation_en.properties` (EN): `SCOS_VALIDATION_002/004/010/011/012`
- `etc/doc/usecase/07-mensagens-erro-pt-en.md`: já sincronizado (referência/fonte de verdade do patch)
- `ExceptionCodeError.java`: sem alteração — todos os códigos já existem no enum (commit `20338f9`)
- Sem impacto em Liquibase, OpenAPI ou numeração de código
