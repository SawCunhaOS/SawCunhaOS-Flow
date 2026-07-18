# Diretrizes de desenvolvimento — API (scos-organization-api → scos-organization-domain)

## Objetivo
Documentar as **regras, convenções e checklist** para criar/alterar uma API desde o módulo `flow-organization-api` até `flow-organization-domain`, garantindo consistência entre contrato (OpenAPI), código (permissões, features), persistência (Liquibase) e testes.

## Escopo
- Módulos: `flow-organization-api`, `flow-organization-usecase`, `flow-organization-domain`, `flow-organization-infrastructure`, `flow-organization-boot`.
- Artefatos: OpenAPI (`etc/api/organization/*.yml`), enums de permission/feature, Use Cases, Entities, Repositories, changelogs Liquibase, seeds (`configure_system.sql`), testes e CI.

---

## Resumo rápido
- Siga Clean Architecture + Hexagonal + DDD.
- Contrato primeiro: atualize o OpenAPI em `etc/api/organization` antes de codificar a controller.
- Permissões do `x-authorize` no OpenAPI devem existir em `ScosOrganizationPermission` e são validadas por `PermissionsConsistencyTest`.
- Auditoria: application‑side (campos `CREATED_AT`, `UPDATED_AT`, `USER_AT` em DB).
- Java 25, Spring Boot 4.x, PostgreSQL 18+, Maven.

---

## Definição Gerais
- **Lombok:** Usar anotações `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder` para reduzir boilerplate em entidades, DTOs e outros POJOs.
- **Injeção de Dependência:** Utilizar as anotações do Spring (`@Component`, `@Service`, `@Repository`) para gerenciar dependências e promover baixo acoplamento. Evitar injeção manual ou uso de `new` para criar instâncias de classes gerenciadas pelo Spring. Utilizar a anotação do lombok `@RequiredArgsConstructor` para injeção de dependências via construtor, garantindo imutabilidade e facilitando testes unitários.
- **Validação:** Usar Bean Validation (javax.validation) para validar DTOs de entrada, garantindo que as regras de negócio sejam respeitadas antes de chegar à camada de serviço ou domínio.
- **MapStruct:** Utilizar MapStruct para mapeamento entre DTOs e entidades, promovendo uma separação clara entre camadas e reduzindo o código de mapeamento manual.
- **Testes:** JUnit 5 para testes unitários, Mockito para mocks e TestContainers para testes de integração com banco de dados real. Seguir o padrão Given/When/Then para clareza nos testes.
- **Logging:** Usar a anotação do lombok `@Slf4j` com Logback para logging estruturado, garantindo que mensagens de log sejam informativas e úteis para depuração e monitoramento.
- **Transações:** Utilizar a anotação `@Transactional` do Spring para garantir a atomicidade das operações, especialmente em Use Cases que envolvem múltiplas operações de banco de dados. Configurar rollback para exceções específicas conforme necessário.
- **Auditoria:** Toda Entity deve estender `BaseEntity`, que inclui campos de auditoria (`createdAt`, `updatedAt`, `createdBy`, `updatedBy`). A anotação personalizada `@Auditable` pode ser usada para marcar entidades que devem ser auditadas, e um listener de JPA pode ser configurado para preencher esses campos automaticamente durante as operações de persistência.
- **NonNull:** Usar a anotação `@NonNull` do jspecify para indicar que um parâmetro ou campo não pode ser nulo, promovendo segurança contra NullPointerExceptions e melhorando a legibilidade do código. Deve ser usada em metodos de Use Cases, serviços e entidades para garantir que os dados essenciais sejam sempre fornecidos.
- **Exceções Personalizadas:** Criar exceções personalizadas (ex: `ScosException`) para representar erros específicos do domínio, facilitando o tratamento de erros e a comunicação clara de problemas para os consumidores da API.
- **QueryDSL:** Utilizar QueryDSL para consultas complexas no repositório, promovendo uma abordagem programática e segura para construção de queries, evitando erros comuns de string concatenation em JPQL ou SQL.
- **Liquibase:** Manter os scripts de migração de banco de dados organizados em `src/main/resources/db/changelog/`.
- **hypersistence-utils:** Usar hypersistence-utils para otimizar consultas e evitar problemas comuns de N+1, especialmente em relacionamentos complexos entre entidades.
- **openapi-generator-maven-plugin**:** Configurar o plugin para gerar os contratos de API a partir dos arquivos OpenAPI, garantindo que o código esteja sempre alinhado com a documentação e facilitando a manutenção do contrato.

---

