# Diretrizes de desenvolvimento — API (scos-organization-api → scos-organization-domain)

## Objetivo 🎯
Documentar as **regras, convenções e checklist** para criar/alterar uma API desde o módulo `scos-organization-api` até `scos-organization-domain`, garantindo consistência entre contrato (OpenAPI), código (permissões, features), persistência (Liquibase) e testes.

## Escopo
- Módulos: `scos-organization-api`, `scos-organization-application`, `scos-organization-domain`, `scos-organization-infrastructure`, `scos-organization-boot`.
- Artefatos: OpenAPI (`etc/api/organization/*.yml`), enums de permission/feature, Use Cases, Entities, Repositories, changelogs Liquibase, seeds (`configure_system.sql`), testes e CI.

---

## Resumo rápido ✅
- Siga Clean Architecture + Hexagonal + DDD.
- Contrato primeiro: atualize o OpenAPI em `etc/api/organization` antes de codificar a controller.
- Permissões do `x-authorize` no OpenAPI devem existir em `ScosOrganizationPermission` e são validadas por `PermissionsConsistencyTest`.
- Auditoria: application‑side (campos `CREATED_AT`, `UPDATED_AT`, `USER_AT` em DB).
- Java 25, Spring Boot 4.x, PostgreSQL 18+, Maven.

---

## Padrões e convenções importantes (resumido)
- Permissões: formato `ACTION_RESOURCE` (ex.: `ENABLE_COMPANY`, `DISABLE_EMPLOYEE`). Atualizar `ScosOrganizationPermission` quando criar nova permissão.
- Features: prefixo `ORGANIZATION_*` (definidos em `ScosOrganizationFeature`).
- `x-authorize` nas OpenAPI operations deve usar CONSTANTS do enum de permissões.
- Cache: `x-cache` + `cachePrefix` devem seguir `SCOS_ORGANIZATION_<TAG>` e basear-se em `tags` do endpoint.
- DTOs simples → usar `record`; hierarquias controladas → `sealed` classes.
- Naming de testes: unit `*Test.java`, integration `*IntegrationTest.java`.
- Commits: `[TIPO] Mensagem curta` (ex.: `[FEAT] Criar endpoint de company`).
- Branch: `feature/<descrição>`, `bugfix/<descrição>`, `fix/<hotfix>`.

---

## Fluxo end‑to‑end (resumo de implementação)
1. Atualizar OpenAPI (`etc/api/organization/ScosOrganization_*.yml`) com `x-authorize`, `x-cache`, `tags` e contrato.
2. Adicionar/atualizar permissão em `ScosOrganizationPermission` (e feature em `ScosOrganizationFeature` se necessário).
4. Implementar Controller no `scos-organization-api` que delega para um Use Case da camada `application`.
5. Implementar Use Case em `scos-organization-application` (Ports & UseCase). Validar entrada e converter para entidades de domínio.
6. Atualizar/Adicionar entidades e interfaces de `Repository` no `scos-organization-domain`.
7. Implementar Adapter/Repository concreto em `scos-organization-infrastructure`.
8. Se alterações de esquema forem necessárias, adicionar changelog Liquibase em `scos-organization-boot/src/main/resources/db/changelog/...` com rollback.
9. Adicionar testes unitários e de integração; atualizar seeds (`configure_system.sql`) se necessário.
10. Executar `mvn clean install` e garantir `PermissionsConsistencyTest` passa.

---

## Checklist detalhado ao adicionar um novo endpoint (PASSO A PASSO)
1. OpenAPI
   - Atualize `etc/api/organization/ScosOrganization_*.yml` (descrição, request/response, tags).
   - Adicione `x-authorize: <PERMISSION>` e `x-cache` / `cachePrefix` quando aplicável.
   - Defina `operationId` claro.

   Exemplo:
   ```yaml
   /companies:
     post:
       tags: [Company]
       operationId: createCompany
       x-authorize: CREATE_COMPANY
       x-cache: false
       requestBody: ...
   ```

2. Permissão / Feature
   - Adicione a constante em `scos-organization-infrastructure/.../ScosOrganizationPermission.java`.
   - Se for uma nova área funcional, adicione feature em `ScosOrganizationFeature`.
   - Atualize `etc/architecture/permissions.md` (mapeamento Feature ↔ Permission).

3. Controller
   - Implementar endpoint REST (validations com `@Valid`, DTOs, response codes corretos).
   - Delegar trabalho ao Use Case da camada application.

4. Use Case (Application Layer)
   - Nome: `CreateCompanyUseCase`/`CreateCompanyService` com input DTO e output DTO.
   - Implementar regras de negócio mínimas e chamar Port (`Repository` ou `EventPublisher`).

