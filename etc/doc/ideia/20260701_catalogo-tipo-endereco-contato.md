# Catálogo de Tipo de Endereço e Contato

**Data**: 2026-07-01
**Status**: 🔄 Em Análise
**Tipo**: 🔧 Refatoração

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

> **Regra**: Uma ideia = uma funcionalidade. Features independentes → arquivos separados.

- **Nome da funcionalidade**: `catalogo-tipo-endereco-contato`
- **Resumo em uma frase**: Substituir o campo `type: string` livre em `CompanyAddress`/`EmployeeAddress`/`CompanyContact`/`EmployeeContact` por referência a catálogos dinâmicos `AddressType`/`ContactType`, alinhando o contrato à FK fechada do schema v2.

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade — migrar `type` texto livre para catálogo FK, nos dois pares (endereço, contato) por serem a mesma mudança estrutural aplicada em paralelo
- [x] Não mistura com histórico de status, fiscal, cargo, jornada, outbox ou perfil — ideias próprias
- [x] O nome é específico

---

## 1️⃣ Visão

### Problema
`CompanyAddress`/`EmployeeAddress`/`CompanyContact`/`EmployeeContact` usam hoje `type: string` sem validação, texto livre. O schema v2 substituiu isso por FK para `SCOS_ADDRESS_TYPE`/`SCOS_CONTACT_TYPE` — catálogos dinâmicos, compartilhados entre `COMPANY` e `EMPLOYEE` via coluna `ENTITY_TYPE`, com `DELETE` bloqueado por trigger (só desativa via `ACTIVE`). O contrato público ainda expõe o campo antigo como string livre, incompatível com o banco atual.

### Objetivo
`AddressType`/`ContactType` têm CRUD de referência completo (create/update, sem delete — só desativar); os 4 schemas de endereço/contato passam a usar `addressTypeId`/`contactTypeId` (integer, FK) no lugar de `type` (string). Critério de sucesso: nenhum campo `type` livre restante nesses 4 schemas.

### Fora de Escopo
- Histórico de status, dados fiscais, cargo/contrato, jornada, outbox, perfil adicional — ideias próprias
- Migração de dados existentes — sistema sem produção, decisão já tomada em rodadas anteriores

---

## 2️⃣ Requisitos

### Funcionais
- [ ] **RF-01**: Criar CRUD de referência `AddressType` — `code`, `description`, `entityType` (COMPANY/EMPLOYEE), `active`. Endpoints: `GET/POST /v1/address-types` (com filtro `?entityType=`), `GET/PUT /v1/address-types/{id}`, `PUT /v1/address-types/{id}/enable`, `PUT /v1/address-types/{id}/disable` (sem `DELETE` — bloqueado por trigger no banco)
- [ ] **RF-02**: Criar CRUD de referência `ContactType` — mesmo padrão de `AddressType`
- [ ] **RF-03**: `CompanyAddress`/`EmployeeAddress` (schemas `*Address`, `Create*AddressRequest`, `Update*AddressRequest`): trocar `type: string` por `addressTypeId: integer (format: int64)`, obrigatório
- [ ] **RF-04**: `CompanyContact`/`EmployeeContact` (schemas equivalentes): trocar `type: string` por `contactTypeId: integer (format: int64)`, obrigatório
- [ ] **RF-05**: Corrigir divergência de tipo já identificada na rodada JPA: `EmployeeAddress.number` estava `long`, banco é `INT` — ajustar o schema OpenAPI se estiver com tipo mais largo que `int32`
- [ ] **RF-06** (achado na revisão): Toda rota nova precisa de `x-authorize` + entrada correspondente em `ScosOrganizationPermission` — `GET/CREATE/UPDATE_ADDRESS_TYPE`, `ENABLE/DISABLE_ADDRESS_TYPE`, mesmo conjunto para `CONTACT_TYPE`