## Estrutura de Entidades
- **Entities:** Representam o modelo de domínio, com regras de negócio e validações. Organizadas por **bounded context** em `scos-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/<bounded-context>/<agregado>/internal/`.
- **Bounded contexts ativos:** `corporate/` (company, department, employee, position), `access/` (login, profile, resource, system, integration), `configuration/`.
- **Estrutura por agregado:**
  - `internal/` — entidades JPA, enums, repositórios (acesso direto ao banco)
  - `dto/` — objetos de entrada/saída do domínio
  - `service/` — implementação do serviço de domínio (`...Bean`)
  - `specification/` — interface do serviço de domínio (contrato)
- **Exemplo:** `Company`, `Department`, `Employee`.
- **Regras de Negócio:** As regras de alteração de estado como ativação/desativação, hierarquia organizacional, etc., devem ser implementadas nas entidades ou em serviços de domínio, nunca na controller.
- **Exemplo de Código:**
```java
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "SCOS_COMPANY")
@Auditable
public class Company extends BaseEntity {
    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "ACTIVE", nullable = false)
    private boolean active;

    // Regras de negócio
    public void activate() {
        if (this.active) {
            throw new ScosException(SCOS_COMPANY_007);
        }
        this.active = true;
    }

    public void deactivate() {
        if (!this.active) {
            throw new ScosException(SCOS_COMPANY_007);
        }
        this.active = false;
    }
}
```

## Estrutura de Repositórios
- **Repositories:** Interfaces que definem as operações de persistência, localizadas em `scos-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/<bounded-context>/<agregado>/internal/`.
- **Exemplo:** `CompanyRepository`, `DepartmentRepository`.
- **Regras de Negócio:** 
   - Repositórios devem ser usados apenas para operações de acesso a dados (CRUD).
   - JpaSpecificationExecutor deve ser usado para consultas dinâmicas, enquanto QueryDSL pode ser utilizado para consultas complexas e específicas.
   - QuerydslPredicateExecutor deve ser implementado para permitir a construção de consultas flexíveis e seguras, especialmente para métodos de existência ou verificações condicionais.
   - Métodos de existência (ex: `existsByCode`) devem ser implementados usando QueryDSL para garantir eficiência e flexibilidade.
   - Validações como "departamento não pode ser deletado se tiver funcionários vinculados" devem ser implementadas em serviços de domínio, e o repositório deve fornecer os métodos necessários para verificar essas condições.
- **Exemplo de Código:**
```java
@Repository
public interface DepartmentRepository extends BaseJpaRepository<Department, Long>, JpaSpecificationExecutor<Department>, QuerydslPredicateExecutor<Department> {
    QDepartment qDepartment = QDepartment.department;

    Page<Department> findAll(Pageable pageable);

    Optional<Department> findById(Long id);

    default boolean existsByCodeAndNotId(Long departmentId, String code) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();

        booleanBuilder.and(qDepartment.code.eq(code))
                      .and(qDepartment.id.ne(departmentId));

        return exists(booleanBuilder.getValue());
    }
}```

## Estrutura de Dominio
- **Domain Services:** Contêm lógica de negócio que não pertence a uma única entidade, como validações complexas ou operações que envolvem múltiplas entidades. Localizados em `scos-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/<bounded-context>/<agregado>/service/` (implementação Bean) e `specification/` (interface).
- **Exemplo:** `DepartmentServiceBean implements DepartmentService` com métodos de validação como `validateDepartmentCodeExistsValidation`
- **Regras de Negócio:** Validações como "código de departamento deve ser único" ou "empresa deve existir para criar departamento" devem residir em serviços de domínio, garantindo que as entidades permaneçam focadas em seu estado e comportamento.
- **Exemplo de Código:**
```java
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DepartmentDomainService {

    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;

    public void validateDepartmentCodeExistsValidation(@NonNull String departmentCode){
        log.info("Validating department code: {}", departmentCode);
        if (departmentRepository.existsByCode(departmentCode)) throw new ScosException(ExceptionCodeError.SCOS_DEPARTMENT_002);
    }

}
```

