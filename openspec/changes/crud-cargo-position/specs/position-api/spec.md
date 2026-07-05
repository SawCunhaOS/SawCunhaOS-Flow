## ADDED Requirements

### Requirement: GET /v1/positions retorna lista paginada filtrável por departmentId e active
O sistema SHALL retornar uma lista paginada de cargos na rota `GET /v1/positions`, incluindo os dados do departamento vinculado em cada item. Quando o parâmetro de query `departmentId` é informado, a listagem SHALL ser restrita aos cargos daquele departamento. Quando o parâmetro de query `active` é informado, a listagem SHALL ser restrita aos cargos com aquele status. Os dois filtros podem ser combinados.

#### Scenario: Listagem sem filtros
- **WHEN** `GET /v1/positions` é chamado com `page=0&size=10`
- **THEN** retorna HTTP 200 com `data[]` (cada item incluindo `department`) e `paginatedDTO` preenchidos

#### Scenario: Listagem filtrada por departmentId
- **WHEN** `GET /v1/positions?departmentId=5` é chamado e existem cargos de departamentos diferentes
- **THEN** retorna HTTP 200 com `data[]` contendo somente cargos cujo `department.id == 5`

#### Scenario: Listagem filtrada por active
- **WHEN** `GET /v1/positions?active=true` é chamado e existem cargos ativos e inativos
- **THEN** retorna HTTP 200 com `data[]` contendo somente cargos com `active == true`

#### Scenario: Listagem filtrada por departmentId e active combinados
- **WHEN** `GET /v1/positions?departmentId=5&active=false` é chamado
- **THEN** retorna HTTP 200 com `data[]` contendo somente cargos do departamento `5` com `active == false`

### Requirement: GET /v1/positions/{id} retorna cargo com departamento ou 404
O sistema SHALL retornar o cargo identificado por `{id}`, incluindo os dados completos do departamento vinculado. SHALL retornar HTTP 404 com `SCOS_POSITION_001` quando `{id}` não existir em `SCOS_POSITION`.

#### Scenario: Cargo encontrado
- **WHEN** `GET /v1/positions/10` é chamado e o cargo existe
- **THEN** retorna HTTP 200 com `data.id = 10` e `data.department` preenchido

#### Scenario: Cargo não encontrado
- **WHEN** `GET /v1/positions/999999` é chamado e o cargo não existe
- **THEN** retorna HTTP 404 com `codeError = "SCOS_POSITION_001"`

### Requirement: POST /v1/positions cria cargo vinculado a departamento ativo
O sistema SHALL criar um cargo quando `code` (único em `SCOS_POSITION`, ≤ 30 chars), `description` (≤ 30 chars) e `departmentId` forem válidos. SHALL retornar HTTP 400 se algum campo obrigatório estiver ausente ou vazio. SHALL retornar HTTP 409 com `SCOS_POSITION_002` se `code` já existir. SHALL retornar HTTP 404 com `SCOS_DEPARTMENT_001` se `departmentId` não existir. SHALL retornar HTTP 422 com `SCOS_DEPARTMENT_006` se o departamento existir mas estiver inativo. O campo opcional `isTrustPosition` (default `false`) é aceito sem validação adicional.

#### Scenario: Criação bem-sucedida
- **WHEN** `POST /v1/positions` é chamado com `code`, `description` e `departmentId` válidos, sendo o departamento ativo
- **THEN** retorna HTTP 201 com o `id` do cargo criado

#### Scenario: Campo obrigatório ausente
- **WHEN** `POST /v1/positions` é chamado sem `description`
- **THEN** retorna HTTP 400

#### Scenario: Code duplicado
- **WHEN** `POST /v1/positions` é chamado com `code` já existente em outro cargo
- **THEN** retorna HTTP 409 com `codeError = "SCOS_POSITION_002"`

#### Scenario: departmentId não encontrado
- **WHEN** `POST /v1/positions` é chamado com `departmentId` inexistente
- **THEN** retorna HTTP 404 com `codeError = "SCOS_DEPARTMENT_001"`

#### Scenario: departmentId inativo
- **WHEN** `POST /v1/positions` é chamado com `departmentId` de um departamento existente porém com `active = false`
- **THEN** retorna HTTP 422 com `codeError = "SCOS_DEPARTMENT_006"`

### Requirement: PUT /v1/positions/{id} atualiza cargo revalidando o departamento sempre
O sistema SHALL atualizar `code`, `description`, `departmentId` e `isTrustPosition` de um cargo existente. SHALL retornar HTTP 404 com `SCOS_POSITION_001` se `{id}` não existir. SHALL retornar HTTP 409 com `SCOS_POSITION_002` se o novo `code` colidir com outro cargo (excluindo o próprio `{id}`). SHALL revalidar o `departmentId` informado (existência e status ativo) em toda atualização, mesmo quando idêntico ao departamento atual do cargo, retornando `SCOS_DEPARTMENT_001`/`SCOS_DEPARTMENT_006` conforme aplicável.

