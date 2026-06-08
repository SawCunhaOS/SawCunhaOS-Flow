## Why

As entidades JPA do módulo `scos-organization-domain` foram criadas antes das migrations Liquibase serem finalizadas, gerando divergência significativa entre código e banco: campos mapeados que não existem no banco, campos do banco ausentes nos models, nomes de colunas PK incorretos e seis tabelas sem nenhuma entidade JPA correspondente — impedindo o correto funcionamento do Hibernate e o uso das tabelas de integração, permissões e sistema.

## What Changes

- **Remover** campos sem correspondência no banco: `Company.dateCreated`, `Company.active`, `Employee.dateCreated`, `Employee.active`, `Login.password`, `Login.salt`, `Login.dateCreated`, `Login.dateLastChangePassword`, `Profile.features`, `CompanyContact.responsiblePerson`, `PartnersConfiguration.key/description/defaultValue`
- **Adicionar** campos presentes no banco e ausentes nos models: `Employee.status` (`StatusEmployee`), `Login.type` (`LoginType`), `PartnersConfiguration.value` e `PartnersConfiguration.type`
- **Corrigir** nomes de colunas PK: `CompanyContact.CONTACT_ID` → `COMPANY_ID_CONTACT`, `EmployeeContact.CONTACT_ID` → `EMPLOYEE_ID_CONTACT`
- **Corrigir** `EmployeeAddressPk.addressId` → `employeeIdAddress` / coluna `ADDRESS_ID` → `EMPLOYEE_ID_ADDRESS`
- **Refatorar** `CompanyAddress` de PK simples auto-gerada para PK composta (`CompanyAddressPk`), alinhando ao padrão já existente em `EmployeeAddress`
- **Corrigir** enum `StatusCompany.DELETED` → `DISABLED`; criar enums `StatusEmployee` e `LoginType`
- **Criar** 6 entidades JPA ausentes: `System`, `Resource`, `ProfileResource`, `IntegrationKeycloak`, `IntegrationKeycloakLog`, `IntegrationMessageInvalid` — cada uma com seu repositório
- **Aplicar** `@Auditable(auditRead = true)` nas entidades com dados pessoais (LGPD): `Employee`, `EmployeeContact`, `EmployeeAddress`, `Login`, `IntegrationKeycloak`
- **Atualizar** repositórios impactados pelas mudanças de PK e enum

## Capabilities

### New Capabilities

- `model-employee`: Entidade `Employee` corrigida com `StatusEmployee`, métodos de negócio ajustados e auditoria de leitura LGPD habilitada
- `model-company`: Entidade `Company` corrigida, `CompanyAddress` com PK composta (`CompanyAddressPk`), `CompanyContact` com PK corrigida
- `model-login`: Entidade `Login` corrigida com `LoginType`; `Profile` limpo
- `model-configuration`: `PartnersConfiguration` reestruturada conforme schema real
- `model-system`: Novas entidades `System` e `Resource` com repositórios
- `model-permission`: Nova entidade `ProfileResource` com PK composta e repositório
- `model-integration`: Novas entidades `IntegrationKeycloak`, `IntegrationKeycloakLog`, `IntegrationMessageInvalid` com repositórios

### Modified Capabilities

<!-- Nenhuma spec existente — projeto sem openspec/specs/ anterior -->

## Impact

- **Pacotes modificados**: `domain/model` e `domain/repository` em `scos-organization-domain`
- **Nenhuma migration criada** — Liquibase já está correto, somente código Java muda
- **Repositórios impactados**: `CompanyRepository` (enum), `CompanyAddressRepository` (PK Long → `CompanyAddressPk`), `CompanyContactRepository` (revisão)
- **Hibernate schema-validation**: deve passar sem erros após as mudanças
- **Camadas externas** (application, infrastructure, api): podem quebrar compilação onde referenciam campos removidos — fora do escopo desta change, mas devem ser corrigidas na sequência
