# Implementação do CRUD de Catálogo — AddressType e ContactType

**Data**: 2026-07-05
**Status**: 🔄 Em Análise
**Tipo**: 🆕 Nova Feature

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

- **Nome da funcionalidade**: `crud-catalogo-tipo-endereco-contato`
- **Resumo em uma frase**: Implementar a camada de código (domain/usecase/api/permissão) dos 12 endpoints de catálogo `AddressType`/`ContactType` cujo contrato OpenAPI já existe.

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade
- [x] Não mistura features independentes no mesmo arquivo
- [x] O nome é específico (não genérico como "melhorar módulo X")

> `AddressType` e `ContactType` são tratados numa única ideia (não 2) porque: (1) já foram tratados juntos como par no change `catalogo-tipo-endereco-contato` que fechou o contrato; (2) já vivem no mesmo pacote de domínio (`corporate/catalog/internal/`); (3) são o mesmo padrão de CRUD gerado a partir do mesmo par de decisões técnicas — não existe cenário onde um seria implementado sem o outro.

---

## 1️⃣ Visão

### Problema

O contrato OpenAPI do catálogo de referência `AddressType`/`ContactType` já foi fechado no change `catalogo-tipo-endereco-contato` (completo, `etc/api/organization/ScosOrganization_Catalog.yml`), substituindo o campo livre `type: string` de `CompanyAddress`/`EmployeeAddress`/`CompanyContact`/`EmployeeContact` por FK (`addressTypeId`/`contactTypeId`). Esse mesmo change documentou explicitamente que a implementação de código ficaria para uma change futura.

Hoje existe apenas a entidade JPA e o repositório vazio:
```
flow-organization-domain/.../corporate/catalog/internal/AddressType.java
flow-organization-domain/.../corporate/catalog/internal/AddressTypeRepository.java   (sem métodos custom)
flow-organization-domain/.../corporate/catalog/internal/ContactType.java
flow-organization-domain/.../corporate/catalog/internal/ContactTypeRepository.java   (sem métodos custom)
```
Não existe `dto/`, `service/`, `specification/` no domain, nenhum Use Case, nenhum `Delegate` (`delegate/` não tem pasta pra catálogo), e `ScosOrganizationPermission` não tem nenhuma entrada `*_ADDRESS_TYPE`/`*_CONTACT_TYPE`. Os 12 endpoints do contrato (UC-081 a UC-092, doc `05-catalogo-motivos.md`) não funcionam.

### Objetivo

Implementar os 12 endpoints (6 por entidade: `GET` lista paginada, `POST`, `GET /{id}`, `PUT /{id}`, `PUT /{id}/enable`, `PUT /{id}/disable`) seguindo exatamente o padrão já usado por `DepartmentService`/`PositionService` (referência mais recente: change `crud-cargo-position`).

### Fora de Escopo

- Migração dos Use Cases de `CompanyAddress`/`EmployeeAddress`/`CompanyContact`/`EmployeeContact` para consumirem `addressTypeId`/`contactTypeId` (já são FK no JPA hoje — validar se os Use Cases desses agregados já resolvem a entidade referenciada corretamente é outra frente, não deste CRUD de catálogo)
- Motivos de transição de status (`ReasonActivate/Inactivate/Disable/Enable`) — ideia separada: `crud-catalogo-motivos-transicao-status`
- Testes de integração com Testcontainers (não necessário nesta etapa, por instrução explícita)

---

## 2️⃣ Requisitos

### Funcionais
- [ ] **RF-01**: `GET /v1/address-types` — lista paginada, filtro opcional `entityType` (UC-081)
- [ ] **RF-02**: `POST /v1/address-types` — cria com `code` (único em `SCOS_ADDRESS_TYPE`, ≤30), `description` (≤255), `entityType` (`COMPANY`/`EMPLOYEE`) (UC-082)
- [ ] **RF-03**: `GET /v1/address-types/{id}` — 404 se não existir (UC-083)
- [ ] **RF-04**: `PUT /v1/address-types/{id}` — `code` único excluindo `{id}` (UC-084)
- [ ] **RF-05**: `PUT /v1/address-types/{id}/enable` — `ACTIVE=false→true`, 422 se já ativo (UC-085)
- [ ] **RF-06**: `PUT /v1/address-types/{id}/disable` — `ACTIVE=true→false`, 422 se já inativo; não remove vínculos existentes (UC-086)
- [ ] **RF-07**: RF-01 a RF-06 espelhados para `ContactType` em `/v1/contact-types` (UC-087 a UC-092)
- [ ] **RF-08**: Novas entradas em `ScosOrganizationPermission`: `GET/CREATE/UPDATE_ADDRESS_TYPE`, `ENABLE/DISABLE_ADDRESS_TYPE`, mesmo conjunto para `CONTACT_TYPE` (nomes já fixados em `x-authorize` do contrato)

