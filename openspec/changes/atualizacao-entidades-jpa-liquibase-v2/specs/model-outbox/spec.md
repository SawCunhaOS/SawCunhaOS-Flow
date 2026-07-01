## ADDED Requirements

### Requirement: Enums OutboxBackend e OutboxEventStatus
O sistema SHALL fornecer o enum `OutboxBackend` com os valores `PGMQ`, `KAFKA`, `DIRECT_API`, e o enum `OutboxEventStatus` com os valores `PENDING`, `PROCESSING`, `PROCESSED`, `FAILED`.

#### Scenario: OutboxBackend com três valores
- **WHEN** o enum `OutboxBackend` é inspecionado
- **THEN** deve conter exatamente `PGMQ`, `KAFKA`, `DIRECT_API`

#### Scenario: OutboxEventStatus com quatro valores
- **WHEN** o enum `OutboxEventStatus` é inspecionado
- **THEN** deve conter exatamente `PENDING`, `PROCESSING`, `PROCESSED`, `FAILED`

### Requirement: Entidade OutboxTopic
O sistema SHALL fornecer a entidade `OutboxTopic`, estendendo `BaseEntity`, anotada com `@Auditable`, com PK natural `String topic` (coluna `TOPIC`, sem `@GeneratedValue`), `backend` (`OutboxBackend`), `targetSystem` (nullable), `defaultMaxRetries` (int, default 3) e `active`.

#### Scenario: PK é chave natural sem geração automática
- **WHEN** a entidade `OutboxTopic` é inspecionada
- **THEN** o campo `@Id` é do tipo `String`, mapeado para a coluna `TOPIC`, sem `@GeneratedValue`

#### Scenario: Repositório criado
- **WHEN** o módulo é inspecionado
- **THEN** deve existir `OutboxTopicRepository` estendendo `BaseJpaRepository<OutboxTopic, String>` + `JpaSpecificationExecutor` + `QuerydslPredicateExecutor`

### Requirement: Entidade OutboxEvent sem herança de BaseEntity
A entidade `OutboxEvent` SHALL mapear `updatedAt` e `userAt` como campos simples nullable (sem `@UpdateTimestamp`), sem estender `BaseEntity`, pois a tabela tem `UPDATED_AT`/`USER_AT` nullable — incompatível com o contrato `NOT NULL` de `BaseEntity`. `createdAt` usa `@CreationTimestamp`. Campos: `eventHash` (`UK`), FK `topic`, `aggregateId`, `payload` (JSONB via `@Type(JsonBinaryType.class)`), `responseData` (JSONB nullable), `status` (`OutboxEventStatus`), `retryCount` (default 0), `maxRetries` (default 3), `message` (nullable), `requesting`, `startedAt` (nullable), `processedAt` (nullable).

#### Scenario: OutboxEvent não estende BaseEntity
- **WHEN** a entidade `OutboxEvent` é inspecionada
- **THEN** não deve estender `BaseEntity`; `updatedAt` e `userAt` são campos simples nullable

#### Scenario: payload mapeado como JSONB
- **WHEN** o campo `payload` é inspecionado
- **THEN** deve estar anotado com `@Type(JsonBinaryType.class)`

#### Scenario: Repositório criado
- **WHEN** o módulo é inspecionado
- **THEN** deve existir `OutboxEventRepository` estendendo `BaseJpaRepository<OutboxEvent, Long>` + `JpaSpecificationExecutor` + `QuerydslPredicateExecutor`

### Requirement: Entidades OutboxEventLog e OutboxEventDeadLetter imutáveis
O sistema SHALL fornecer `OutboxEventLog` e `OutboxEventDeadLetter` — imutáveis (sem `UPDATED_AT`), sem estender `BaseEntity`, `createdAt` (`@CreationTimestamp`) e `userAt` (nullable, campo direto — diferente do padrão `ProfileResource` onde `userAt` é `NOT NULL`).

`OutboxEventLog`: FK `outboxEvent`, `consumer`, `success` (boolean), `response` (nullable).
`OutboxEventDeadLetter`: FK `outboxEvent` (nullable — payload pode nunca ter virado evento válido), `topic`, `source`, `payload` (texto), `errorType`, `retryCount` (default 0).

#### Scenario: userAt nullable em ambas
- **WHEN** `OutboxEventLog` ou `OutboxEventDeadLetter` são inspecionadas
- **THEN** `userAt` é um campo nullable, sem constraint `NOT NULL` na anotação

#### Scenario: outboxEvent nullable em OutboxEventDeadLetter
- **WHEN** um payload corrompido chega antes de virar um evento válido
- **THEN** `OutboxEventDeadLetter.outboxEvent` é `null`

#### Scenario: Repositórios criados
- **WHEN** o módulo é inspecionado
- **THEN** deve existir `OutboxEventLogRepository` e `OutboxEventDeadLetterRepository`, cada um estendendo `BaseJpaRepository<T, Long>` + `JpaSpecificationExecutor<T>` + `QuerydslPredicateExecutor<T>`
