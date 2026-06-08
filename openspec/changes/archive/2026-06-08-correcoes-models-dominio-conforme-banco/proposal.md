## Why

O change anterior (`atualizacao-models-dominio-conforme-banco`) introduziu três inconsistências: status `DELETED` foi omitido dos enums (confundido com `DISABLED`), campos de email foram mapeados como `String` sem validação via VO `Email`, e o campo `RESPONSIBLE_PERSON` de `CompanyContact` foi removido indevidamente. Estas correções são necessárias antes de qualquer uso dos models em camadas superiores.

## What Changes

- Adicionar `DELETED` a `StatusCompany` e `StatusEmployee` — soft delete permanente (distinto de `DISABLED`, que é desativação reversível)
- Adicionar método `delete()` em `Company` e `Employee` para transição para `DELETED`
- Atualizar guards de `activate()`/`inactivate()` para bloquear transições a partir de `DELETED`
- Substituir `String email` por `Email` VO (`@Embedded` + `@AttributeOverride`) em **todas** as entidades com campo de e-mail: `CompanyContact`, `Employee`, `IntegrationKeycloak`
- Restaurar campo `RESPONSIBLE_PERSON` em `CompanyContact` (entity + Liquibase + `domain_model.md`)

## Capabilities

### New Capabilities

- `status-deleted`: Comportamento de soft delete permanente via status `DELETED` nos enums de status
- `email-vo`: Uso obrigatório do VO `Email` para todos os campos de e-mail em entidades JPA

### Modified Capabilities

- `company-contact-schema`: Campo `RESPONSIBLE_PERSON` adicionado à tabela `SCOS_COMPANY_CONTACT` (schema + entity)

## Impact

**Código Java:**
- `model/company/StatusCompany.java`
- `model/company/Company.java` (método `delete()`, guards)
- `model/company/CompanyContact.java` (campo `email` → VO; campo `responsiblePerson` adicionado)
- `model/employee/StatusEmployee.java`
- `model/employee/Employee.java` (método `delete()`, guards, campo `email` → VO)
- `model/integration/IntegrationKeycloak.java` (campo `email` → VO)

**Banco de dados:**
- Liquibase `scos_company_contact.yml` — nova coluna `RESPONSIBLE_PERSON VARCHAR(255) NOT NULL`
- `etc/database/domain_model.md` — tabela `SCOS_COMPANY_CONTACT` atualizada
- `etc/doc/ideia/20260608_atualizacao-models-dominio-conforme-banco.md` — atualizar ideia

**Compilação:**
- Outras camadas que referenciem `StatusCompany`/`StatusEmployee` não são impactadas por adição de novo valor de enum
- Código que acessa `employee.getEmail()` ou `companyContact.getEmail()` como `String` precisa ser atualizado para `getEmail().getEmail()`
