# Adequação REST Nível 2 e Nomenclatura — APIs do Módulo Organization

**Data**: 2026-06-08  
**Status**: 🔄 Em Análise  
**Tipo**: 🔧 Refatoração

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

- **Nome da funcionalidade**: `adequacao-rest-nivel2-organization`
- **Resumo em uma frase**: Corrigir violações do Richardson Maturity Model Nível 2 e do RFC 9110 nas specs OpenAPI do módulo Organization, e padronizar nomenclatura de URIs e schemas entre todos os recursos.

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade
- [x] Não mistura features independentes no mesmo arquivo
- [x] O nome é específico (não genérico como "melhorar módulo X")

---

## 1️⃣ Visão

### Problema

As specs OpenAPI do módulo Organization apresentam múltiplas categorias de violações de design que comprometem consistência, aderência ao RFC 9110 e o Richardson Maturity Model (RMM) Nível 2:

**P1. Verbos de ação na URI (RPC-style)**
Endpoints de transição de estado usam sub-resources com verbos ao invés de operar sobre o estado do recurso:
```
PUT /v1/company/{id}/enable     ← verbo na URI
PUT /v1/company/{id}/disable    ← verbo na URI
PUT /v1/employee/{id}/enable
PUT /v1/employee/{id}/disable
PUT /v1/employee/{employeeId}/login/{id}/block
PUT /v1/employee/{employeeId}/login/{id}/unblock
```

**P2. DELETE com requestBody**
`DELETE /v1/profile/{id}/features` usa `requestBody` — prática desencorajada pelo RFC 9110 §9.3.5 ("A client SHOULD NOT generate content in a DELETE request") e incompatível com vários proxies/gateways HTTP.

**P3. Inconsistência de enable/disable entre recursos**
Company e Employee têm `/enable` e `/disable`. Department, Position e Profile não têm. Sem definição clara de qual critério diferencia recursos com e sem toggle de status.

**P4. Inconsistência plural/singular nas URIs de coleções — violação de convenção REST**
Coleções devem usar substantivos no plural (RFC 9110 não prescreve, mas é convenção universalmente adotada). Situação atual:
```
/v1/company          ← singular ❌  →  deve ser /v1/companies
/v1/employee         ← singular ❌  →  deve ser /v1/employees
/v1/profile          ← singular ❌  →  deve ser /v1/profiles

Sub-recursos (também coleções):
/company/{id}/contact    ← singular ❌  →  /companies/{id}/contacts
/company/{id}/address    ← singular ❌  →  /companies/{id}/addresses
/employee/{id}/contact   ← singular ❌  →  /employees/{id}/contacts
/employee/{id}/address   ← singular ❌  →  /employees/{id}/addresses
/employee/{id}/login     ← singular ❌  →  /employees/{id}/logins

Já corretos:
/v1/departments  ✅
/v1/positions    ✅
/v1/features     ✅
```

**P5. Inconsistência de sufixo nos schemas**
Schemas de request/response sem padrão uniforme entre os arquivos:
```
Company.yml / Employee.yml / Department-Position.yml:
  *Request  (CreateCompanyRequest, UpdateCompanyRequest) ✅
  *Response (GetCompanyResponse, GetAllCompaniesResponse) ✅

Login.yml (Profile e Login):
  *DTO (CreateProfileDTO, UpdateProfileDTO) ❌
  *DTO (CreatedEmployeeLoginDTO, UpdateEmployeeLoginDTO) ❌
  Mix: CreateEmployeeLoginRequest (ref) → CreatedEmployeeLoginDTO (definição) ← ref quebrada
```

**P6. Enum `LoginStatus.ENABLE` — valor incorreto**
```yaml
LoginStatus:
  enum:
    - ENABLE      ← verbo/infinitivo ❌ (outros valores são estados: BLOCKED, INACTIVE, PENDING)
    - BLOCKED     ✅
    - INACTIVE    ✅
```
Enum representa estado, não ação. `ENABLE` deve ser `ACTIVE` (consistente com `StatusCompany.ACTIVE`) ou `ENABLED`.

**P7. Risco de colisão de path — `/v1/employee/login/info`**
```
GET /v1/employee/{id}          ← {id} é path parameter
GET /v1/employee/login/info    ← "login" literal entra em conflito com {id}
```
Frameworks como Spring resolvem literal antes de parâmetro, mas o endpoint não pertence conceitualmente à hierarquia `employee/{id}`. Deve ser movido para `/v1/logins/me`.

### Objetivo

