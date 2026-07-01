## REMOVED Requirements

### Requirement: StatusCompany inclui DELETED para soft delete permanente
**Reason**: O schema v2 (`adequacao-liquibase-domain-model-v2`) introduziu um `CHECK` de vocabulário fechado em `SCOS_COMPANY.STATUS` (`ACTIVE/INACTIVE/DISABLED`) que não admite `DELETED`. `INACTIVE` passa a ser o único estado terminal definitivo — não existe mais uma distinção entre "inativado" e "deletado permanentemente".
**Migration**: Remover `StatusCompany.DELETED` e `Company.delete()`. Código que chamava `company.delete()` deve chamar `company.inactivate(reasonInactivateId)` (ver `model-status-history`).

### Requirement: StatusEmployee inclui DELETED para soft delete permanente
**Reason**: Mesmo motivo do requirement acima, aplicado a `SCOS_EMPLOYEE.STATUS` (`CHECK` `ACTIVE/INACTIVE/DISABLED`).
**Migration**: Remover `StatusEmployee.DELETED` e `Employee.delete()`. Código que chamava `employee.delete()` deve chamar `employee.inactivate(reasonInactivateId)` (ver `model-status-history`).
