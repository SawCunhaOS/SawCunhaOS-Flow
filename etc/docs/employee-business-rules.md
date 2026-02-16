# Regras de Negócio — Employee API

Fonte: `etc/api/organization/ScosOrganization_Employee.yml`

## Visão Geral
Regras para criação/atualização/exclusão de `Employee` e recursos relacionados (`EmployeeContact`, `EmployeeAddress`).

---

## Entidades principais
- Employee (id, name, nameTreatment, taxIdentifier, email, birthDate, dateOfHiring, company, position, supervisor, active)
- EmployeeContact (type, phone)
- EmployeeAddress (addressId, number, complement, latitude, longitude)

---

## Regras extraídas do OpenAPI (obrigatórias)
- `CreateEmployeeRequest` campos obrigatórios: `name`, `nameTreatment`, `taxIdentifier`, `email`, `birthDate`, `dateOfHiring`, `company_id`, `position_id`.
- Mensagens de validação: `x-required-message`/`x-empty-message` (ex.: `SCOS-003`, `SCOS-001`).
- Endpoints: list, get by id, create (201), update (204), delete (204), enable/disable.

---

## Regras de negócio (detalhadas)
- Unicidade:
  - `email` deve ser único por tenant (sugerido).
  - `taxIdentifier` deve ser único entre empregados (sugerido; validar formato CPF/CNPJ conforme país/tenant).
- Validações de data:
  - `birthDate` < `dateOfHiring`.
  - `birthDate` e `dateOfHiring` não podem ser datas futuras.
- Referências:
  - `company_id` e `position_id` devem existir e estar `active`.
  - `supervisor_id`, se informado, deve apontar para outro `employee` existente e não pode formar ciclos.
- Regras de exclusão:
  - Não permitir exclusão de `Employee` se houver `Login` ativo, pendências ou histórico que exijam preservação.
  - Alternativa: soft-delete (`status = DELETED`) e manter histórico.
- Estado/Transições:
  - `enable`/`disable`: atualizar `active` e publicar evento; ao desativar, opcionalmente desabilitar `Login` associado.
- Sincronização externa:
  - Operações que afetam identidade/autenticação (criar/atualizar/remover) devem acionar sincronização com Keycloak via port/adapter.
- Auditoria: todas as operações de criação/alteração/exclusão geram eventos de auditoria e domínio.

---

## Validações de campo (exemplos)
- `email`: formato válido + domínio permitido (se aplicável).
- `taxIdentifier`: validar formato (CPF) e unicidade.
- `position_id`: IDs referenciais válidos.

---

## Eventos de domínio sugeridos
- `employee.created`, `employee.updated`, `employee.deleted`, `employee.status.changed`
- `employee.login.*` (quando aplicável)

---

## Perguntas / decisões a confirmar
- Forma de exclusão: hard vs soft-delete?
- Políticas de unicidade de e-mail e taxIdentifier (global vs por company)?
- Comportamento ao inativar funcionário (deve invalidar sessões/credenciais automaticamente?)

---

## Observações técnicas
- Use validações do domínio (Value Objects) para `taxIdentifier` (ex.: `TaxIdentifier` no README).
- Validar e bloquear operações concorrentes usando `x-jdempotentresource` quando aplicável.
