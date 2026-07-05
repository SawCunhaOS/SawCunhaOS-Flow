# Mensagens de Erro — Português e Inglês

> Complementa os documentos de regras por endpoint (**00** a **06**). Catálogo final, pronto para os arquivos `scos_message_validation(.properties/_en.properties)` e `scos_message_organization(.properties/_en.properties)`. Onde o texto abaixo difere do que está hoje no código-fonte, há uma nota de uma linha explicando o motivo — não é histórico de decisão, é a razão técnica da mudança, necessária para quem for aplicar o patch.

---

## 1. Validação de Campo (`SCOS_VALIDATION_`)

| Código | Português | English | Nota |
| --- | --- | --- | --- |
| `SCOS_VALIDATION_001` | O campo ''{0}'' deve ser informado. | The field ''{0}'' must be provided. | — |
| `SCOS_VALIDATION_002` | O valor no campo ''{0}'' deve ser maior ou igual a {1}. | The value in the field ''{0}'' must be greater than or equal to {1}. | EN corrigido — "is below the specified" não é uma frase de erro completa em inglês; alinhado à estrutura afirmativa do PT |
| `SCOS_VALIDATION_003` | É obrigatório informar o ''{0}''. | It is mandatory to provide the ''{0}''. | — |
| `SCOS_VALIDATION_004` | O valor no campo ''{0}'' deve ser menor ou igual a {1}. | The value in the field ''{0}'' must be less than or equal to {1}. | Mesma correção estrutural do `SCOS_VALIDATION_002` |
| `SCOS_VALIDATION_005` | Deve ser informado um email válido. | A valid email must be provided. | — |
| `SCOS_VALIDATION_006` | O campo ''{0}'' deve ser maior ou igual a {1} e menor ou igual a {2}. | The field ''{0}'' must be greater than or equal to {1} and less than or equal to {2}. | ⚠️ PT corrigido — a ordem de `{1}`/`{2}` estava invertida em relação ao `SCOS_VALIDATION_008` (que usa `{1}`=mínimo, `{2}`=máximo de forma consistente). **Confirmar o binding real dos parâmetros no código antes de aplicar** — se `{1}`/`{2}` estiverem de fato amarrados na ordem antiga em algum lugar do código Java, corrigir lá também, não só aqui |
| `SCOS_VALIDATION_007` | O campo ''{0}'' deve ter o formato ''HH:mm:ss'' ou ''HH:mm''. | The field ''{0}'' must be in the format ''HH:mm:ss'' or ''HH:mm''. | — |
| `SCOS_VALIDATION_008` | É permitido informar no mínimo ''{1}'' e no máximo ''{2}'' no campo ''{0}''. | It is allowed to provide at least ''{1}'' and at most ''{2}'' in the field ''{0}''. | — |
| `SCOS_VALIDATION_009` | O CEP informado é inválido. | The provided ZIP code is invalid. | PT corrigido — "Informado" com maiúscula no meio da frase e "invalido" sem acento no original |
| `SCOS_VALIDATION_010` | O CNPJ informado é inválido. | The provided CNPJ is invalid. | EN criado — ausente no código |
| `SCOS_VALIDATION_011` | O CPF informado é inválido. | The provided CPF is invalid. | EN criado — ausente no código |
| `SCOS_VALIDATION_012` | O documento informado é inválido. | The provided document is invalid. | EN criado — ausente no código |

**Placeholders:** `{0}` é sempre o nome do campo; `{1}`/`{2}` são limites (mínimo/máximo), preenchidos dinamicamente pelo framework de validação.

---

## 2. Regras de Negócio (`SCOS_<MÓDULO>_<NNN>`)

### 2.1. Departamento

| Código | Português                                                                       | English | Nota |
| --- |---------------------------------------------------------------------------------| --- | --- |
| `SCOS_DEPARTMENT_001` | O departamento informado não existe.                                            | The informed department does not exist. | — |
| `SCOS_DEPARTMENT_002` | Já existe um departamento cadastrado com esse código.                           | There is already a department registered with this code. | — |
| `SCOS_DEPARTMENT_003` | Não foi possível inativar o departamento, pois ele possui vínculos com posição. | It was not possible to delete the department because it has links with positions. | — |
| `SCOS_DEPARTMENT_004` | O departamento informado já está ativo.                                         | The informed department is already active. | Criado — não existia em nenhum idioma |
| `SCOS_DEPARTMENT_005` | O departamento informado já está inativo.                                       | The informed department is already inactive. | Criado |
| `SCOS_DEPARTMENT_006` | Não é possível associar o cargo a um departamento inativo.                      | It is not possible to associate the position with an inactive department. | Criado |

### 2.2. Cargo

| Código | Português | English | Nota |
| --- | --- | --- | --- |
| `SCOS_POSITION_001` | O cargo informado não existe. | The informed position does not exist. | — |
| `SCOS_POSITION_002` | Já existe um cargo cadastrado com esse código. | There is already a position registered with this code. | — |
| `SCOS_POSITION_003` | Não foi possível excluir o cargo, pois ele possui vínculos com empregados. | It was not possible to delete the position because it has links with employees. | — |
| `SCOS_POSITION_004` | O cargo informado já está ativo. | The informed position is already active. | Criado |
| `SCOS_POSITION_005` | O cargo informado já está inativo. | The informed position is already inactive. | Criado |