## Estrutura de Use Cases
- **Use Cases:** Contêm a lógica de aplicação, orquestrando entidades e serviços para atender a um caso de uso específico. Localizados em `scos-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/<bounded-context>/<agregado>/`.
- **Padrão:** Interface pública (`FindDepartmentUseCase`) + implementação Bean package-private (`FindDepartmentUseCaseBean`), ambas no mesmo pacote.
- **Exemplo:** `FindDepartmentUseCase`, `FindDepartmentUseCaseBean`.
- **Regras de Negócio:** Use Cases devem ser transacionais e conter a lógica de orquestração, mas não regras de negócio complexas, que devem residir nas entidades ou serviços de domínio.
- **Exemplo de Código:**
```java
// Interface pública — exposta para o delegate
public interface FindDepartmentUseCase {
    DepartmentOutput execute(@NonNull Long departmentId);
}

// Implementação package-private — não vaza para fora do pacote
@Service
@RequiredArgsConstructor
@Slf4j
class FindDepartmentUseCaseBean implements FindDepartmentUseCase {

    private final DepartmentService departmentService;

    @Override
    public DepartmentOutput execute(@NonNull Long departmentId) {
        log.info("Finding department: {}", departmentId);
        return departmentService.findById(departmentId);
    }
}
```

## Estrutura de Controllers
- **Controllers (Delegates):** Implementam as interfaces geradas pelo OpenAPI generator, mapeando as requisições para os Use Cases. Localizados em `scos-organization-api/src/main/java/br/com/sawcunhaos/organization/api/delegate/<agregado>/`.
- **Nomenclatura:** `<Agregado>Delegate implements <Agregado>ApiDelegate`.
- **Regras de Negócio:** 
   - Delegates devem ser finos: apenas delegam para Use Cases e constroem o objeto de resposta com os DTOs gerados.
   - O contrato da API deve ser definido primeiro no OpenAPI (`etc/api/organization/*.yml`), e o delegate deve implementar a interface gerada a partir desse contrato.
   - DTOs gerados ficam em `target/generated-sources/openapi/src/main/java/br/com/sawcunhaos/organization/api/` e **não** devem ser editados manualmente.
   - A interface `XxxApiDelegate` é gerada; a implementação manual fica em `scos-organization-api/src/main/java/.../delegate/`.
- **Exemplo de Código:**
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class DepartmentDelegate implements DepartmentApiDelegate {

    private final FindDepartmentUseCase findDepartmentUseCase;

    @Override
    public GetDepartmentResponse getDepartmentById(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return GetDepartmentResponse.builder()
                .data(findDepartmentUseCase.execute(id))
                .build();
    }
}
```

## Estrutura do Liquibase
- **Changelogs:** Localizados no módulo `flow-organization-boot` em `src/main/resources/db/changelog/`, organizados por versão e tipo de alteração.
- **Regras:**
  - Todos os arquivos devem ser feitos com yml para manter consistência.
  - A Criação de views, functions, procedures e triggers deve ser criada em `sql` e feita via Liquibase, garantindo que a estrutura do banco de dados esteja sempre versionada e alinhada com o código.
  - O padrão de versionamento deve seguir o formato `vX.Y.Z` para a pasta, dentro da pasta deve ter o arquivo principal `vX.Y.Z.yml` que inclui os changelogs específicos de cada pasta tables, e insert, a cada pasta tem o arquivo com o nome da pasta para organizar os changelogs internos.
  - Pastas `function/`, `view/`, `procedure/` e `triggers/` ficam na **raiz** do changelog (não dentro de pastas de versão), pois evoluem independente de versão de schema. Scripts SQL individuais ficam dentro de cada pasta; o arquivo `<tipo>.yml` orquestra os `runOnChange: true` changesets.
  - Pastas de versão (`vX.Y.Z/`) contêm apenas `tables/` e `indexes/`.
  - Todo changelog deve conter um rollback definido para garantir que as alterações possam ser revertidas em caso de problemas.
  - Todo changelog deve ser referenciado no arquivo `vX.Y.Z.yml` correspondente (para tables/indexes) ou no `db.changelog-master.yml` (para function/view/triggers).
  - Todo changelog deve ter um `changeSet` com um `id` único e um `author` para garantir rastreabilidade.
  - Todo changelog deve ter tagDatabase com a versão correspondente (ex: `v1.0.0`).
  - Todo changelog deve seguir melhores práticas de design de banco de dados (normalização, índices, chaves estrangeiras).
  - Todo changelog deve seguir as melhores práticas do Liquibase: evitar mudanças destrutivas sem backup, descrições claras por changeSet.
- Exemplo de estrutura:
```text
src/main/resources/db/changelog/
├── db.changelog-master.yml       ← orquestra tudo
├── v1.0.0/
│   ├── v1.0.0.yml
│   ├── tables/
│   │   └── tables.yml
│   └── indexes/
│       └── indexes.yml
├── function/                     ← raiz, não versionado
│   ├── function.yml
│   └── minha_function.sql
├── view/
│   ├── view.yml
│   └── vw_minha_view.sql
├── triggers/
│   ├── triggers.yml
│   └── trg_minha_trigger.sql
└── procedure/
    └── procedure.yml
