## 1. Contrato OpenAPI & regeneração

- [x] 1.1 Adicionar `maxLength: 30` a `code` e `maxLength: 255` a `description` em `CreateLegalNatureRequest` e `UpdateLegalNatureRequest` (`etc/api/organization/ScosOrganization_Company.yml`)
- [x] 1.2 Rodar `generate-sources` e confirmar `@Size(max=30/255)` nos DTOs regenerados

## 2. Banco (Liquibase)

- [x] 2.1 Criar changeSet `ALTER COLUMN SCOS_LEGAL_NATURE.CODE varchar(10) → varchar(30)` preservando `UK_CODE_SCOS_LEGAL_NATURE`, com `rollback` para `varchar(10)` (em `scos_legal_nature.yml`)
- [ ] 2.2 Aplicar localmente confirmando que a migração sobe sem erro — **validação no ambiente do usuário**

## 3. Domínio — repositórios e guarda

- [x] 3.1 Criar `corporate/company/internal/LegalNatureRepository` com `findAll(Pageable)`, `existsByCode(String)` e `existsByCodeAndNotId(String, Long)` (default methods QueryDSL sobre `qLegalNature`)
- [x] 3.2 Adicionar `existsByLegalNatureId(Long)` a `CompanyRepository` (default method sobre `qCompany.legalNature.id`)

## 4. Domínio — camada de serviço

- [x] 4.1 Criar DTOs `corporate/company/dto/LegalNatureInput` e `LegalNatureOutput`
- [x] 4.2 Criar interface `corporate/company/specification/LegalNatureService`
- [x] 4.3 Criar `corporate/company/service/LegalNatureMapper` (entidade ↔ output)
- [x] 4.4 Criar `corporate/company/service/LegalNatureServiceBean`: `create` (unicidade → `SCOS_LEGAL_NATURE_002`), `update` (unicidade excluindo id + `findById` → `SCOS_LEGAL_NATURE_001`), `find`/`findAll` (`SCOS_LEGAL_NATURE_001`), `delete` (guarda 3.2 → `SCOS_LEGAL_NATURE_003`; senão remoção física). Sem `updateAuditInfo` (entidade não estende `BaseEntity`)

## 5. Erros & permissões

- [x] 5.1 Adicionar a `ExceptionCodeError`: `SCOS_LEGAL_NATURE_001`(404,`SCOS_TITLE_NOT_FOUND`), `SCOS_LEGAL_NATURE_002`(409,`SCOS_TITLE_CONFLICT`), `SCOS_LEGAL_NATURE_003`(422,`SCOS_TITLE_BUSINESS_RULE_VIOLATION`)
- [x] 5.2 Adicionar 3 mensagens de `detail` em `scos_message_organization.properties` e `scos_message_organization_en.properties`
- [x] 5.3 Adicionar a `ScosOrganizationPermission`: `GET_LEGAL_NATURE`, `CREATE_LEGAL_NATURE`, `UPDATE_LEGAL_NATURE`, `DELETE_LEGAL_NATURE`

## 6. Use Cases (`application/usecase/corporate/company/fiscal/legalnature/`)

- [x] 6.1 `CreateLegalNatureUseCase` (interface) + `CreateLegalNatureUseCaseBean` (`@Service` package-private)
- [x] 6.2 `UpdateLegalNatureUseCase` + `...Bean`
- [x] 6.3 `FindLegalNatureUseCase` + `...Bean`
- [x] 6.4 `FindAllLegalNatureUseCase` + `...Bean`
- [x] 6.5 `DeleteLegalNatureUseCase` + `...Bean`
- [x] 6.6 `LegalNatureApiMapper` (DTO da API ↔ `LegalNatureInput`/`LegalNatureOutput`)

## 7. Delegate (`api/delegate/company/`)

- [x] 7.1 `LegalNatureDelegate implements LegalNatureApiDelegate` — ligar `getAllLegalNatures`/`createLegalNature`/`getLegalNatureById`/`updateLegalNature`/`deleteLegalNature` aos Use Cases

## 8. Testes

- [x] 8.1 `LegalNatureServiceBeanTest` (Mockito): unicidade em create e update, `findById` inexistente (404), delete com vínculo (422), delete sem vínculo (ok) — 11 casos verdes
- [x] 8.2 ~~Testes de Use Cases / Testcontainers~~ — **DESCOPADO**: mantém a barra atual do projeto (só `ServiceBean` unit), igual aos changes `crud-cnae` e `crud-catalogo-tipo-endereco-contato`

## 9. Verificação

- [x] 9.1 Build completo (`mvn clean test`) — todos os módulos verdes (151 testes domain, sem regressão)
- [ ] 9.2 Smoke manual dos 5 endpoints (200/201/204/400/404/409/422) — **validação no ambiente do usuário**