### Não-Funcionais
- [ ] **RNF-01**: Mensagens de erro via `ScosException`/bundle i18n, seguindo padrão `SCOS_VALIDATION_*` já usado por Department/Position
- [ ] **RNF-02**: Sem `DELETE` — catálogo é append-only, inativação lógica apenas (mesma regra do banco: trigger bloqueia `DELETE` físico)

---

## 3️⃣ Arquitetura

### Componentes Afetados
```
flow-organization-domain
├── corporate/catalog/dto/AddressTypeInput.java, AddressTypeOutput.java: adição
├── corporate/catalog/dto/ContactTypeInput.java, ContactTypeOutput.java: adição
├── corporate/catalog/internal/AddressTypeRepository.java: modificação (existsByCode/existsByCodeAndNotId via QueryDSL default methods)
├── corporate/catalog/internal/ContactTypeRepository.java: modificação (idem)
├── corporate/catalog/service/AddressTypeMapper.java, AddressTypeServiceBean.java: adição
├── corporate/catalog/service/ContactTypeMapper.java, ContactTypeServiceBean.java: adição
├── corporate/catalog/specification/AddressTypeService.java: adição
└── corporate/catalog/specification/ContactTypeService.java: adição

flow-organization-usecase
└── application/usecase/corporate/catalog/
    ├── addresstype/ (Create/Update/Find/FindAll/Enable/DisableAddressTypeUseCase + Bean, AddressTypeApiMapper): adição
    └── contacttype/ (idem, ContactType): adição

flow-organization-api
└── delegate/catalog/
    ├── AddressTypeDelegate.java implements AddressTypeApiDelegate: adição
    └── ContactTypeDelegate.java implements ContactTypeApiDelegate: adição

flow-organization-infrastructure
└── enumaration/ScosOrganizationPermission.java: modificação (+12 entradas)
```

### Fluxo Principal
```
Delegate → UseCase → {AddressType|ContactType}Service (specification)
   → ServiceBean valida unicidade de code (existsByCode/existsByCodeAndNotId)
   → Mapper (dto ↔ entity) → Repository (JPA)
UseCase → ApiMapper (domain dto ↔ OpenAPI generated dto) → Delegate → response
```

### Decisões Técnicas
| Decisão | Escolha | Alternativa Descartada | Motivo |
|---------|---------|------------------------|--------|
| Pacote das 2 entidades | `corporate/catalog/{dto,service,specification}` compartilhado, classes por entidade | Pacote por entidade (`corporate/catalog/addresstype/`, `.../contacttype/`) ao estilo Department/Position | `internal/` já bundla as 2 entidades juntas hoje — manter a mesma granularidade evita split artificial de um par que sempre muda junto. Ajustável no `design.md` do change se o time preferir o padrão 1-pacote-por-agregado |
| Unicidade de `code` | Escopo por entidade (`SCOS_ADDRESS_TYPE` e `SCOS_CONTACT_TYPE` são catálogos independentes) | Unicidade cross-catálogo | Doc 05 e schema físico não relacionam os 2 catálogos — são tabelas distintas |
| `entityType` no filtro de listagem | Query param opcional, mesmo padrão do `active` em `GET /v1/departments` | Filtro obrigatório | Contrato já define como opcional (`entityType` não está em `paginationFilter` obrigatório) |

### Banco de Dados
- **Impacto**: ❌ Não — tabelas `SCOS_ADDRESS_TYPE`/`SCOS_CONTACT_TYPE` já existem (schema v2, change `adequacao-liquibase-domain-model-v2`, completo)

### Mensagens de Validação (código + HTTP + título + PT-BR + EN)

> Padrão do projeto — **já implementado** pela change `padronizacao-http-status-exception-code` (`ExceptionCodeError.java` hoje tem construtor de 3 argumentos: `code`, `httpCode`, `title`). Cada constante nova aqui segue o mesmo padrão: `code` em `ExceptionCodeError`, `httpCode` (int), `title` (chave de uma das 7 categorias já existentes — `SCOS_TITLE_NOT_FOUND`/`SCOS_TITLE_CONFLICT`/`SCOS_TITLE_BUSINESS_RULE_VIOLATION`/etc., **nenhuma categoria nova a criar**), e o texto de `detail` (pt-br/en) em `scos_message_organization.properties`/`_en.properties`. Sem código "003 em uso" (Department/Position têm; aqui não — doc 05 diz explicitamente que inativar não bloqueia por vínculo existente).