5. Domain
   - Adicionar/alterar Entidades/Value Objects e invariantes no `scos-organization-domain`.
   - Repositório (interface) permanece no domain; implementação vai para `infrastructure`.

6. Infra / Persistência
   - Implementar Repository Adapter em `scos-organization-infrastructure`.
   - Se precisar de migrations, crie arquivo Liquibase em `scos-organization-boot/src/main/resources/db/changelog/v<...>/tables`.
   - Incluir `CREATED_AT/UPDATED_AT/USER_AT` e definir rollback.

7. Seeds / Configuração
   - Se precisar de usuários/profiles iniciais, atualize `configure_system.sql`.

8. Testes
   - Unit tests para UseCase/Domain (nome `*Test.java`).
   - Integration test com `@SpringBootTest` e, se possível, DB embarcado/containers (nome `*IntegrationTest.java`).
   - Adicione testes para garantir que `x-authorize` token existe (PermissionsConsistencyTest já cuida disso).

9. Documentação & PR
   - Atualize `etc/architecture`/docs relevantes e `README.md` se necessário.
   - Abra PR com descrição clara, checklist preenchido e referência a tickets.

---

## Segurança / Autorização
- `x-authorize` nos YAMLs deve corresponder a constantes em `ScosOrganizationPermission`.
- Evite lógica de autorização no controller; utilize a camada de segurança centralizada (filtros/interceptors/annotations).

---

## Auditoria (policy)
- Auditoria é feita pela aplicação: garanta que `CREATED_AT`, `UPDATED_AT`, `USER_AT` sejam populados via `AuditorAware` / `EntityListeners`.
- Não rely on DB triggers (projeto decidiu manter auditing na camada app).
- Adicione testes que verifiquem os campos de auditoria ao persistir entidades.

---

## Domain Events & Kafka
- Publique eventos de domínio na camada `application` (ex.: `CompanyCreatedEvent`).
- Nome de tópico e payload: siga o padrão do repositório (consulte outros eventos).
- Garanta idempotência e schema‑versioning.

---

## Testes e CI 🧪
- Unitários: rápidos, isolados; `*Test.java`.
- Integração: `@SpringBootTest`, cobertura para flows que envolvem DB e camada web; `*IntegrationTest.java`.
- Testes críticos devem manter cobertura > 80%.
- CI deverá executar `PermissionsConsistencyTest` para garantir `x-authorize` ↔ enum sincronizados.

Comandos úteis:
- `mvn -pl scos-organization-boot -am clean install`
- Executar apenas testes de permissões: execute a classe `PermissionsConsistencyTest` no módulo `scos-organization-infrastructure`.

---

## Regras de revisão de PR (cheklist mínimo)
- [ ] Compila: `mvn clean install` passa
- [ ] Testes unitários e integração passam
- [ ] `PermissionsConsistencyTest` passa
- [ ] OpenAPI atualizado e `x-authorize` mapeado para `ScosOrganizationPermission`
- [ ] Changelog Liquibase com rollback adicionado (quando houver DDL)
- [ ] Seeds/`configure_system.sql` ajustados se necessário
- [ ] Documentação atualizada (`etc/architecture`, `README.md`)

---

## Exemplos rápidos
- OpenAPI operation (exemplo):
```yaml
post:
  tags: [Company]
  operationId: createCompany
  x-authorize: CREATE_COMPANY
  requestBody: ...
```
- Permission enum (exemplo):
```java
// ScosOrganizationPermission.java
CREATE_COMPANY("CREATE_COMPANY", List.of(ScosOrganizationFeature.ORGANIZATION_COMPANY_MANAGEMENT))
```
- UseCase skeleton:
```java
public interface CreateCompanyUseCase { CompanyOutput execute(CreateCompanyInput in); }
```

---

## Onde procurar / arquivos de referência 🔎
- OpenAPI: `etc/api/organization/ScosOrganization_*.yml`
- Permissions: `scos-organization-infrastructure/.../ScosOrganizationPermission.java`
- Features: `scos-organization-infrastructure/.../ScosOrganizationFeature.java`
- Liquibase changelogs: `scos-organization-boot/src/main/resources/db/changelog/**`
- Permissões doc: `etc/architecture/permissions.md`

---

## FAQ / Problemas comuns
- Permission mismatch (failing `PermissionsConsistencyTest`): verifique `x-authorize` no YAML e o enum em `ScosOrganizationPermission`.
- Falha em migração Liquibase: confirme `rollback` e dependências de foreign keys.
- Auditoria não populada: verifique `AuditorAware` e testes de integração.

---

## Observações finais 💡
- Priorize contrato → código → infra.
- Use os padrões do projeto (naming, arquitetura e testes) para manter consistência e evitar regressões.
