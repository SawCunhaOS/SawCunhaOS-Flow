## 1. Contrato OpenAPI & regeneração

- [x] 1.1 Adicionar `maxLength: 30` a `code` e `maxLength: 255` a `description` em `CreateCnaeRequest` e `UpdateCnaeRequest` (`etc/api/organization/ScosOrganization_Company.yml`)
- [x] 1.2 Rodar `generate-sources` e confirmar `@Size(max=30/255)` nos DTOs regenerados (`CreateCnaeRequest`/`UpdateCnaeRequest`)

## 2. Banco (Liquibase)

- [x] 2.1 Criar changeSet `ALTER COLUMN SCOS_CNAE.CODE varchar(10) → varchar(30)` preservando `UK_CODE_SCOS_CNAE`, com `rollback` para `varchar(10)`
- [ ] 2.2 Registrar o changeSet no changelog e aplicar localmente confirmando que a migração sobe sem erro

## 3. Domínio — repositórios e guardas

- [x] 3.1 Criar `corporate/company/internal/CnaeRepository` com `findAll(Pageable)`, `existsByCode(String)` e `existsByCodeAndNotId(String, Long)` (default methods QueryDSL sobre `qCnae`)
- [x] 3.2 Adicionar `existsByCnaePrincipalId(Long)` a `CompanyRepository` (default method sobre `qCompany.cnaePrincipalId`)
- [x] 3.3 Adicionar `existsByCnaeId(Long)` a `CompanyCnaeSecondaryRepository` (default method sobre `qCompanyCnaeSecondary.id.cnaeId`)

## 4. Domínio — camada de serviço

- [x] 4.1 Criar DTOs `corporate/company/dto/CnaeInput` e `CnaeOutput`
- [x] 4.2 Criar interface `corporate/company/specification/CnaeService`
- [x] 4.3 Criar `corporate/company/service/CnaeMapper` (entidade ↔ input/output)
- [x] 4.4 Criar `corporate/company/service/CnaeServiceBean`: `create` (unicidade → `SCOS_CNAE_002`), `update` (unicidade excluindo id + `findById` → `SCOS_CNAE_001`), `find`/`findAll` (`SCOS_CNAE_001`), `delete` (guarda 3.2+3.3 → `SCOS_CNAE_003`; senão remoção física)

## 5. Erros & permissões

- [x] 5.1 Adicionar a `ExceptionCodeError`: `SCOS_CNAE_001`(404,`SCOS_TITLE_NOT_FOUND`), `SCOS_CNAE_002`(409,`SCOS_TITLE_CONFLICT`), `SCOS_CNAE_003`(422,`SCOS_TITLE_BUSINESS_RULE_VIOLATION`)
- [x] 5.2 Adicionar 3 mensagens de `detail` em `scos_message_organization.properties` e `scos_message_organization_en.properties`
- [x] 5.3 Adicionar a `ScosOrganizationPermission`: `GET_CNAE`, `CREATE_CNAE`, `UPDATE_CNAE`, `DELETE_CNAE`

## 6. Use Cases (`application/usecase/corporate/company/fiscal/cnae/`)

- [x] 6.1 `CreateCnaeUseCase` (interface) + `CreateCnaeUseCaseBean` (`@Service` package-private)
- [x] 6.2 `UpdateCnaeUseCase` + `...Bean`
- [x] 6.3 `FindCnaeUseCase` + `...Bean`
- [x] 6.4 `FindAllCnaeUseCase` + `...Bean`
- [x] 6.5 `DeleteCnaeUseCase` + `...Bean`
- [x] 6.6 `CnaeApiMapper` (DTO da API ↔ `CnaeInput`/`CnaeOutput`)

## 7. Delegate (`api/delegate/company/`)

- [x] 7.1 `CnaeDelegate implements CnaeApiDelegate` — ligar `getAllCnaes`/`createCnae`/`getCnaeById`/`updateCnae`/`deleteCnae` aos Use Cases, com autorização por permissão (`GET_CNAE`/`CREATE_CNAE`/`UPDATE_CNAE`/`DELETE_CNAE`)

## 8. Testes

- [x] 8.1 `CnaeServiceBeanTest` (Mockito): unicidade em create e update, `findById` inexistente (404), delete com vínculo principal (422), delete com vínculo secundário (422), delete sem vínculo (ok)
- [x] 8.2 ~~Testes dos Use Cases~~ — **DESCOPADO** (decisão): use cases são delegação fina; o projeto não mantém testes de use case (mesma barra do change `crud-catalogo-tipo-endereco-contato`)
- [x] 8.3 ~~Integração Testcontainers~~ — **DESCOPADO** (decisão): o projeto não possui infra Testcontainers/`@DataJpaTest`; introduzir seria iniciativa transversal, fora do escopo desta change. Guarda de delete e unicidade cobertas por unit + validação de runtime

## 9. Verificação

- [x] 9.1 Build completo (`mvn clean test`) — módulos com testes verdes
- [ ] 9.2 Smoke manual dos 5 endpoints (200/201/204/400/404/409/422) — **validação no ambiente do usuário** (Postgres+Keycloak+Redis); junto com 2.2 (migração sobe)