| Código | HTTP | Title | PT-BR (detail) | EN (detail) |
|---|---|---|---|---|
| `SCOS_ADDRESS_TYPE_001` | 404 | `SCOS_TITLE_NOT_FOUND` | O tipo de endereço informado não existe. | The informed address type does not exist. |
| `SCOS_ADDRESS_TYPE_002` | 409 | `SCOS_TITLE_CONFLICT` | Já existe um tipo de endereço cadastrado com esse código. | There is already an address type registered with this code. |
| `SCOS_ADDRESS_TYPE_003` | 422 | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | O tipo de endereço informado já está ativo. | The informed address type is already active. |
| `SCOS_ADDRESS_TYPE_004` | 422 | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | O tipo de endereço informado já está inativo. | The informed address type is already inactive. |
| `SCOS_CONTACT_TYPE_001` | 404 | `SCOS_TITLE_NOT_FOUND` | O tipo de contato informado não existe. | The informed contact type does not exist. |
| `SCOS_CONTACT_TYPE_002` | 409 | `SCOS_TITLE_CONFLICT` | Já existe um tipo de contato cadastrado com esse código. | There is already a contact type registered with this code. |
| `SCOS_CONTACT_TYPE_003` | 422 | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | O tipo de contato informado já está ativo. | The informed contact type is already active. |
| `SCOS_CONTACT_TYPE_004` | 422 | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | O tipo de contato informado já está inativo. | The informed contact type is already inactive. |

> Validação de campo obrigatório/`entityType` enum inválido (400) fica a cargo do `x-required-message`/`x-empty-message` já definidos no contrato OpenAPI (`SCOS_VALIDATION_*` genérico), não precisa de código novo aqui.

---

## 4️⃣ Implementação

### Arquivos

**Novos**:
- `corporate/catalog/dto/AddressTypeInput.java`, `AddressTypeOutput.java`, `ContactTypeInput.java`, `ContactTypeOutput.java`
- `corporate/catalog/service/AddressTypeMapper.java`, `AddressTypeServiceBean.java`, `ContactTypeMapper.java`, `ContactTypeServiceBean.java`
- `corporate/catalog/specification/AddressTypeService.java`, `ContactTypeService.java`
- `usecase/corporate/catalog/addresstype/*UseCase[Bean].java` (6), `AddressTypeApiMapper.java`
- `usecase/corporate/catalog/contacttype/*UseCase[Bean].java` (6), `ContactTypeApiMapper.java`
- `api/delegate/catalog/AddressTypeDelegate.java`, `ContactTypeDelegate.java`
- Testes unitários: `AddressTypeServiceBeanTest.java`, `ContactTypeServiceBeanTest.java` (Mockito, sem Testcontainers)

**Modificados**:
- `corporate/catalog/internal/AddressTypeRepository.java`, `ContactTypeRepository.java` — `existsByCode`/`existsByCodeAndNotId`
- `ScosOrganizationPermission.java` — +12 entradas
- `ExceptionCodeError.java` — +8 constantes (`SCOS_ADDRESS_TYPE_001..004`, `SCOS_CONTACT_TYPE_001..004`), cada uma já com os 3 argumentos do construtor atual (`code`, `httpCode`, `title` — reaproveitando as 7 categorias de `SCOS_TITLE_*` já existentes, nenhuma nova)
- `scos_message_organization.properties` / `scos_message_organization_en.properties` — +8 mensagens de `detail` cada (pt-br/en); nenhuma chave `SCOS_TITLE_*` nova necessária

### Tarefas
- [ ] **T-01**: Domain — dto/service/specification `AddressType` + `existsByCode*` no repo
- [ ] **T-02**: Domain — dto/service/specification `ContactType` + `existsByCode*` no repo
- [ ] **T-03**: Usecase — 6 Use Cases + Bean + ApiMapper `AddressType`
- [ ] **T-04**: Usecase — 6 Use Cases + Bean + ApiMapper `ContactType`
- [ ] **T-05**: Api — `AddressTypeDelegate`, `ContactTypeDelegate`
- [ ] **T-06**: `ScosOrganizationPermission` — 12 novas entradas
- [ ] **T-07**: `ExceptionCodeError` — 8 novas constantes com `httpCode`/`title` conforme a tabela (construtor de 3 args já existe, implementado pela change `padronizacao-http-status-exception-code`) + `scos_message_organization[_en].properties` — 8 mensagens de `detail` × 2 idiomas
- [ ] **T-08**: Testes unitários dos 2 `ServiceBean`

### Riscos e Edge Cases
1. `code` duplicado deve validar dentro do mesmo catálogo, não globalmente
2. `disable` de um tipo em uso não deve apagar vínculos existentes em `CompanyAddress`/`EmployeeAddress`/`CompanyContact`/`EmployeeContact` — só bloquear novos cadastros (nota explícita da doc 05)
3. `enable`/`disable` idempotentes devem retornar 422 (`SCOS_VALIDATION_*`) se já no estado alvo, não 204 silencioso

---

## 📎 Referências
- `etc/doc/usecase/05-catalogo-motivos.md` (seções 9, UC-081 a UC-092)
- Change completo `catalogo-tipo-endereco-contato` (contrato OpenAPI)
- Change de referência `crud-cargo-position` (padrão de implementação Position/Department)
- Change completa `padronizacao-http-status-exception-code` — já implementada; define o construtor de 3 argumentos (`code`/`httpCode`/`title`) de `ExceptionCodeError` e as 7 chaves `SCOS_TITLE_*` reaproveitadas aqui

---
