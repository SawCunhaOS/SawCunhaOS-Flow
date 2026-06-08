## 1. Adicionar DELETED aos enums de status

- [x] 1.1 Adicionar `DELETED("Deletada")` em `StatusCompany`
- [x] 1.2 Adicionar `DELETED("Deletado")` em `StatusEmployee`

## 2. Atualizar Company com método delete() e guards

- [x] 2.1 Adicionar método `delete()` em `Company` — define `status = StatusCompany.DELETED`
- [x] 2.2 Atualizar guard em `activate()`: bloquear também quando `status == DELETED`
- [x] 2.3 Atualizar guard em `inactivate()`: bloquear também quando `status == DELETED`

## 3. Atualizar Employee com método delete() e guards

- [x] 3.1 Adicionar método `delete()` em `Employee` — define `status = StatusEmployee.DELETED`
- [x] 3.2 Adicionar guard em `activate()`: lançar exceção quando `status == DELETED`
- [x] 3.3 Adicionar guard em `inactivate()`: lançar exceção quando `status == DELETED`

## 4. Aplicar Email VO em CompanyContact

- [x] 4.1 Substituir `String email` por `@Embedded @AttributeOverride(name = "email", column = @Column(name = "EMAIL")) Email email` em `CompanyContact`
- [x] 4.2 Adicionar import `br.com.sawcunhaos.foundation.utils.valueobjects.Email` em `CompanyContact`
- [x] 4.3 Remover import de `Column` duplicado se necessário (manter apenas o necessário)

## 5. Aplicar Email VO em Employee

- [x] 5.1 Substituir `String email` por `@Embedded @AttributeOverride(name = "email", column = @Column(name = "EMAIL")) Email email` em `Employee`
- [x] 5.2 Adicionar import `br.com.sawcunhaos.foundation.utils.valueobjects.Email` em `Employee`

## 6. Aplicar Email VO em IntegrationKeycloak

- [x] 6.1 Substituir `String email` por `@Embedded @AttributeOverride(name = "email", column = @Column(name = "EMAIL")) Email email` em `IntegrationKeycloak`
- [x] 6.2 Adicionar import `br.com.sawcunhaos.foundation.utils.valueobjects.Email` em `IntegrationKeycloak`

## 7. Restaurar RESPONSIBLE_PERSON em CompanyContact

- [x] 7.1 Adicionar campo `responsiblePerson` com `@Column(name = "RESPONSIBLE_PERSON")` em `CompanyContact`
- [x] 7.2 Adicionar coluna `RESPONSIBLE_PERSON VARCHAR(255) NOT NULL` no changeset `20260606-Samuel.Cunha-005` em `scos_company_contact.yml`
- [x] 7.3 Adicionar campo `RESPONSIBLE_PERSON` na tabela `SCOS_COMPANY_CONTACT` em `etc/database/domain_model.md` (diagrama mermaid + tabela descritiva)

## 8. Atualizar arquivo de ideia

- [x] 8.1 Atualizar `etc/doc/ideia/20260608_atualizacao-models-dominio-conforme-banco.md` com as três correções: DELETED nos enums, Email VO, e RESPONSIBLE_PERSON
