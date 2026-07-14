## 1. Contrato (OpenAPI)

- [x] 1.1 Confirmar em `etc/api/organization/ScosOrganization_Company.yml` as operations UC-001..005 (`POST`/`PUT`/`GET {id}`/`GET` lista), respostas `201`/`204`/`200 data`/`4XX`-`5XX` via `ScosComponents.yml` e `x-authorize` (`CREATE_COMPANY`/`UPDATE_COMPANY`/`GET_COMPANY`)
- [x] 1.2 Confirmar validações de campo no schema: `taxIdentifier` (`x-is-cnpj`, `pattern`, `x-jdempotentrequestpayload` no POST), obrigatoriedade e `x-required-message`/`x-empty-message`, `sectorOfActivity` texto livre ≤100
- [x] 1.3 Regenerar as interfaces `CompanyApiDelegate`/DTOs a partir do YAML (build)

## 2. Domínio (regras de negócio)

- [x] 2.1 Criar DTOs `CompanyInput`, `CompanyOutput` e `ParentCompanyOutput` em `scos-organization-domain/.../company/dto/`
- [x] 2.2 Criar `CompanyMapper` (entidade `Company` ⇄ `CompanyOutput`, desembrulhando `Cnpj` e FKs)
- [x] 2.3 Criar a interface pública `CompanyService` (specification) com `create`/`update`/`findById`/`findAll`
- [x] 2.4 Implementar `CompanyServiceBean` (`@Service` package-private, `@Transactional`; leitura `readOnly = true`): unicidade de `taxIdentifier` (`SCOS_COMPANY_002`), FKs (`404`), compatibilidade de `reasonActivateId` reusando o service de `ReasonActivate` (`422`), empresa mãe `ACTIVE` (`422`), profundidade ≤ `COMPANY_HIERARCHY_MAX_DEPTH` (`422`), na ordem `409 → 404 → 422`
- [x] 2.5 Na criação, montar a 1ª linha de `SCOS_COMPANY_STATUS_HISTORY` (`newStatus=ACTIVE` + `reasonActivateId`) e persistir na mesma transação
- [x] 2.6 No update, garantir unicidade de `taxIdentifier` excluindo o próprio `{id}` (`existsByTaxIdentifierAndNotId`) e ignorar `parentCompanyId` (imutável); `404 SCOS_COMPANY_001` se não existir
- [x] 2.7 Adicionar códigos de erro dedicados `SCOS_COMPANY_008..011` (422) ao enum `ExceptionCodeError` + mensagens i18n PT/EN (motivo inativo/incompatível, mãe inativa, profundidade excedida)

## 3. Use Cases (orquestração)

- [x] 3.1 Criar `CreateCompanyUseCase` (+Bean) — mapeia DTO e chama `CompanyService.create` (sem validação)
- [x] 3.2 Criar `UpdateCompanyUseCase` (+Bean)
- [x] 3.3 Criar `FindCompanyUseCase` (+Bean) — findById
- [x] 3.4 Criar `FindAllCompanyUseCase` (+Bean) — listagem paginada com filtro por status/nome
- [x] 3.5 Criar `CompanyApiMapper` (API DTO ⇄ domain `CompanyOutput`)

## 4. API (Delegate)

- [x] 4.1 Criar `CompanyDelegate implements CompanyApiDelegate` em `scos-organization-api/.../delegate/company/`, ligando os 4 endpoints aos use cases
- [x] 4.2 Garantir formato de resposta: `200` com `data`, `201`/`204` sem corpo, `paginatedDTO` na listagem
- [x] 4.3 `CREATE_COMPANY`/`UPDATE_COMPANY`/`GET_COMPANY` já existem no `ScosOrganizationPermission` — sem mudança necessária

## 5. Testes

- [x] 5.1 Testes unitários de `CompanyServiceBean` (Mockito): unicidade, FK inexistente, motivo inativo/incompatível, mãe inativa, profundidade excedida — `CompanyServiceBeanTest` 15/15 verde
- [x] 5.2 Testes unitários de `CompanyApiMapper` (mapeamento completo/resumo; conversão de status) — `CompanyApiMapperTest` 4/4 verde
- [x] 5.3 Testes de integração (Testcontainers + Postgres) cobrindo cada UC-S/UC-E de UC-001..005, incluindo o side effect em `SCOS_COMPANY_STATUS_HISTORY` e a ordem `400 → 409 → 404 → 422` — `CompanyControllerTest` 17/17 verde (CNPJ único por teste p/ evitar colisão no cache de idempotência)
- [x] 5.4 Teste de integração de idempotência do POST (mesma `taxIdentifier` → resposta original, sem duplicar) — coberto em `CompanyControllerTest#create_sameRequestTwice_isIdempotent`

## 6. Verificação final

- [x] 6.1 Rodar a suíte (unit + integração) verde — unit 19/19, IT `CompanyControllerTest` 18/18 e `LegalNatureControllerTest` 27/27. `@AfterEach` na base `ScosOrganizationTestUtil` limpa o cache `scos:authority:ctx` ao fim de cada teste, corrigindo também os 403 pré-existentes de LegalNature. Único contorno de ambiente: `-Denforcer.skip=true` (enforcer `RequireUpperBoundDeps` em `scos-security-starter`/hibernate-spatial, alheio ao change)
- [x] 6.2 Validar o YAML/contrato e conferir que os 4 endpoints respondem conforme `01-empresa.md`
