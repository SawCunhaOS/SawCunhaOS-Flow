## Context

As specs OpenAPI do módulo Organization (`Employee.yml`, `Login.yml`, `Department-Position.yml`) são a fonte de geração de código Java via openapi-generator. Erros de copy-paste introduzidos durante o desenvolvimento inicial causam referências de schema inexistentes, operationId incorretos e parâmetros de path inválidos. O módulo usa o padrão delegate (openapi-generator `spring` generator com `delegatePattern=true`).

Estado atual dos erros por arquivo:

| Arquivo | Erros |
|---------|-------|
| `Employee.yml` | 4 (rename schema, 3 refs erradas) + server URL |
| `Login.yml` | 3 (operationId, 2 params inválidos) + server URL |
| `Department-Position.yml` | server URL |

## Goals / Non-Goals

**Goals:**
- Specs OpenAPI com referências de schema 100% corretas e resolúveis
- `operationId`, `summary` e `description` coerentes com a operação real
- Parâmetros de path declarados apenas quando existem na URI do endpoint
- `servers.url` uniforme entre todos os arquivos do módulo
- Padrão de nomenclatura `*Request` para todos os schemas de entrada (DTOs de input)

**Non-Goals:**
- Mudanças de design REST (verbos na URI, enable/disable, DELETE+body) — escopo de `adequacao-rest-nivel2-organization`
- Novos endpoints ou novos schemas
- Mudanças em código Java (geração será consequência automática da correção da spec)

## Decisions

### D-01: Renomear `UpdateEmployeeDTO` → `UpdateEmployeeRequest`

O projeto usa o sufixo `*Request` para DTOs de entrada (ex: `UpdateCompanyRequest`, `UpdateDepartmentRequest`, `UpdatePositionRequest`) e `*DTO` para objetos de resposta intermediários. `UpdateEmployeeDTO` é um DTO de entrada e deve seguir o padrão.

Impacto: renomear a definição do schema E a referência em `updateEmployee` requestBody (linha 117).

### D-02: `GET /v1/employee/login/info` — remover params de path inválidos

Os params `idEmployee` e `idRequest` foram declarados por copy-paste, mas a URI `/v1/employee/login/info` não tem variáveis de path. O endpoint busca dados do contexto do usuário autenticado (JWT), não por ID explícito. Como não há implementação Java de Employee/Login ainda, a remoção é segura e não causa breaking change.

### D-03: Correções são only-docs — sem impacto em contrato HTTP

Todas as correções são ao nível de documentação/spec (nomes de schema, operationId, parâmetros declarados). O comportamento HTTP dos endpoints (URI, método, status code) não muda.

## Risks / Trade-offs

- **Regeneração de DTOs**: Após corrigir as specs, o openapi-generator irá gerar `UpdateEmployeeRequest.java` (em vez de `UpdateEmployeeDTO.java`) e remover o DTO inexistente `UpdateEmployeeContactDTO`. Qualquer código Java que já referencie esses nomes (improvável — não há controllers de Employee/Login) precisaria ser atualizado. → Mitigação: verificado que não existem controllers Java de Employee/Login no módulo `scos-organization-api/src`.
- **`servers.url` sem `/`**: OpenAPI 3.x trata `organization/api` como path relativo e `/organization/api` como path absoluto. O comportamento real depende do runtime (Spring Boot ignora `servers` na geração delegate). Risco mínimo, mas uniformidade reduz confusão. → Mitigação: adotar `/organization/api` seguindo Company.yml.
