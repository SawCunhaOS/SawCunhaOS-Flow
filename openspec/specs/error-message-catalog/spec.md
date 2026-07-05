## ADDED Requirements

### Requirement: Mensagens EN não contêm texto em português colado
Toda entrada em `scos_message_organization_en.properties` e `scos_message_validation_en.properties` SHALL conter uma tradução real em inglês — nenhuma entrada MAY conter o mesmo texto em português presente no arquivo PT correspondente.

#### Scenario: Chave de configuração lida do arquivo EN
- **WHEN** o sistema resolve `SCOS_CONFIGURATION_001` ou `SCOS_CONFIGURATION_002` usando o locale EN
- **THEN** o texto retornado está em inglês e é diferente do texto PT da mesma chave

#### Scenario: Chave de autoridade/login lida do arquivo EN
- **WHEN** o sistema resolve `SCOS_AUTHORITY_001`, `SCOS_LOGIN_010` ou `SCOS_LOGIN_011` usando o locale EN
- **THEN** o texto retornado está em inglês e é diferente do texto PT da mesma chave

#### Scenario: Códigos de validação de documento lidos do arquivo EN
- **WHEN** o sistema resolve `SCOS_VALIDATION_010`, `SCOS_VALIDATION_011` ou `SCOS_VALIDATION_012` usando o locale EN
- **THEN** o texto retornado está em inglês e é diferente do texto PT da mesma chave

### Requirement: Toda chave com texto PT tem tradução EN correspondente
Toda chave presente em `scos_message_organization.properties` ou `scos_message_validation.properties` (PT) SHALL possuir uma chave correspondente no arquivo `_en.properties` do mesmo módulo.

#### Scenario: Códigos de Department/Position recém-adicionados ao PT
- **WHEN** o sistema resolve `SCOS_DEPARTMENT_004`, `SCOS_DEPARTMENT_005`, `SCOS_DEPARTMENT_006`, `SCOS_POSITION_004` ou `SCOS_POSITION_005` usando o locale EN
- **THEN** o sistema retorna um texto em inglês (não lança `NoSuchMessageException` nem retorna a chave literal)

### Requirement: Texto da mensagem reflete a operação real do código
O texto de uma mensagem de erro SHALL usar o verbo/ação que corresponde à operação real implementada pelo código que lança aquele `ExceptionCodeError` — mensagens de código sem exclusão física/lógica implementada SHALL NOT usar o verbo "delete"/"excluir".

#### Scenario: Departamento com posições ativas vinculadas
- **WHEN** o sistema lança `SCOS_DEPARTMENT_003` ao tentar desabilitar um departamento com posições `ACTIVE` vinculadas
- **THEN** o texto EN usa o verbo "disable" e menciona "active positions", não "delete"

#### Scenario: Cargo com funcionários ativos vinculados
- **WHEN** o sistema lança `SCOS_POSITION_003` ao tentar desabilitar um cargo com funcionários `ACTIVE` vinculados
- **THEN** o texto EN usa o verbo "disable" e menciona "active employees", não "delete"

### Requirement: Placeholders de limite seguem ordem consistente entre códigos de validação
Nos códigos de validação que usam dois placeholders de limite, `{1}` SHALL representar o valor mínimo e `{2}` SHALL representar o valor máximo, de forma consistente entre `SCOS_VALIDATION_006` e `SCOS_VALIDATION_008`.

#### Scenario: Valor fora do intervalo mínimo/máximo
- **WHEN** o sistema resolve `SCOS_VALIDATION_006` em PT com um mínimo e um máximo
- **THEN** o texto renderizado apresenta o mínimo antes do máximo, na mesma ordem usada por `SCOS_VALIDATION_008`

### Requirement: Mensagens que descrevem regra ainda não implementada trazem nota de transparência
Quando o texto de uma mensagem no catálogo de referência (`etc/doc/usecase/07-mensagens-erro-pt-en.md`) descrever uma operação (ex.: `block`) que nenhuma classe Java aplica atualmente, o catálogo SHALL trazer uma nota de uma linha explicando essa divergência.

#### Scenario: Catálogo documenta bloqueio de empresa ainda não implementado
- **WHEN** um desenvolvedor consulta a entrada de `SCOS_COMPANY_003` ou `SCOS_COMPANY_006` no catálogo de referência
- **THEN** a coluna de nota informa que nenhuma classe Java lança esse código hoje e que o texto documenta o alvo (`block`) para implementação futura
