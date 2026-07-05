## 1. Domain — AddressType

- [x] 1.1 Criar `AddressTypeInput`/`AddressTypeOutput` em `corporate/catalog/dto/`
- [x] 1.2 Adicionar `existsByCode`/`existsByCodeAndNotId` (QueryDSL default methods) em `AddressTypeRepository`
- [x] 1.3 Criar `AddressTypeMapper` (dto ↔ entity) em `corporate/catalog/service/`
- [x] 1.4 Criar `AddressTypeService` (specification) em `corporate/catalog/specification/`
- [x] 1.5 Criar `AddressTypeServiceBean` — `create`/`update`/`findById`/`findAll`/`enable`/`disable`, validando unicidade de `code` e idempotência de `enable`/`disable`

## 2. Domain — ContactType

- [x] 2.1 Criar `ContactTypeInput`/`ContactTypeOutput` em `corporate/catalog/dto/`
- [x] 2.2 Adicionar `existsByCode`/`existsByCodeAndNotId` em `ContactTypeRepository`
- [x] 2.3 Criar `ContactTypeMapper` em `corporate/catalog/service/`
- [x] 2.4 Criar `ContactTypeService` (specification) em `corporate/catalog/specification/`
- [x] 2.5 Criar `ContactTypeServiceBean` (mesmo padrão de 1.5)

## 3. Usecase — AddressType

- [x] 3.1 Criar `CreateAddressTypeUseCase`/`Bean`, `UpdateAddressTypeUseCase`/`Bean`
- [x] 3.2 Criar `FindAddressTypeUseCase`/`Bean`, `FindAllAddressTypeUseCase`/`Bean` (com filtro `entityType` opcional)
- [x] 3.3 Criar `EnableAddressTypeUseCase`/`Bean`, `DisableAddressTypeUseCase`/`Bean`
- [x] 3.4 Criar `AddressTypeApiMapper` (domain dto ↔ dto OpenAPI gerado)

## 4. Usecase — ContactType

- [x] 4.1 Criar `CreateContactTypeUseCase`/`Bean`, `UpdateContactTypeUseCase`/`Bean`
- [x] 4.2 Criar `FindContactTypeUseCase`/`Bean`, `FindAllContactTypeUseCase`/`Bean` (com filtro `entityType` opcional)
- [x] 4.3 Criar `EnableContactTypeUseCase`/`Bean`, `DisableContactTypeUseCase`/`Bean`
- [x] 4.4 Criar `ContactTypeApiMapper`

## 5. Api — Delegates

- [x] 5.1 Criar `AddressTypeDelegate implements AddressTypeApiDelegate` em `delegate/catalog/`
- [x] 5.2 Criar `ContactTypeDelegate implements ContactTypeApiDelegate` em `delegate/catalog/`

## 6. Permissões e mensagens de erro

- [x] 6.1 Adicionar 5 entradas em `ScosOrganizationPermission` para `AddressType` (`GET`/`CREATE`/`UPDATE`/`ENABLE`/`DISABLE_ADDRESS_TYPE`)
- [x] 6.2 Adicionar 5 entradas em `ScosOrganizationPermission` para `ContactType` (mesmo conjunto)
- [x] 6.3 Adicionar 4 constantes em `ExceptionCodeError` para `AddressType` (`SCOS_ADDRESS_TYPE_001..004`) com `httpCode`/`title` conforme design.md
- [x] 6.4 Adicionar 4 constantes em `ExceptionCodeError` para `ContactType` (`SCOS_CONTACT_TYPE_001..004`)
- [x] 6.5 Adicionar as 8 mensagens de `detail` em `scos_message_organization.properties`/`_en.properties`

## 7. Validação

- [x] 7.1 Criar `AddressTypeServiceBeanTest` (Mockito) cobrindo unicidade, idempotência, not-found — 16 testes
- [x] 7.2 Criar `ContactTypeServiceBeanTest` (mesmo padrão) — 16 testes
- [x] 7.3 Rodar suíte de testes do módulo `scos-organization-domain` e confirmar que nada quebrou — 68/68 OK
- [x] 7.4 Build completo dos módulos afetados (`domain`, `usecase`, `api`, `infrastructure`, `shared`) — BUILD SUCCESS

## 8. Pré-requisito descoberto durante implementação

- [x] 8.1 `ScosOrganization_Catalog.yml` não estava plugado no `openapi-generator-maven-plugin` (nem em `scos-organization-api/pom.xml` nem em `scos-organization-usecase/pom.xml`) — adicionada a execução `ScosOrganization_Catalog` em ambos os poms, mesmo padrão das demais (`generateModels=true`/`generateApis=false` no usecase; inverso no api)
