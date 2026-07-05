## Context

`Position` (entidade JPA) e `PositionRepository` já existem em `scos-organization-domain/.../corporate/position/`, incluindo `activate()`/`deactivate()` (com `SCOS_POSITION_004`/`005`) e `existsByCode`/`existsByCodeAndNotId`/`existsByDepartmentId`. As permissões (`GET/CREATE/UPDATE/ENABLE/DISABLE_POSITION`) já existem em `ScosOrganizationPermission`. O contrato OpenAPI (`ScosOrganization_Department-Position.yml`) já define os 6 endpoints. O que falta é inteiramente a camada de aplicação: `dto/`, `service/`, `specification/` do Position, os Use Cases, e `PositionDelegate`.

O padrão de referência é `Department`, já 100% implementado: `specification/DepartmentService` (interface pública) + `service/DepartmentServiceBean` (`@Service` package-private) + `service/DepartmentMapper` (MapStruct) + `dto/DepartmentInput`/`DepartmentOutput` (records) no domain; e em `scos-organization-usecase`, uma interface pública + `@Service` Bean package-private por operação (`CreateDepartmentUseCase`/`CreateDepartmentUseCaseBean`, etc.), chamando `DepartmentService`. O `DepartmentDelegate` no módulo API injeta os Use Cases e mapeia DTOs gerados do OpenAPI ↔ tipos de domínio, sem MapStruct nessa camada.

Restrição adicional: `Position` referencia `Department` (`@ManyToOne`), e `Employee` referencia `Position` (`@ManyToOne`) — mas não existe relação inversa mapeada (`Position` não tem `Set<Employee>`), nem uma API pública no agregado Employee (`employee/specification/` está vazio hoje). A regra de disable de Position (UC-033) exige checar se há funcionário `ACTIVE` vinculado, o que exige alcançar o agregado Employee a partir do Position.

## Goals / Non-Goals

**Goals:**
- Implementar os 6 endpoints de Position (UC-028 a UC-033) replicando fielmente o padrão arquitetural do Department
- Reaproveitar `DepartmentService.findDepartmentById()` já existente para validar `departmentId` no create/update de Position (existência + status ativo)
- Resolver a checagem cross-agregado Employee→Position sem violar o encapsulamento do pacote `employee.internal`, expondo o mínimo necessário

**Non-Goals:**
- Jornada de Trabalho do Cargo (`/v1/positions/{positionId}/work-schedule*`) — já implementada em `jornada-trabalho-position-employee`
- Motivo de Mudança de Cargo (`/v1/reason-position-change*`) — feature própria, fora de escopo
- CRUD de Employee — só o mínimo necessário para a checagem de disable é adicionado
- Aplicar os textos de mensagem PT/EN de `SCOS_POSITION_004`/`005` nos arquivos `.properties` (Documento 07 já define o texto; aplicar é tarefa de infraestrutura de mensagens, não desta change)

## Decisions

### D-01: Estrutura em camadas idêntica ao Department

Domain (`corporate/position/dto|service|specification`) segue exatamente o padrão de Department: `PositionInput`/`PositionOutput` (records `@Builder`), `PositionMapper` (`@Mapper(componentModel = "spring")`), `PositionService` (interface pública com `create/update/findById/findAll/enable/disable/findPositionById`) + `PositionServiceBean` (`@Service` package-private, `@RequiredArgsConstructor`). Use Cases em `scos-organization-usecase/.../usecase/corporate/position/` seguem o mesmo par interface-pública + Bean-package-private por operação.

**Alternativa descartada**: estrutura hexagonal `port/in`/`usecase` (vista em `openspec/changes/impl-api-configuration`, nunca chegou a ser implementada em código real). Motivo: não corresponde ao padrão realmente em uso no código (`Department`, `Company`, etc.); manter uma única convenção no módulo.

### D-02: Reaproveitar `DepartmentService.findDepartmentById()`

`PositionServiceBean.create()`/`update()` chamam `departmentService.findDepartmentById(departmentId)`, que já lança `ScosException(SCOS_DEPARTMENT_001)` se não existir. Em seguida valida `department.isActive()`, lançando `ScosException(SCOS_DEPARTMENT_006)` se inativo.

**Alternativa descartada**: injetar `DepartmentRepository` diretamente em `PositionServiceBean`. Motivo: duplicaria a lógica de "não encontrado" já centralizada em `DepartmentService`; manter uma única porta de entrada pro agregado Department.

### D-03: Checagem de funcionário ativo — método público mínimo no agregado Employee

Como `employee/specification/` está vazio, cria-se uma interface pública mínima nesse pacote (ex.: `EmployeePositionQueryService` ou método adicionado a uma futura `EmployeeService`) expondo `boolean existsActiveEmployeeInPosition(Long positionId)`, implementada por um Bean package-private em `employee/service/` que usa QueryDSL sobre `EmployeeQueryRepository` (`qEmployee.position.id.eq(positionId).and(qEmployee.status.eq(StatusEmployee.ACTIVE))`). `PositionServiceBean.disable()` injeta essa interface pública — nunca `EmployeeQueryRepository` diretamente.

