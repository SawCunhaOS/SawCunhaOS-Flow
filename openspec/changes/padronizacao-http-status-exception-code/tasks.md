## 1. Mensagens de título (PT-BR/EN)

- [x] 1.1 Adicionar as 7 chaves `SCOS_TITLE_NOT_FOUND`, `SCOS_TITLE_CONFLICT`, `SCOS_TITLE_BUSINESS_RULE_VIOLATION`, `SCOS_TITLE_EXTERNAL_INTEGRATION_FAILURE`, `SCOS_TITLE_INTERNAL_ERROR`, `SCOS_TITLE_UNAUTHORIZED`, `SCOS_TITLE_GENERIC` em `scos-organization-shared/src/main/resources/scos_message_organization.properties` (PT-BR, ver design.md seção 5)
- [x] 1.2 Adicionar as mesmas 7 chaves em `scos_message_organization_en.properties` (EN, texto realmente traduzido, não igual ao PT)

## 2. ExceptionCodeError — campos e constantes

- [x] 2.1 Adicionar campo `private final int httpCode` e campo `private final String title` na classe (via `@AllArgsConstructor`)
- [x] 2.2 Atualizar as 6 constantes `SCOS_CONFIGURATION_*`/`SCOS_DEPARTMENT_*` com `httpCode`/`title` conforme a tabela do design.md
- [x] 2.3 Atualizar as 5 constantes `SCOS_POSITION_*` conforme a tabela
- [x] 2.4 Atualizar as 7 constantes `SCOS_COMPANY_*` conforme a tabela (inclui `SCOS_COMPANY_003`/`SCOS_COMPANY_006`, sem throw site hoje)
- [x] 2.5 Atualizar `SCOS_EMPLOYEE_001` conforme a tabela
- [x] 2.6 Atualizar as 4 constantes `SCOS_USER_*` conforme a tabela
- [x] 2.7 Atualizar `SCOS_AUTHORITY_001` conforme a tabela
- [x] 2.8 Atualizar as 6 constantes `SCOS_LOGIN_*` conforme a tabela (`001/002/003` mantêm `400`/`SCOS_TITLE_GENERIC`; `010/011` recebem `401`/`SCOS_TITLE_UNAUTHORIZED` mesmo sem throw site hoje; `013` recebe `422`/`SCOS_TITLE_BUSINESS_RULE_VIOLATION`)
- [x] 2.9 Remover o override manual de `getHttpCode()` (o campo + `@Getter` já satisfazem a interface)

## 3. Validação

- [x] 3.1 Rodar `DepartmentServiceBeanTest`/`PositionServiceBeanTest` existentes e confirmar que continuam passando (comparam só `code`, não `httpCode`/`title`) — 36/36 OK
- [x] 3.2 Adicionar/ajustar asserções de `httpCode`/`title` num teste (novo ou existente) para pelo menos um código de cada categoria (`404`, `409`, `422`, `502`, `500`, `401`, órfão `400`) — `ExceptionCodeErrorTest` novo, 7/7 OK
- [x] 3.3 Build completo do módulo `scos-organization-shared` (compilação — todas as 29 constantes precisam compilar com a nova assinatura de construtor)
