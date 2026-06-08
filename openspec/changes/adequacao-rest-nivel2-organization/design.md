## Context

O módulo Organization possui 4 arquivos de spec OpenAPI (`Company`, `Employee`, `Login`, `Department-Position`) que acumularam inconsistências de design ao longo do desenvolvimento:

- URIs de coleções misturadas entre singular (`/v1/company`, `/v1/employee`) e plural (`/v1/departments`, `/v1/positions`)
- Endpoint `DELETE /v1/profile/{id}/features` com `requestBody`, violando RFC 9110 §9.3.5
- Schemas de Profile e Login usando sufixo `*DTO` enquanto Company/Employee usam `*Request`/`*Response`
- `LoginStatus.ENABLE` é verbo (infinitivo), inconsistente com os demais valores do enum (`BLOCKED`, `INACTIVE`, `PENDING`)
- `GET /v1/employee/login/info` cria risco de colisão de path com `GET /v1/employee/{id}`
- Department e Position sem endpoints de toggle de status, diferente de Company e Employee

As specs são o contrato da API. A implementação Java segue as specs; portanto, a correção começa nos arquivos YAML e se propaga para os controllers.

## Goals / Non-Goals

**Goals:**
- Padronizar todas as URIs de coleções para plural
- Eliminar a violação RFC 9110 §9.3.5 (DELETE com body)
- Uniformizar sufixo de schemas para `*Request`/`*Response`
- Corrigir `LoginStatus.ENABLE` → `ENABLED`
- Mover endpoint de informação de login para path sem risco de colisão
- Adicionar toggle de status (enable/disable) a Department e Position

**Non-Goals:**
- HATEOAS / RMM Nível 3
- Mudanças no schema de banco de dados (escopo de change separado)
- Novas funcionalidades de negócio
- Endpoints de Company Document (atualmente comentados)
- Padronização de operationIds duplicados (escopo de `correcao-erros-specs-openapi-organization`)

## Decisions

### D-01: URIs de coleções — plural em tudo
**Decisão**: Plural universal, incluindo sub-recursos.

**Rationale**: `departments` e `positions` já usam plural; adotar singular universalmente implicaria reverter esses dois, aumentando o impacto. Plural é o padrão REST dominante (GitHub, Stripe, Google APIs).

**Alternativa rejeitada**: Singular universal — reverteria o que já está correto e vai contra a convenção REST amplamente adotada.

**Mapeamento completo**:
```
/v1/company                              → /v1/companies
/v1/company/{id}                         → /v1/companies/{id}
/v1/company/{id}/enable                  → /v1/companies/{id}/enable
/v1/company/{id}/disable                 → /v1/companies/{id}/disable
/v1/company/{companyId}/contact          → /v1/companies/{companyId}/contacts
/v1/company/{companyId}/contact/{id}     → /v1/companies/{companyId}/contacts/{id}
/v1/company/{companyId}/address          → /v1/companies/{companyId}/addresses
/v1/company/{companyId}/address/{id}     → /v1/companies/{companyId}/addresses/{id}

/v1/employee                             → /v1/employees
/v1/employee/{id}                        → /v1/employees/{id}
/v1/employee/{id}/enable                 → /v1/employees/{id}/enable
/v1/employee/{id}/disable                → /v1/employees/{id}/disable
/v1/employee/{employeeId}/contact        → /v1/employees/{employeeId}/contacts
/v1/employee/{employeeId}/contact/{id}   → /v1/employees/{employeeId}/contacts/{id}
/v1/employee/{employeeId}/address        → /v1/employees/{employeeId}/addresses
/v1/employee/{employeeId}/address/{id}   → /v1/employees/{employeeId}/addresses/{id}
/v1/employee/{employeeId}/login          → /v1/employees/{employeeId}/logins
/v1/employee/{employeeId}/login/{id}     → /v1/employees/{employeeId}/logins/{id}
/v1/employee/{employeeId}/login/{id}/block     → /v1/employees/{employeeId}/logins/{id}/block
/v1/employee/{employeeId}/login/{id}/unblock   → /v1/employees/{employeeId}/logins/{id}/unblock
/v1/employee/{employeeId}/login/{id}/password  → /v1/employees/{employeeId}/logins/{id}/password
/v1/employee/{employeeId}/login/{id}/profile/{profileId} → /v1/employees/{employeeId}/logins/{id}/profile/{profileId}
/v1/employee/login/info                  → /v1/logins/me

/v1/profile                              → /v1/profiles
/v1/profile/{id}                         → /v1/profiles/{id}
/v1/profile/{id}/features                → /v1/profiles/{id}/features
```