### Não-Funcionais
- [ ] **RNF-01**: Breaking change aceito diretamente, sem período de transição — decidido com o usuário (sistema sem produção)
- [ ] **RNF-02**: Nenhuma alteração de schema no banco — schema v2 já existe
- [ ] **RNF-03** (achado na revisão): DTOs seguem o padrão SCOS do restante do arquivo — `CreateAddressTypeRequest`/`UpdateAddressTypeRequest` (mesmo pra `ContactType`), `GetAddressTypeResponse { data }`, `GetAllAddressTypesResponse { data: array, paginatedDTO }`. `GET /v1/address-types`/`GET /v1/contact-types` usam `paginationFilter` (query, obrigatório) — sem exceção nenhuma lista do contrato foge da paginação

---

## 3️⃣ Arquitetura

### Componentes Afetados
```
etc/api/organization/
├── ScosOrganization_Company.yml
│   ├── schemas novos: AddressType, ContactType (ou compartilhado — ver decisão)
│   ├── paths novos: /v1/address-types*, /v1/contact-types*
│   └── CompanyAddress(es)/CompanyContact (mod — type → addressTypeId/contactTypeId)
└── ScosOrganization_Employee.yml
    └── EmployeeAddress/EmployeeContact (mod — type → addressTypeId/contactTypeId)
```
> `AddressType`/`ContactType` são compartilhados entre `Company` e `Employee` (via `entityType`) — decidir no `/propose` se os schemas/paths ficam num arquivo central (`ScosComponents.yml` ou um `ScosOrganization_Catalog.yml` novo) referenciado por ambos, ou duplicados. Recomendação: arquivo central, para não duplicar schema em 2 arquivos.
>
> **Atenção — não é o mesmo enum `entityType` de `historico-status-motivos-organization`**: o `entityType` de `AddressType`/`ContactType` tem só 2 valores (`COMPANY`/`EMPLOYEE`), confirmado pelo `CHECK` real do banco (`chk_address_type_entity_type`/`chk_contact_type_entity_type`, `flow-organization-boot/.../checks/checks.yml`). Já o `entityType` dos 4 catálogos `Reason*` (idea `historico-status-motivos-organization`) tem 3 valores (`COMPANY`/`EMPLOYEE`/`LOGIN`). A idea JPA (`atualizacao-entidades-jpa-liquibase-v2`, RF-20) fala em "reaproveitar o enum `EntityType` do item A" — isso vale só pro **enum Java interno**; no **contrato OpenAPI** precisam ser 2 schemas de enum distintos (nomes a decidir no `/propose`), senão o contrato aceitaria `LOGIN` aqui e só falharia depois no `CHECK` do Postgres, em vez de um `4XX` de validação.

### Detalhamento de Campos por Arquivo YAML