```
- Exemplo do arquivo `v1.0.0.yml`:
```yaml
databaseChangeLog:
    - include:
        file: tables/tables.yml
        relativeToChangelogFile: true
```
- Exemplo de changelog para criação de tabela:
```yaml
databaseChangeLog:
  - changeSet:
      id: SGC_1601202512432
      author: Samuel.Cunha
      changes:
        - tagDatabase:
            tag: v1.0.0
        - createTable:
            tableName: SCOS_POSITION
            columns:
              - column:
                  name:  POSITION_ID
                  type: serial
                  constraints:
                    primaryKey: true
                    primaryKeyName: PK_SCOS_POSITION
                    nullable: false
              - column:
                  name:  DEPARTMENT_ID
                  autoIncrement: false
                  type: BIGINT
                  constraints:
                    nullable: false
                    foreignKeyName: FK_DEPARTMENT_ID_SCOS_POSITION
                    validateForeignKey: true
                    referencedColumnNames: DEPARTMENT_ID
                    referencedTableName: SCOS_DEPARTMENT
              - column:
                  name:  CODE
                  type: varchar(50)
                  constraints:
                    nullable: false
        - renameSequence:
            oldSequenceName: SCOS_POSITION_POSITION_ID_SEQ
            newSequenceName: SEQ_POSITION_ID
        - addUniqueConstraint:
            columnNames: CODE
            constraintName: UK_CODE_SCOS_POSITION
            tableName: SCOS_POSITION
            validate: true
      rollback:
        - dropTable:
            cascadeConstraints: true
            tableName: SCOS_POSITION
---

## Padrão de códigos de erro (mensagens de erro do domínio)

Este documento define o padrão oficial para a criação e manutenção dos códigos de erro utilizados pelo módulo `scos-organization`.

### Padrão
- Formato: `SCOS_<MÓDULO>_<NNN>`
  - `SCOS_` — prefixo fixo do projeto
  - `<MÓDULO>` — nome da entidade ou funcionalidade em letras maiúsculas (ex.: DEPARTMENT, POSITION, COMPANY, USER)
  - `<NNN>` — sufixo numérico de 3 dígitos, incrementado sequencialmente por módulo (ex.: 001, 002)

Exemplos válidos:
- `SCOS_DEPARTMENT_001`
- `SCOS_POSITION_002`
- `SCOS_COMPANY_004`

Regex de validação sugerida: `^SCOS_[A-Z0-9]+_\d{3}$`

### Diretrizes de uso
- Sempre utilize o `ExceptionCodeError` (enum) ao lançar `ScosException` no domínio.
- Ao adicionar um novo erro para um módulo, inclua o próximo código incremental de 3 dígitos.
- Inclua mensagem legível no `messages.properties` ou equivalente e referencie o código no `ExceptionCodeError`.
- Não utilizar hífens (`-`) — usar underscore (`_`) conforme padrão.

### Compatibilidade / migração
- Códigos antigos baseados em prefixos diferentes (ex.: `SCOS_...`) podem existir — manter *aliases* apenas enquanto necessário para compatibilidade.
- Objetivo: padronizar todos os códigos para `SCOS_` e migrar referências gradualmente.

### Como adicionar um novo código (passo a passo)
1. Escolher o módulo/área (ex.: `DEPARTMENT`).
2. Incrementar o sufixo (ex.: se já existir `_001` e `_002`, use `_003`).
3. Adicionar a constante em `ExceptionCodeError` com o novo código (ex.: `SCOS_DEPARTMENT_003("SCOS_DEPARTMENT_003")`).
4. Adicionar teste cobrindo o lançamento do `ScosException` com o novo `ExceptionCodeError`.
5. Atualizar `messages.properties` com a mensagem human-readable vinculada ao código.

### Exemplo de referência no código
```java
if (!departmentRepository.existsById(id)) {
    throw new ScosException(ExceptionCodeError.SCOS_DEPARTMENT_001);
}
```

---

## Padrão de permissões e features
- As permissões devem seguir o formato `ACTION_RESOURCE` (ex.: `CREATE_COMPANY`, `ENABLE_EMPLOYEE`).
- As features devem seguir o formato `ORGANIZATION_<DOMÍNIO>_<SCOPE>` (ex.: `ORGANIZATION_COMPANY_MANAGEMENT`).
- O `x-authorize` nos OpenAPI YAMLs deve referenciar permissões que existem em `ScosOrganizationPermission` e devem ser validadas por `PermissionsConsistencyTest`.
- Nome das Features: `ORGANIZATION_<DOMÍNIO>_<SCOPE>` (ex.: `ORGANIZATION_COMPANY_MANAGEMENT`).
- Nome das permissões: `ACTION_RESOURCE` (ex.: `CREATE_COMPANY`, `ENABLE_EMPLOYEE`).
- Cache / x-cache: `SCOS_ORGANIZATION_<TAG>` (já aplicado nos YAMLs).

