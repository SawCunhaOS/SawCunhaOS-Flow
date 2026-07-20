# Layering & package structure

Package-by-feature at the top (one package per bounded context), then
package-by-layer inside. This keeps a context's code together and makes the
dependency direction visible.

```
com.example.ordering            <- one bounded context
├── domain
│   └── model
│       ├── Order.java                 (aggregate root)
│       ├── OrderLine.java             (entity, package-private constructor)
│       ├── OrderId.java               (value object / typed id)
│       ├── Money.java                 (value object)
│       ├── OrderStatus.java           (enum)
│       ├── OrderConfirmed.java        (domain event)
│       └── OrderRepository.java       (repository INTERFACE)
├── application
│   ├── ConfirmOrderUseCase.java       (@Service, @Transactional)
│   └── DomainEventPublisher.java      (port interface)
├── infrastructure
│   ├── persistence
│   │   ├── OrderJpaEntity.java        (@Entity — lives here, not in domain)
│   │   ├── OrderJpaEntityRepository.java (Spring Data interface)
│   │   ├── OrderMapper.java
│   │   └── JpaOrderRepository.java    (implements domain OrderRepository)
│   └── messaging
│       └── SpringDomainEventPublisher.java
└── api
    ├── OrderController.java           (@RestController)
    ├── dto
    │   ├── PlaceOrderRequest.java
    │   └── OrderResponse.java
    └── OrderApiMapper.java
```

## Enforcing the dependency rule

Direction must be: `api → application → domain` and `infrastructure → domain`.
The domain depends on nothing else in the project.

To stop violations from creeping in, suggest one of:

- **ArchUnit** tests that assert the domain package imports no Spring/JPA and
  that layers only depend inward. Example:

```java
@AnalyzeClasses(packages = "com.example.ordering")
class ArchitectureTest {
    @ArchTest
    static final ArchRule domain_is_framework_free =
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta.persistence..");
}
```

- **Spring Modulith** when contexts live in one deployable and you want runtime
  verification of module boundaries plus documentation generation. Mention it as
  an option for modular monoliths; it pairs naturally with this layout.

## Typed IDs

Prefer a value-object ID over a bare `UUID`/`Long`. It prevents passing an
`OrderId` where a `CustomerId` is expected and reads better.

```java
public record OrderId(UUID value) {
    public OrderId { if (value == null) throw new IllegalArgumentException("id required"); }
    public static OrderId newId() { return new OrderId(UUID.randomUUID()); }
}
```

## One context per package, not per project (at first)

Don't split into Maven/Gradle modules or microservices prematurely. Start with
package boundaries inside one module; promote to separate modules or services
only when team ownership, deployment, or scaling actually demands it. Splitting
too early ossifies boundaries you don't understand yet.

---

## SCOS Organization — real project structure

This project uses Maven modules (one per layer) with bounded-context grouping
inside `flow-organization-domain`. The generic layout above maps to:

```
flow-organization-domain      ← domain layer
  br.com.sawcunhaos.organization.domain
  ├── corporate/
  │   ├── company/
  │   │   ├── internal/        ← entity (@Entity), enums, repository interface
  │   │   │   ├── Company.java
  │   │   │   └── CompanyRepository.java
  │   │   ├── dto/             ← input/output DTOs for this aggregate
  │   │   ├── service/         ← domain service implementation (CompanyServiceBean)
  │   │   └── specification/   ← domain service interface (CompanyService)
  │   ├── department/  (same structure)
  │   ├── employee/    (same structure)
  │   └── position/    (same structure)
  ├── access/
  │   ├── login/       (same structure)
  │   ├── profile/     (same structure)
  │   └── ...
  └── configuration/   (same structure)

flow-organization-usecase     ← application layer
  br.com.sawcunhaos.organization.application.usecase
  └── corporate/
      └── department/
          ├── FindDepartmentUseCase.java      (public interface)
          └── FindDepartmentUseCaseBean.java  (package-private @Service)

flow-organization-api         ← interfaces/api layer
  br.com.sawcunhaos.organization.api
  └── delegate/
      └── department/
          └── DepartmentDelegate.java         (@Component implements DepartmentApiDelegate)

flow-organization-infrastructure  ← infrastructure layer (JPA config, async, message, etc.)
flow-organization-boot            ← app entry point + Liquibase changelogs
```

Key differences from the generic template:
- **Repositories live in `internal/`** alongside entities (not in a top-level `domain/repository/`).
- **Domain service interface in `specification/`**, implementation in `service/` (Bean suffix).
- **Use cases** follow interface + package-private Bean pattern in `flow-organization-usecase`.
- **Controllers** are delegates (`XxxDelegate implements XxxApiDelegate`) — no `@RestController`.