Specs OpenAPI do módulo Organization com design REST consistente e aderente ao RMM Nível 2 e RFC 9110: sem verbos na URI para transição de estado, sem DELETE+body, com URIs de coleções no plural, schemas com sufixo uniforme (*Request/*Response), enum de status correto, e endpoint de login info sem risco de colisão.

### Fora de Escopo

- Erros de copy-paste (schema refs, operationId duplicados) — cobertos em `20260608_correcao-erros-specs-openapi-organization.md`
- HATEOAS (RMM Nível 3) — decisão arquitetural separada, fora do objetivo atual
- Company Document (endpoints comentados) — feature separada
- RFC 9205 — não aplicável: esse RFC é para designers de protocolos que constroem novos protocolos sobre HTTP (ex: WebSocket, CoAP). Para APIs REST convencionais, o documento correto é o RFC 9110.

---

## 2️⃣ Requisitos

### Funcionais

- [ ] **RF-01**: Transições de estado (enable/disable, block/unblock) usam padrão sub-resource PUT — `PUT /v1/{recurso}/{id}/enable` e `PUT /v1/{recurso}/{id}/disable`. Idempotente, URI explícita, sem body. *(Decisão: Opção A)*
- [ ] **RF-02**: `DELETE /v1/profiles/{id}/features` com body removido — endpoint já existe como `PUT /v1/profiles/{id}/features` que substitui a lista completa de features (semântica de substituição total). Remover o DELETE. *(Decisão: Opção A)*
- [ ] **RF-03**: Política de toggle de status documentada e aplicada consistentemente: Department e Position devem ter `PUT /{id}/enable` e `PUT /{id}/disable` igual a Company e Employee
- [ ] **RF-04**: URIs de coleções padronizadas no plural — company→companies, employee→employees, profile→profiles, contact→contacts, address→addresses, login→logins
- [ ] **RF-05**: Schemas de Login e Profile migrados para sufixo *Request/*Response, alinhado com Company/Employee/Department/Position
- [ ] **RF-06**: `LoginStatus.ENABLE` corrigido para `ENABLED` e atualizado na implementação Java. *(Decisão: ENABLED)*
- [ ] **RF-07**: `GET /v1/employee/login/info` movido para `GET /v1/logins/me`

### Não-Funcionais

- [ ] **RNF-01**: Mudanças breaking de URI documentadas — qualquer renomeação de endpoint existente exige versionamento ou período de deprecação
- [ ] **RNF-02**: Implementação Java deve ser atualizada em sincronia com a spec
- [ ] **RNF-03**: Conformidade com RFC 9110 validada: DELETE sem body, PUT idempotente sobre o recurso-alvo

---

## 3️⃣ Arquitetura

### Decisão — Transição de Estado ✅ DECIDIDO

**Opção A escolhida** — Sub-resource PUT (padrão atual, mantido):
```
PUT /v1/companies/{id}/enable    → ativa
PUT /v1/companies/{id}/disable   → desativa
PUT /v1/employees/{id}/enable
PUT /v1/employees/{id}/disable
PUT /v1/employees/{employeeId}/logins/{id}/block
PUT /v1/employees/{employeeId}/logins/{id}/unblock
```
Idempotente, URI explícita, sem body. Mesmo padrão adotado por GitHub, Stripe, Google para ações nomeadas. Aceita a impureza RMM L2 em favor de expressividade.

### Decisão — DELETE com Body (`/profiles/{id}/features`) ✅ DECIDIDO

**Opção A escolhida** — Remover `DELETE /v1/profiles/{id}/features`:
```
PUT /v1/profiles/{id}/features   ← já existe, substitui lista completa
Body: {"features": [...lista final...]}
```
O `DELETE` com body é removido. O `PUT` existente cobre o caso de uso (quem quer remover todas: PUT com lista vazia; quem quer remover N: PUT com a lista sem os N). Sem novo endpoint necessário.

### Mapeamento Completo de Renomeações de URI

```
ATUAL                                    → PROPOSTO
─────────────────────────────────────────────────────────────────────
/v1/company                              → /v1/companies
/v1/company/{id}                         → /v1/companies/{id}
/v1/company/{id}/enable                  → /v1/companies/{id}/enable  (ou PATCH)
/v1/company/{id}/disable                 → /v1/companies/{id}/disable (ou PATCH)
/v1/company/{companyId}/contact          → /v1/companies/{companyId}/contacts
/v1/company/{companyId}/contact/{id}     → /v1/companies/{companyId}/contacts/{id}
/v1/company/{companyId}/address          → /v1/companies/{companyId}/addresses
/v1/company/{companyId}/address/{id}     → /v1/companies/{companyId}/addresses/{id}

/v1/employee                             → /v1/employees
/v1/employee/{id}                        → /v1/employees/{id}
/v1/employee/{id}/enable                 → /v1/employees/{id}/enable  (ou PATCH)
/v1/employee/{id}/disable                → /v1/employees/{id}/disable (ou PATCH)
/v1/employee/{employeeId}/contact        → /v1/employees/{employeeId}/contacts
/v1/employee/{employeeId}/contact/{id}   → /v1/employees/{employeeId}/contacts/{id}
/v1/employee/{employeeId}/address        → /v1/employees/{employeeId}/addresses
/v1/employee/{employeeId}/address/{id}   → /v1/employees/{employeeId}/addresses/{id}
/v1/employee/{employeeId}/login          → /v1/employees/{employeeId}/logins
/v1/employee/{employeeId}/login/{id}     → /v1/employees/{employeeId}/logins/{id}
/v1/employee/{employeeId}/login/{id}/block    → /v1/employees/{employeeId}/logins/{id}/block
/v1/employee/{employeeId}/login/{id}/unblock  → /v1/employees/{employeeId}/logins/{id}/unblock
/v1/employee/{employeeId}/login/{id}/password → /v1/employees/{employeeId}/logins/{id}/password
/v1/employee/{employeeId}/login/{id}/profile/{profileId} → /v1/employees/{employeeId}/logins/{id}/profile/{profileId}
/v1/employee/login/info                  → /v1/logins/me

/v1/profile                              → /v1/profiles
/v1/profile/{id}                         → /v1/profiles/{id}
/v1/profile/{id}/features                → /v1/profiles/{id}/features

Sem mudança:
/v1/departments, /v1/departments/{id}    ← já correto ✅
/v1/positions, /v1/positions/{id}        ← já correto ✅
/v1/features                             ← já correto ✅
/v1/logins/me                            ← novo endpoint (RF-07)
```

### Componentes Afetados

```
etc/api/organization/
├── ScosOrganization_Company.yml          → modificação (URIs + schemas se mudar enable/disable)
├── ScosOrganization_Employee.yml         → modificação (URIs + schemas)
├── ScosOrganization_Login.yml            → modificação (URIs + schemas *DTO→*Request/Response + DELETE features + LoginStatus.ENABLE + /logins/me)
└── ScosOrganization_Department-Position.yml → modificação (adicionar toggle status)

src/ (implementação Java)
└── todos os Controllers/endpoints afetados pelas renomeações de URI
```

### Banco de Dados

- **Impacto**: ❌ Não (mudança apenas na spec/contrato de API)

---

## 4️⃣ Implementação

### Tarefas

**Grupo A — Transição de estado e RFC 9110**
- [ ] **T-01**: Documentar decisão de transição de estado como ADR (sub-resource PUT escolhido)
- [ ] **T-02**: Garantir que endpoints enable/disable/block/unblock existentes usam PUT sem body — validar specs e implementação Java
- [ ] **T-03**: Remover `DELETE /v1/profiles/{id}/features` da spec e do controller Java — o `PUT` existente já cobre o caso de uso

**Grupo B — Toggle de status (bloqueado por T-01)**
- [ ] **T-04**: Adicionar endpoints de toggle de status em Department
- [ ] **T-05**: Adicionar endpoints de toggle de status em Position
- [ ] **T-06**: Validar schemas `Department` e `Position` para incluir campo `active` nas respostas

**Grupo C — Nomenclatura (independente, pode rodar em paralelo)**
- [ ] **T-07**: Renomear URIs de recursos raiz para plural — company→companies, employee→employees, profile→profiles — em todas as specs e implementação Java
- [ ] **T-08**: Renomear sub-recursos de coleções para plural — contact→contacts, address→addresses, login→logins — em todas as specs e implementação Java
- [ ] **T-09**: Migrar schemas de Login.yml de sufixo *DTO para *Request/*Response — alinhar com padrão dos demais arquivos
- [ ] **T-10**: Corrigir `LoginStatus.ENABLE` para `ENABLED` na spec e no enum Java — verificar se valor está persistido no banco e criar migration se necessário
- [ ] **T-11**: Mover `GET /v1/employee/login/info` para `GET /v1/logins/me` — atualizar spec e controller Java

### Riscos e Edge Cases

1. **Breaking change massivo**: renomear URIs de coleções (T-07/T-08) afeta todos os consumidores — avaliar se já há clientes antes de mudar; considerar período de deprecação com ambas as URIs ativas
2. **DELETE features em batch**: se optar por delete granular (Opção B no T-03), operação de "remover N features" vira N chamadas — considerar impacto de performance
3. **Toggle em Department/Position**: verificar se domain model já suporta `active` para Department/Position ou se precisa de migration (coberta em `20260606_adequacao-liquibase-domain-model.md`)
4. **LoginStatus breaking**: se `ENABLE` já está persistido no banco, alterar o enum requer migration de dados além da mudança de spec
5. **`/v1/logins/me` autenticação**: endpoint retorna dados do usuário autenticado via token JWT — garantir que o controller extrai o subject do token, não aceita `employeeId` por path

---

## 📎 Referências

- Exploração: maturidade REST + RFC 9110 + nomenclatura das specs OpenAPI do módulo Organization (2026-06-08)
- Ideia relacionada (bugs): `20260608_correcao-erros-specs-openapi-organization.md`
- RFC 9110 — HTTP Semantics (substitui RFC 7231; DELETE sem body §9.3.5, PUT idempotente §9.3.4)
- RFC 9205 — **não aplicável**: escopo é protocolo sobre HTTP, não APIs REST convencionais
- Richardson Maturity Model — Martin Fowler