### D-02: Transição de estado — PUT sub-resource sem body
**Decisão**: Manter e padronizar o padrão atual de sub-resource com PUT e sem body.

**Rationale**: Idempotente (chamar `/enable` N vezes = mesmo resultado), URI explícita e legível, nenhum body necessário (a ação é implícita na URI), e é o padrão adotado por GitHub, Stripe e Google APIs para ações de estado nomeadas.

**Alternativa rejeitada**: PATCH no recurso raiz com `{"status": "ACTIVE"}` — exige que o client conheça os valores válidos do enum e que todos os recursos suportem PATCH, aumentando a superfície de mudança.

### D-03: DELETE /profiles/{id}/features — remover endpoint
**Decisão**: Remover `DELETE /v1/profiles/{id}/features` com body. Usar o `PUT /v1/profiles/{id}/features` existente para substituição total da lista.

**Rationale**: RFC 9110 §9.3.5 proíbe body em DELETE. O PUT já existe e tem semântica de substituição completa: enviar lista vazia = remover todas; enviar lista parcial = manter apenas as passadas.

**Alternativa rejeitada**: `DELETE /v1/profiles/{id}/features/{featureCode}` — remove uma feature por vez, tornando a remoção de N features em N chamadas.

### D-04: LoginStatus.ENABLE → ENABLED
**Decisão**: Renomear para `ENABLED` (past participle, representa estado).

**Rationale**: `BLOCKED`, `INACTIVE`, `PENDING` são todos adjetivos/past participles representando estados; `ENABLE` é verbo no infinitivo, inconsistente. `ACTIVE` (alternativa) foi descartado para preservar distinção semântica entre `LoginStatus` e `StatusCompany`.

**Risco**: Se o valor `ENABLE` já está gravado em banco (`SCOS_LOGIN.STATUS`), é necessária migration Liquibase antes do deploy.

### D-05: /v1/logins/me — endpoint singleton de sessão
**Decisão**: Mover `GET /v1/employee/login/info` para `GET /v1/logins/me`.

**Rationale**: O endpoint retorna dados do usuário autenticado via JWT — não pertence à hierarquia `employee/{id}` e cria ambiguidade de path com `GET /v1/employee/{id}` onde `{id}="login"`. O padrão `me` é amplamente estabelecido (GitHub `/user`, Auth0 `/userinfo`).

**Implementação**: Controller extrai `employeeId` / `loginId` do JWT, não de path parameter.

## Risks / Trade-offs

| Risco | Mitigação |
|---|---|
| Breaking change massivo em URIs — qualquer cliente atual quebra | Verificar consumidores antes do deploy; documentar no CHANGELOG; considerar período de deprecação com ambas as URIs |
| `LoginStatus.ENABLE` persistido no banco | Antes de alterar o enum Java, verificar `SELECT DISTINCT STATUS FROM SCOS_LOGIN`; se houver dados, criar Liquibase migration `UPDATE SCOS_LOGIN SET STATUS='ENABLED' WHERE STATUS='ENABLE'` |
| Department/Position sem `active` no domain model | Verificar se entidades Java já têm campo `active`; se não, checar pendência na change de domain model (`adequacao-liquibase-domain-model`) |

## Migration Plan

1. Atualizar specs YAML (breaking change — incrementar versão da API se existir versionamento)
2. Atualizar `@RequestMapping` nos controllers Java para as novas URIs
3. Verificar e migrar `LoginStatus.ENABLE` → `ENABLED` no banco se necessário
4. Adicionar endpoints `enable`/`disable` em `DepartmentController` e `PositionController`
5. Migrar schemas de `*DTO` para `*Request`/`*Response` no Login.yml e corrigir refs quebradas
6. Remover `deleteFeaturesInProfile` do controller e do spec
7. Criar controller method `GET /v1/logins/me` extraindo identity do JWT