#### Scenario: Atualização bem-sucedida
- **WHEN** `PUT /v1/positions/10` é chamado com todos os campos válidos e departamento ativo
- **THEN** retorna HTTP 204 e os dados do cargo são persistidos

#### Scenario: Cargo não encontrado
- **WHEN** `PUT /v1/positions/999999` é chamado
- **THEN** retorna HTTP 404 com `codeError = "SCOS_POSITION_001"`

#### Scenario: Code duplicado em outro cargo
- **WHEN** `PUT /v1/positions/10` é chamado com `code` já usado pelo cargo `11`
- **THEN** retorna HTTP 409 com `codeError = "SCOS_POSITION_002"`

#### Scenario: departmentId igual ao atual mas departamento foi desativado
- **WHEN** `PUT /v1/positions/10` é chamado enviando o mesmo `departmentId` já associado ao cargo, e esse departamento está com `active = false`
- **THEN** retorna HTTP 422 com `codeError = "SCOS_DEPARTMENT_006"`

#### Scenario: departmentId alterado para departamento inexistente
- **WHEN** `PUT /v1/positions/10` é chamado com `departmentId` que não existe
- **THEN** retorna HTTP 404 com `codeError = "SCOS_DEPARTMENT_001"`

### Requirement: PUT /v1/positions/{id}/enable reativa cargo inativo
O sistema SHALL alterar o cargo de `ACTIVE=false` para `ACTIVE=true`. SHALL retornar HTTP 404 com `SCOS_POSITION_001` se `{id}` não existir. SHALL retornar HTTP 422 com `SCOS_POSITION_004` se o cargo já estiver ativo.

#### Scenario: Ativação bem-sucedida
- **WHEN** `PUT /v1/positions/10/enable` é chamado e o cargo está `active = false`
- **THEN** retorna HTTP 204 e o cargo passa a `active = true`

#### Scenario: Cargo já ativo
- **WHEN** `PUT /v1/positions/10/enable` é chamado e o cargo já está `active = true`
- **THEN** retorna HTTP 422 com `codeError = "SCOS_POSITION_004"`

#### Scenario: Cargo não encontrado
- **WHEN** `PUT /v1/positions/999999/enable` é chamado
- **THEN** retorna HTTP 404 com `codeError = "SCOS_POSITION_001"`

### Requirement: PUT /v1/positions/{id}/disable inativa cargo sem funcionário ativo vinculado
O sistema SHALL alterar o cargo de `ACTIVE=true` para `ACTIVE=false`. SHALL retornar HTTP 404 com `SCOS_POSITION_001` se `{id}` não existir. SHALL retornar HTTP 422 com `SCOS_POSITION_003` se existir ao menos um `Employee` com `status = ACTIVE` vinculado ao cargo. SHALL retornar HTTP 422 com `SCOS_POSITION_005` se o cargo já estiver inativo. A checagem de funcionário ativo vinculado ocorre antes da checagem de "já inativo".

#### Scenario: Desativação bem-sucedida
- **WHEN** `PUT /v1/positions/10/disable` é chamado, o cargo está `active = true` e não há `Employee` com `status = ACTIVE` vinculado
- **THEN** retorna HTTP 204 e o cargo passa a `active = false`

#### Scenario: Funcionário ativo vinculado bloqueia desativação
- **WHEN** `PUT /v1/positions/10/disable` é chamado e existe `Employee` com `status = ACTIVE` e `position.id = 10`
- **THEN** retorna HTTP 422 com `codeError = "SCOS_POSITION_003"`

#### Scenario: Funcionário inativo vinculado não bloqueia
- **WHEN** `PUT /v1/positions/10/disable` é chamado e o único `Employee` vinculado ao cargo tem `status = INACTIVE`
- **THEN** retorna HTTP 204 e o cargo passa a `active = false`

#### Scenario: Cargo já inativo
- **WHEN** `PUT /v1/positions/10/disable` é chamado e o cargo já está `active = false` (e sem funcionário ativo vinculado)
- **THEN** retorna HTTP 422 com `codeError = "SCOS_POSITION_005"`

#### Scenario: Cargo não encontrado
- **WHEN** `PUT /v1/positions/999999/disable` é chamado
- **THEN** retorna HTTP 404 com `codeError = "SCOS_POSITION_001"`

### Requirement: Checagem pública de funcionário ativo vinculado ao cargo
O agregado Employee SHALL expor um método público (fora do pacote `employee.internal`) que retorna se existe algum `Employee` com `status = ACTIVE` vinculado a um `positionId`. Este método é a única forma pela qual o agregado Position pode consultar vínculos de Employee.

#### Scenario: Consulta retorna true com funcionário ativo
- **WHEN** o método público é chamado com um `positionId` que tem um `Employee` com `status = ACTIVE` vinculado
- **THEN** retorna `true`

#### Scenario: Consulta retorna false sem funcionário ativo
- **WHEN** o método público é chamado com um `positionId` sem nenhum `Employee` com `status = ACTIVE` vinculado (podendo haver vínculos `INACTIVE`/`DISABLED`)
- **THEN** retorna `false`