### 2.3. Empresa

| Código | Português | English | Nota |
| --- | --- | --- | --- |
| `SCOS_COMPANY_001` | A empresa informada não existe. | The informed company does not exist. | — |
| `SCOS_COMPANY_002` | Já existe uma empresa cadastrada com esse CNPJ. | There is already a company registered with this CNPJ. | — |
| `SCOS_COMPANY_003` | Não é possível bloquear a empresa, pois ela possui colaboradores vinculados. | It is not possible to block the company because it has linked employees. | PT/EN revisados — texto original usava "excluir" e citava "departamentos" (Departamento não tem vínculo de empresa no schema; ver Documento 00, Seção 3.1). Ajustado para a operação real (`block`) e à referência de vínculo correta |
| `SCOS_COMPANY_004` | Operação não permitida: foi detectado um ciclo na hierarquia de empresas. | Operation not allowed: a cycle was detected in the company hierarchy. | — |
| `SCOS_COMPANY_005` | Não é permitido inativar a última empresa matriz ativa do sistema. | It is not allowed to inactivate the last active parent company in the system. | — |
| `SCOS_COMPANY_006` | Não é permitido bloquear a única empresa ativa do sistema. | It is not allowed to block the only active company in the system. | PT/EN revisados — "excluir" trocado por "bloquear" para refletir a operação real (não há exclusão física ou lógica irreversível para Company) |
| `SCOS_COMPANY_007` | Transição de status inválida para a empresa informada. | Invalid status transition requested for the informed company. | — |

### 2.4. Configuração

| Código | Português | English | Nota |
| --- | --- | --- | --- |
| `SCOS_CONFIGURATION_001` | Nenhuma configuração foi encontrada para o identificador informado. | No configuration was found for the informed identifier. | Criado — HTTP 400 (não 404, ver Documento 00 Seção 2.2) |
| `SCOS_CONFIGURATION_002` | O valor informado é incompatível com o tipo de dado declarado para esta configuração. | The informed value is incompatible with the data type declared for this configuration. | Criado — HTTP 400 (não 422) |

### 2.5. Funcionário

| Código | Português | English | Nota |
| --- | --- | --- | --- |
| `SCOS_EMPLOYEE_001` | Transição de status inválida para o funcionário informado. | Invalid status transition requested for the informed employee. | — |

### 2.6. Usuário / Sincronização Keycloak

| Código | Português | English | Nota |
| --- | --- | --- | --- |
| `SCOS_USER_001` | Ocorreu um erro ao tentar cadastrar o usuário no Keycloak. | An error occurred while trying to register the user in Keycloak. | PT com pontuação final adicionada, minúscula em "usuário" (não é início de entidade própria) |
| `SCOS_USER_002` | Ocorreu um erro ao tentar atualizar o usuário no Keycloak. | An error occurred while trying to update the user in Keycloak. | Idem |
| `SCOS_USER_003` | Ocorreu um erro ao tentar excluir o usuário no Keycloak. | An error occurred while trying to delete the user in Keycloak. | PT: "apagar" trocado por "excluir", consistente com o verbo usado no resto do catálogo |
| `SCOS_USER_004` | Erro desconhecido ao processar o usuário. | Unknown error while processing the user. | Artigo adicionado para consistência |

### 2.7. Autoridade e Login

| Código | Português | English | Nota |
| --- | --- | --- | --- |
| `SCOS_AUTHORITY_001` | O Login informado não existe no sistema. | The informed Login does not exist in the system. | EN criado — ausente no código |
| `SCOS_LOGIN_001` | *(sem Javadoc, sem uso identificado no código — não é possível propor texto com segurança)* | *(idem)* | Não criado — sem informação suficiente sobre o cenário pretendido; reportar ao time antes de escrever |
| `SCOS_LOGIN_002` | *(idem)* | *(idem)* | Idem |
| `SCOS_LOGIN_003` | *(idem)* | *(idem)* | Idem |
| `SCOS_LOGIN_010` | O Login informado está inativo no sistema. | The informed Login is inactive in the system. | EN criado |
| `SCOS_LOGIN_011` | O Login informado está bloqueado no sistema. | The informed Login is blocked in the system. | EN criado |
| `SCOS_LOGIN_013` | Transição de status inválida para o Login informado. | Invalid status transition requested for the informed Login. | Criado — não existia em nenhum idioma |

---

## 3. Resumo do que Precisa ser Aplicado no Código

| Ação | Códigos |
| --- | --- |
| Adicionar entrada nova (PT + EN) | `SCOS_DEPARTMENT_004/005/006`, `SCOS_POSITION_004/005`, `SCOS_CONFIGURATION_001/002`, `SCOS_LOGIN_013` |
| Adicionar só EN (PT já existe) | `SCOS_VALIDATION_010/011/012`, `SCOS_AUTHORITY_001`, `SCOS_LOGIN_010/011` |
| Corrigir texto existente (PT e/ou EN) | `SCOS_VALIDATION_002`, `SCOS_VALIDATION_004`, `SCOS_VALIDATION_006` (mais confirmação de binding), `SCOS_VALIDATION_009`, `SCOS_COMPANY_003/006`, `SCOS_USER_001-004` |
| Sem informação suficiente para propor | `SCOS_LOGIN_001/002/003` — investigar com o time antes |
