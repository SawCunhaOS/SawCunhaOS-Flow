## 1. Preparação

- [x] 1.1 Ler `ExceptionCodeError` e o bundle i18n de cada agregado (AddressType, ContactType, Position, Reason*, Cnae, LegalNature, Configuration) e mapear os `code`/`title`/`detail` de 404/409/422/validação
- [x] 1.2 Confirmar no seed `setsup_database.sql` os registros de cada agregado (id, code, active) e o vínculo company↔cnae/legal_nature usado no 422 de DELETE
- [x] 1.3 Confirmar as URIs `/api/v1/...` e as permissões de cada delegate (GET/CREATE/UPDATE/ENABLE/DISABLE/DELETE) no contrato e no `ScosOrganizationPermission`

## 2. Shape A — Catálogos (enable/disable)

- [x] 2.1 `api/catalog/AddressTypeControllerTest` — GET(lista+id), POST, PUT, enable, disable; segurança (401/401-inválido/403), 404/409/422/400, idempotência POST, filtros `entityType`/`active`
- [x] 2.2 `api/catalog/ContactTypeControllerTest` — mesma matriz do 2.1 com códigos/filtros do ContactType

## 3. Shape A — Position (enable/disable)

- [x] 3.1 `api/position/PositionControllerTest` — matriz completa shape A + filtros `departmentId`/`active` e regra de vínculo com department

## 4. Shape A — Reasons (enable/disable)

- [x] 4.1 `api/reason/ReasonActivateControllerTest` — matriz completa shape A com códigos `SCOS_REASON_*` da variante
- [x] 4.2 `api/reason/ReasonInactivateControllerTest` — idem para a variante
- [x] 4.3 `api/reason/ReasonEnableControllerTest` — idem para a variante
- [x] 4.4 `api/reason/ReasonDisableControllerTest` — idem para a variante

## 5. Shape B — Company (DELETE)

- [x] 5.1 `api/company/CnaeControllerTest` — GET(lista+id), POST, PUT, DELETE; 404 `SCOS_CNAE_001`, 409 `_002`, 422 `_003` (DELETE com vínculo FK da company do seed) + segurança/validação
- [x] 5.2 `api/company/LegalNatureControllerTest` — mesma matriz com `SCOS_LEGAL_NATURE_00N`

## 6. Shape C — Configuration

- [x] 6.1 `api/configuration/ConfigurationControllerTest` — preencher stub: GET all, GET keys, GET by id (chave string), PUT update; 401/403/404 + validação; sem create/delete

## 7. Estabilização

- [ ] 7.1 Rodar `mvn verify` completo e corrigir flakiness de isolamento (`code` único por POST, ordem de TRUNCATE/RESTART)
- [ ] 7.2 Confirmar suíte verde de ponta a ponta e que cada resposta de erro valida o contrato RFC 9457 completo
