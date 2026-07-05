## 1. `scos_message_organization.properties` (PT)

- [x] 1.1 `SCOS_COMPANY_003`: trocar para `Não é possível bloquear a empresa, pois ela possui colaboradores vinculados.`
- [x] 1.2 `SCOS_COMPANY_006`: trocar para `Não é permitido bloquear a única empresa ativa do sistema.`
- [x] 1.3 `SCOS_USER_001`: trocar para `Ocorreu um erro ao tentar cadastrar o usuário no Keycloak.`
- [x] 1.4 `SCOS_USER_002`: trocar para `Ocorreu um erro ao tentar atualizar o usuário no Keycloak.`
- [x] 1.5 `SCOS_USER_003`: trocar para `Ocorreu um erro ao tentar excluir o usuário no Keycloak.`
- [x] 1.6 `SCOS_USER_004`: trocar para `Erro desconhecido ao processar o usuário.`

## 2. `scos_message_organization_en.properties` (EN)

- [x] 2.1 Adicionar `SCOS_DEPARTMENT_004=The informed department is already active.`
- [x] 2.2 Adicionar `SCOS_DEPARTMENT_005=The informed department is already inactive.`
- [x] 2.3 Adicionar `SCOS_DEPARTMENT_006=It is not possible to associate the position with an inactive department.`
- [x] 2.4 Adicionar `SCOS_POSITION_004=The informed position is already active.`
- [x] 2.5 Adicionar `SCOS_POSITION_005=The informed position is already inactive.`
- [x] 2.6 Corrigir `SCOS_DEPARTMENT_003` para `It was not possible to disable the department because it has active positions linked to it.` (era "delete")
- [x] 2.7 Corrigir `SCOS_POSITION_003` para `It was not possible to disable the position because it has active employees linked to it.` (era "delete")
- [x] 2.8 Corrigir `SCOS_COMPANY_003` para `It is not possible to block the company because it has linked employees.`
- [x] 2.9 Corrigir `SCOS_COMPANY_006` para `It is not allowed to block the only active company in the system.`
- [x] 2.10 Corrigir `SCOS_CONFIGURATION_001` (estava PT colado) para `The informed key does not exist in the system.`
- [x] 2.11 Corrigir `SCOS_CONFIGURATION_002` (estava PT colado) para `The informed configuration is not registered in the system.`
- [x] 2.12 Corrigir `SCOS_USER_001` para `An error occurred while trying to register the user in Keycloak.`
- [x] 2.13 Corrigir `SCOS_USER_002` para `An error occurred while trying to update the user in Keycloak.`
- [x] 2.14 Corrigir `SCOS_USER_003` para `An error occurred while trying to delete the user in Keycloak.`
- [x] 2.15 Corrigir `SCOS_USER_004` para `Unknown error while processing the user.`
- [x] 2.16 Corrigir `SCOS_AUTHORITY_001` (estava PT colado) para `The informed Login does not exist in the system.`
- [x] 2.17 Corrigir `SCOS_LOGIN_010` (estava PT colado) para `The informed Login is inactive in the system.`
- [x] 2.18 Corrigir `SCOS_LOGIN_011` (estava PT colado) para `The informed Login is blocked in the system.`

## 3. `scos_message_validation.properties` (PT)

- [x] 3.1 Corrigir `SCOS_VALIDATION_006` para `O campo ''{0}'' deve ser maior ou igual a {1} e menor ou igual a {2}.` (ordem `{1}`/`{2}` alinhada ao `SCOS_VALIDATION_008`)
- [x] 3.2 Corrigir `SCOS_VALIDATION_009` para `O CEP informado é inválido.` (typo)

## 4. `scos_message_validation_en.properties` (EN)

- [x] 4.1 Corrigir `SCOS_VALIDATION_002` para `The value in the field ''{0}'' must be greater than or equal to {1}.`
- [x] 4.2 Corrigir `SCOS_VALIDATION_004` para `The value in the field ''{0}'' must be less than or equal to {1}.`
- [x] 4.3 Corrigir `SCOS_VALIDATION_010` (estava PT colado) para `The provided CNPJ is invalid.`
- [x] 4.4 Corrigir `SCOS_VALIDATION_011` (estava PT colado) para `The provided CPF is invalid.`
- [x] 4.5 Corrigir `SCOS_VALIDATION_012` (estava PT colado) para `The provided document is invalid.`

## 5. Verificação

- [x] 5.1 Build local (`mvn -pl scos-organization-shared compile` ou equivalente) para garantir que os `.properties` carregam sem erro de parsing
- [x] 5.2 Conferir que nenhuma chave ficou órfã ou duplicada nos 4 arquivos (`organization`/`validation`, PT/EN)
- [x] 5.3 Conferir manualmente que todas as chaves de `scos_message_organization.properties`/`scos_message_validation.properties` (PT) têm par correspondente no `_en.properties` do mesmo módulo
- [x] 5.4 Confirmar que `ExceptionCodeError.java` não precisou de nenhuma alteração (checagem de escopo — não é tarefa de código)