| Arquivo YAML | Schema/Path | Campo | Tipo | Nota |
|---|---|---|---|---|
| central (a decidir no `/propose`) | `AddressType` (novo) | `code`, `description`, `entityType` (`COMPANY`/`EMPLOYEE`), `active` | string / string / enum (2 valores) / boolean | catálogo de referência; enum próprio — **não** é o `EntityType` de 3 valores dos catálogos `Reason*` |
| central (a decidir no `/propose`) | `ContactType` (novo) | `code`, `description`, `entityType` (`COMPANY`/`EMPLOYEE`), `active` | string / string / enum (2 valores) / boolean | mesmo padrão de `AddressType` |
| central (a decidir no `/propose`) | `CreateAddressTypeRequest`/`UpdateAddressTypeRequest`, `CreateContactTypeRequest`/`UpdateContactTypeRequest` (novos) | `code`, `description`, `entityType` | string / string / enum | `active` fora do body — muda via `enable`/`disable` |
| central (a decidir no `/propose`) | `GetAddressTypeResponse`/`GetAllAddressTypesResponse`, `GetContactTypeResponse`/`GetAllContactTypesResponse` (novos) | `data`, `paginatedDTO` | `$ref` / `$ref ScosPaginated` | padrão SCOS `{ data }` singular, `{ data: array, paginatedDTO }` lista |
| central (a decidir no `/propose`) | `GET/POST /v1/address-types`, `GET/PUT /v1/address-types/{id}`, `PUT .../enable`, `PUT .../disable` (novos) | — | — | mesmo conjunto de paths para `/v1/contact-types` |
| `ScosOrganization_Company.yml` | `CompanyAddress`, `CreateCompanyAddressRequest`, `UpdateCompanyAddressRequest` (mod) | ~~`type: string`~~ → `addressTypeId` | `integer (format: int64)` | obrigatório, FK `AddressType` |
| `ScosOrganization_Company.yml` | `CompanyContact`, `CreateCompanyContactRequest`, `UpdateCompanyContactRequest` (mod) | ~~`type: string`~~ → `contactTypeId` | `integer (format: int64)` | obrigatório, FK `ContactType` |
| `ScosOrganization_Employee.yml` | `EmployeeAddress`, `CreateEmployeeAddressRequest`, `UpdateEmployeeAddressRequest` (mod) | ~~`type: string`~~ → `addressTypeId`; `number` (correção) | `integer (format: int64)`; `integer (format: int32)` | `addressTypeId` obrigatório; `number` estava `long`, banco é `INT` (RF-05) |
| `ScosOrganization_Employee.yml` | `EmployeeContact`, `CreateEmployeeContactRequest`, `UpdateEmployeeContactRequest` (mod) | ~~`type: string`~~ → `contactTypeId` | `integer (format: int64)` | obrigatório, FK `ContactType` |

### Fluxo Principal
```
POST /companies/{companyId}/addresses { addressTypeId, number, complement, latitude, longitude, addressId }
  → valida addressTypeId existe, active=true, entityType=COMPANY
  → usecase cria CompanyAddress com FK addressTypeId
```

### Decisões Técnicas
| Decisão | Escolha | Alternativa Descartada | Motivo |
|---------|---------|------------------------|--------|
| Breaking change | Quebra direto, sem compatibilidade retroativa | Manter os dois campos por um tempo | Decidido com o usuário — sem produção, sem necessidade de transição |
| Localização das rotas de catálogo | Top-level com filtro `?entityType=` (`/v1/address-types`, `/v1/contact-types`) | Rotas duplicadas por agregado | Decidido com o usuário — mesmo padrão de `/v1/departments`, evita duplicação |
| `DELETE` em AddressType/ContactType | Não expor — banco bloqueia via trigger | Expor e deixar erro de banco estourar | Trigger já impede fisicamente; contrato não deve oferecer uma operação que sempre falha |

### Banco de Dados
- **Impacto**: ❌ Não — schema já existe (`adequacao-liquibase-domain-model-v2`, completo)

---

## 4️⃣ Implementação

*Detalhamento de arquivos/tarefas fica para a fase de `/openspec-propose`.*

### Riscos e Edge Cases
1. Onde ficam os schemas/paths compartilhados de `AddressType`/`ContactType` — decidir arquivo central no `/propose` para evitar duplicação entre `Company.yml` e `Employee.yml`
2. Breaking change no contrato público — qualquer client já integrado (mesmo em ambiente de dev/homolog) quebra; comunicar antes de publicar a mudança
3. `entityType` do tipo escolhido precisa bater com o agregado do endereço/contato sendo criado (`AddressType.entityType=COMPANY` só pode ser usado em `CompanyAddress`) — regra de negócio, não validável só pelo contrato

---

## 📎 Referências
- Domain model: `etc/database/domain_model.md`
- Migrations v2: `openspec/changes/adequacao-liquibase-domain-model-v2/`
- Riscos já sinalizados na rodada JPA anterior: `openspec/changes/atualizacao-entidades-jpa-liquibase-v2/`

---