**Alternativa descartada 1**: chamar `EmployeeQueryRepository.existsByPositionId` direto do pacote `.internal` do Employee a partir do Position. Motivo: quebra o encapsulamento do agregado Employee, e o método existente não filtra por `status = ACTIVE` (bloquearia disable mesmo com só funcionários inativos vinculados).
**Alternativa descartada 2**: mapear `@OneToMany Set<Employee> employees` em `Position` (padrão Department→Position). Motivo: adicionaria uma coleção JPA só para uma checagem `exists`, sem outro uso; a API pública no agregado dono (Employee) é mais barata e não muda o mapeamento de `Position`.

### D-04: `PositionOutput` com `DepartmentOutput` aninhado

`PositionOutput` é um record com `department: DepartmentOutput` aninhado (reaproveita o record já existente no pacote `department.dto`). `findAll`/`findById` carregam a entidade `Position` via `QuerydslPredicateExecutor` (o `@ManyToOne department` já é EAGER por padrão) e mapeiam com `PositionMapper`, que compõe `DepartmentMapper` (MapStruct resolve automaticamente por injeção de outro mapper Spring).

**Alternativa descartada**: `@Query` JPQL com constructor expression flat, como `DepartmentRepository.findAll` faz para `DepartmentOutput`. Motivo: `DepartmentOutput` é plano (sem aninhamento); `PositionOutput` precisa do objeto `department` completo, o que uma constructor expression simples não suporta sem subquery extra.

### D-05: Filtro `departmentId` e filtro `active` aplicados (Position e Department)

`FindAllPositionUseCase`/`PositionService.findAll` recebem `Long departmentId` e `Boolean active` (ambos opcionais em runtime) e aplicam via QueryDSL em `PositionRepository.findAllFiltered` (`qPosition.department.id.eq(departmentId)` e `qPosition.active.eq(active)`, cada condição só entra na `BooleanBuilder` quando o valor correspondente não é `null`). O mesmo bug (filtro `active` aceito no contrato mas nunca aplicado na query) existia em `FindAllDepartmentUseCaseBean`/`DepartmentServiceBean` — corrigido junto nesta change: `DepartmentRepository` ganhou `findAllFiltered(Boolean active, Pageable pageable)` na mesma linha, e `DepartmentRepository.findAll(Pageable)` deixou de ser uma `@Query` JPQL com projeção direta para `DepartmentOutput` — agora retorna `Page<Department>` (entidade), mapeado para `DepartmentOutput` via `DepartmentMapper`, exatamente como Position já fazia.

### D-06: `PUT /v1/positions/{id}` sempre revalida o departamento

`PositionServiceBean.update()` chama `departmentService.findDepartmentById(departmentId)` e valida `active` em toda atualização, independentemente de o `departmentId` enviado ser igual ao atual do cargo. Evita deixar um cargo associado a um departamento que foi desativado após a última atualização.

### D-07: `isTrustPosition` sem regra de negócio própria

Campo opcional (`default: false`) apenas repassado entre `CreatePositionRequest`/`UpdatePositionRequest` ↔ `PositionInput` ↔ `Position` ↔ `PositionOutput`/response, sem validação adicional. O Documento 02 não lista o campo na tabela de campos do POST, mas o contrato OpenAPI já o define — tratado como mismatch de documentação, não como ambiguidade de comportamento.

### D-08: Ordem de validação no `disable`

Sequência dentro de `PositionServiceBean.disable()`, na ordem do Documento 02 (UC-033): (1) `findPositionById` → `SCOS_POSITION_001` se não existir; (2) checar `existsActiveEmployeeInPosition` → `SCOS_POSITION_003` se houver vínculo ativo; (3) `position.deactivate()` → lança `SCOS_POSITION_005` internamente se já inativo. A checagem de funcionário ativo vem antes da checagem de "já inativo" porque a regra de negócio (bloqueio por vínculo) é mais específica que o estado atual do cargo.

## Risks / Trade-offs

| Risco | Mitigação |
|-------|-----------|
| Novo método público em `employee/specification/` é o primeiro do agregado Employee — pode divergir do formato que um futuro CRUD de Employee venha a adotar | Manter a interface mínima e coesa (só a checagem necessária); revisar/consolidar quando o CRUD de Employee for proposto |
| `PositionOutput` com `DepartmentOutput` aninhado tira proveito do fetch EAGER de `Position.department` — em paginação grande pode gerar N+1 se o EAGER não for respeitado pelo Hibernate em certas queries QueryDSL | Validar com teste de integração (Testcontainers) que `findAll` não gera N+1; se necessário, usar `EntityGraph` explícito |
| `SCOS_POSITION_004`/`005` não têm texto de mensagem aplicado nos `.properties` ainda (Documento 07 já define o texto) | Fora do escopo desta change; abrir/usar item de infraestrutura de mensagens separado antes de considerar a feature "user-facing complete" |
| `DepartmentRepository.findAll(Pageable)` mudou de retorno (`Page<DepartmentOutput>` → `Page<Department>`) — qualquer outro chamador dessa assinatura precisaria de ajuste | Verificado: único chamador era `DepartmentServiceBean.findAll`, já atualizado nesta change |

## Open Questions

Nenhuma pendente — todas as decisões de escopo foram confirmadas com o usuário na fase de exploração (ver `etc/doc/ideia/20260705_crud-cargo-position.md`).
