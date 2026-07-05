## 1. Domain — ReasonActivate

- [x] 1.1 Criar `ReasonActivateInput`/`ReasonActivateOutput` (dto)
- [x] 1.2 Criar `ReasonActivateMapper` (dto ↔ entity)
- [x] 1.3 Criar `ReasonActivateService` (specification) + `ReasonActivateServiceBean` (validação de unicidade de `code`, transições enable/disable idempotentes)
- [x] 1.4 Adicionar `existsByCode`/`existsByCodeAndNotId` a `ReasonActivateRepository`

## 2. Domain — ReasonInactivate

- [x] 2.1 Criar `ReasonInactivateInput`/`ReasonInactivateOutput` (dto)
- [x] 2.2 Criar `ReasonInactivateMapper`
- [x] 2.3 Criar `ReasonInactivateService` + `ReasonInactivateServiceBean`
- [x] 2.4 Adicionar `existsByCode`/`existsByCodeAndNotId` a `ReasonInactivateRepository`

## 3. Domain — ReasonDisable

- [x] 3.1 Criar `ReasonDisableInput`/`ReasonDisableOutput` (dto)
- [x] 3.2 Criar `ReasonDisableMapper`
- [x] 3.3 Criar `ReasonDisableService` + `ReasonDisableServiceBean`
- [x] 3.4 Adicionar `existsByCode`/`existsByCodeAndNotId` a `ReasonDisableRepository`

## 4. Domain — ReasonEnable

- [x] 4.1 Criar `ReasonEnableInput`/`ReasonEnableOutput` (dto)
- [x] 4.2 Criar `ReasonEnableMapper`
- [x] 4.3 Criar `ReasonEnableService` + `ReasonEnableServiceBean`
- [x] 4.4 Adicionar `existsByCode`/`existsByCodeAndNotId` a `ReasonEnableRepository`

## 5. Exceções e mensagens

- [x] 5.1 Adicionar 16 constantes a `ExceptionCodeError` (`SCOS_REASON_{ACTIVATE,INACTIVATE,DISABLE,ENABLE}_001..004`) com `code`/`httpCode`/`title` conforme tabela do design
- [x] 5.2 Adicionar 16 mensagens de `detail` (pt-br) a `scos_message_organization.properties`
- [x] 5.3 Adicionar 16 mensagens de `detail` (en) a `scos_message_organization_en.properties`

## 6. Permissões

- [x] 6.1 Adicionar 20 novas entradas a `ScosOrganizationPermission` (`GET/CREATE/UPDATE/ENABLE/DISABLE_REASON_{ACTIVATE,INACTIVATE,DISABLE,ENABLE}`)

## 7. Use Cases — ReasonActivate

- [x] 7.1 Criar `CreateReasonActivateUseCase` + Bean
- [x] 7.2 Criar `UpdateReasonActivateUseCase` + Bean
- [x] 7.3 Criar `FindReasonActivateUseCase` + Bean
- [x] 7.4 Criar `FindAllReasonActivateUseCase` + Bean
- [x] 7.5 Criar `EnableReasonActivateUseCase` + Bean
- [x] 7.6 Criar `DisableReasonActivateUseCase` + Bean
- [x] 7.7 Criar `ReasonActivateApiMapper` (domain dto ↔ OpenAPI generated dto)

## 8. Use Cases — ReasonInactivate

- [x] 8.1 Criar `CreateReasonInactivateUseCase` + Bean
- [x] 8.2 Criar `UpdateReasonInactivateUseCase` + Bean
- [x] 8.3 Criar `FindReasonInactivateUseCase` + Bean
- [x] 8.4 Criar `FindAllReasonInactivateUseCase` + Bean
- [x] 8.5 Criar `EnableReasonInactivateUseCase` + Bean
- [x] 8.6 Criar `DisableReasonInactivateUseCase` + Bean
- [x] 8.7 Criar `ReasonInactivateApiMapper`

## 9. Use Cases — ReasonDisable

- [x] 9.1 Criar `CreateReasonDisableUseCase` + Bean
- [x] 9.2 Criar `UpdateReasonDisableUseCase` + Bean
- [x] 9.3 Criar `FindReasonDisableUseCase` + Bean
- [x] 9.4 Criar `FindAllReasonDisableUseCase` + Bean
- [x] 9.5 Criar `EnableReasonDisableUseCase` + Bean
- [x] 9.6 Criar `DisableReasonDisableUseCase` + Bean
- [x] 9.7 Criar `ReasonDisableApiMapper`

## 10. Use Cases — ReasonEnable

- [x] 10.1 Criar `CreateReasonEnableUseCase` + Bean
- [x] 10.2 Criar `UpdateReasonEnableUseCase` + Bean
- [x] 10.3 Criar `FindReasonEnableUseCase` + Bean
- [x] 10.4 Criar `FindAllReasonEnableUseCase` + Bean
- [x] 10.5 Criar `EnableReasonEnableUseCase` + Bean
- [x] 10.6 Criar `DisableReasonEnableUseCase` + Bean
- [x] 10.7 Criar `ReasonEnableApiMapper`

## 11. Delegates

- [x] 11.1 Criar `ReasonActivateDelegate implements ReasonActivateApiDelegate`
- [x] 11.2 Criar `ReasonInactivateDelegate implements ReasonInactivateApiDelegate`
- [x] 11.3 Criar `ReasonDisableDelegate implements ReasonDisableApiDelegate`
- [x] 11.4 Criar `ReasonEnableDelegate implements ReasonEnableApiDelegate`

## 12. Testes unitários

- [x] 12.1 `ReasonActivateServiceBeanTest` (Mockito, unicidade de code, enable/disable idempotente, 404)
- [x] 12.2 `ReasonInactivateServiceBeanTest`
- [x] 12.3 `ReasonDisableServiceBeanTest`
- [x] 12.4 `ReasonEnableServiceBeanTest`

## 13. Verificação final

- [x] 13.1 Conferir que os DTOs OpenAPI gerados (`ReasonEntityType`) refletem os 3 valores (`COMPANY`/`EMPLOYEE`/`LOGIN`) antes de finalizar os `ApiMapper`
- [x] 13.2 Build completo do módulo (`mvn compile`) sem erros nos 4 catálogos
- [ ] 13.3 Validar manualmente ao menos 1 endpoint de cada tipo (create/list/get/update/enable/disable) via um dos 4 catálogos
