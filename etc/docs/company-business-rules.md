# Regras de Negócio — Company API

Fonte: `etc/api/organization/ScosOrganization_Company.yml`

## Visão Geral
Regras de negócio para operações sobre `Company`, `CompanyContact` e `CompanyAddress`.

---

## Entidades principais
- Company (id, name, nameTreatment, taxIdentifier, foundationDate, sectorOfActivity, parentCompany, status)
- CompanyContact (id, type, phone, email, responsiblePerson)
- CompanyAddress (id, addressId, street, zipCode, number, complement, city, state, country, latitude, longitude)

---

## Regras extraídas do OpenAPI (obrigatórias)
- Endpoints suportados: list, get by id, create (201), update (204), delete (204), enable/disable (PUT 204).
- Campos obrigatórios em criação/atualização de `Company`: `name`, `nameTreatment`, `taxIdentifier` (CNPJ), `foundationDate`, `sectorOfActivity`.
  - `taxIdentifier` é validado como CNPJ (`x-is-cnpj: true`).
- Mensagens/erros de validação padronizados via códigos `x-required-message`/`x-empty-message` (ex.: `SCOS-003`, `SCOS-001`).
- Operações idempotentes/cached descritas via `x-jdempotentresource` e `x-cache` (usar para evitar duplicação e para invalidar cache apropriado).
- Autorização por operação definida em `x-authorize` (ex.: `CREATE_COMPANY`, `UPDATE_COMPANY`, `DELETE_COMPANY`).

---

## Regras de negócio propostas / recomendadas (a confirmar)
- Unicidade: `taxIdentifier` (CNPJ) deve ser único entre empresas do tenant. Reutilizar `DomainService.validateUniqueTaxIdentifier` (README).
- Validação de data: `foundationDate` não pode ser superior à data atual.
- Parent company: impedir ciclos (uma empresa não pode ser sua própria ancestral). Validar profundidade máxima conforme regras de negócio (até 5 níveis onde aplicável).
- Exclusão: não permitir `DELETE` se existir:
  - funcionários ativos vinculados à empresa;
  - departamentos/posições ainda associados;
  - filiais (child companies). Preferir soft-delete com `status = DELETED` quando aplicável.
- Ao `disable`/`inactivate` de uma `Company`:
  - bloquear/criar eventos para sincronizar alterações em sistemas dependentes (ex.: Keycloak, serviços consumidores);
  - prevenir criação de novos recursos controlados pela empresa enquanto `inactive`.
- Auditoria: todas as alterações críticas (create/update/delete/enable/disable) devem gerar eventos de auditoria e eventos de domínio Kafka.

---

## Validações de campo (resumo)
- `name`: não vazio, tamanho mínimo/máximo a definir.
- `taxIdentifier`: formato CNPJ válido + unicidade.
- `foundationDate`: formato `date`, <= hoje.
- `parentCompanyId`: se informado, deve existir e estar `active`.

---

## Regras de exclusão e efeitos em cascata
- `Company` só pode ser removida de fato se não houver referências dependentes; caso contrário retornar 409/422 com mensagem explicando dependências.

---

## Eventos de domínio sugeridos (Kafka)
- `company.created` — payload: `{ companyId, tenantId, createdBy, timestamp }`
- `company.updated` — payload com campos alterados
- `company.deleted` / `company.status.changed`
- `company.contact.*`, `company.address.*`

---

## Códigos de validação importantes
- `SCOS-001` — campo vazio
- `SCOS-003` — campo obrigatório

---

## Perguntas / decisões a confirmar
- Soft-delete (`status = DELETED`) ou hard delete?
- Regras exatas de unicidade (global vs por tenant)?
- Políticas de sincronização com Keycloak para empresas (se aplicável).

---

## Observações técnicas
- Respeitar `x-jdempotentresource` e `x-cache` para consistência e performance.
- Consultar `README.md` para utilitários de validação de CNPJ e `TaxIdentifier`.