### Resumo (Feature → responsabilidades / exemplos de permissões)
- ORGANIZATION_ADMINISTRATION
  - Finalidade: administração global do sistema Organization. (aplicada apenas a operações administrativas)
  - Ex.: `GET_FEATURES`, gerenciamento de `Profile` (CREATE/UPDATE/DELETE_PROFILE), sincronização de papéis/roles, auditoria e operações infra/segurança.
- ORGANIZATION_VIEW
  - Finalidade: visão/consulta global.
  - Ex.: `GET_COMPANY`, `GET_EMPLOYEE`, `GET_FEATURES`.
- ORGANIZATION_MANAGEMENT
  - Finalidade: permissão genérica de gerenciamento.
  - Ex.: `CREATE_*`, `UPDATE_*`, `DELETE_*` quando aplicável.

- ORGANIZATION_COMPANY_VIEW / ORGANIZATION_COMPANY_MANAGEMENT
  - Ex.: `GET_COMPANY`, `CREATE_COMPANY`, `ENABLE_COMPANY`, `DOWNLOAD_COMPANY_DOCUMENT`.

- ORGANIZATION_EMPLOYEE_VIEW / ORGANIZATION_EMPLOYEE_MANAGEMENT / ORGANIZATION_EMPLOYEE_LOGIN_MANAGEMENT
  - Ex.: `GET_EMPLOYEE`, `CREATE_EMPLOYEE`, `ENABLE_EMPLOYEE`, `GET_EMPLOYEE_LOGIN`, `UPDATE_EMPLOYEE_LOGIN_PASSWORD`.

- ORGANIZATION_DEPARTMENT_VIEW / ORGANIZATION_DEPARTMENT_MANAGEMENT
  - Ex.: `GET_DEPARTMENT`, `CREATE_DEPARTMENT`.

- ORGANIZATION_POSITION_VIEW / ORGANIZATION_POSITION_MANAGEMENT
  - Ex.: `GET_POSITION`, `CREATE_POSITION`.

- ORGANIZATION_PROFILE_VIEW / ORGANIZATION_PROFILE_MANAGEMENT
  - Ex.: `GET_PROFILE`, `CREATE_PROFILE`.

### Regras de manutenção / sincronização
1. Sempre que adicionar um novo `x-authorize` no OpenAPI:
   - Criar a constante em `ScosOrganizationPermission.java` com o mesmo nome.
   - Atualizar `ScosOrganizationFeature.java` se precisar de nova feature.
   - Atualizar Keycloak realm / seed DB conforme necessário.
   - Rodar o teste `PermissionsConsistencyTest` (módulo `flow-organization-infrastructure`) para validar consistência.

2. Padrão de mapeamento sugerido:
   - Endpoints de leitura → `*_VIEW` + `ORGANIZATION_VIEW`
   - Endpoints de escrita/estado → `*_MANAGEMENT` + `ORGANIZATION_MANAGEMENT`
   - Endpoints sensíveis (senha, bloqueio) → `ORGANIZATION_EMPLOYEE_LOGIN_MANAGEMENT` + `ORGANIZATION_MANAGEMENT`

3. Onde armazenar novos papéis/roles:
   - Primeira opção: Keycloak (roles realm + mapper que traduz roles → authorities).
   - Segunda opção: arquivo YAML em `etc/security/permissions.yml` seguido de um job que sincronize com Keycloak/DB.

### Automação / validação disponível
- `PermissionsConsistencyTest` (módulo `flow-organization-infrastructure`): valida que todo `x-authorize` nos OpenAPI YAMLs possui constante em `ScosOrganizationPermission`.
- Recomenda-se adicionar CI step que execute esse teste para evitar drift.

### Exemplo prático (trecho)
- `x-authorize: [ ENABLE_COMPANY ]`  
  → deve existir `ScosOrganizationPermission.ENABLE_COMPANY` e a feature `ORGANIZATION_COMPANY_MANAGEMENT` deve cobrir essa permissão.

### Observações finais
- As `Features` foram renomeadas de `PARTNERS_*` para `ORGANIZATION_*` para refletir o escopo real do domínio.
- Alterações de nome podem requerer atualização do `Scos_Realm.json` (Keycloak) e do seed DB (arquivo SQL já atualizado).
