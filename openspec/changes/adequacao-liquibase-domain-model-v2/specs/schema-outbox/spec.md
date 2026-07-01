## ADDED Requirements

### Requirement: SCOS_OUTBOX_TOPIC define roteamento
O sistema SHALL ter `SCOS_OUTBOX_TOPIC` com `TOPIC VARCHAR(255) PK` (chave natural, sem ID substituto), `BACKEND VARCHAR(20) NOT NULL` restrito a `PGMQ`/`KAFKA`/`DIRECT_API` via `CHECK`, `TARGET_SYSTEM VARCHAR(50) NULL`, `DEFAULT_MAX_RETRIES INT NOT NULL DEFAULT 3`, `ACTIVE BOOLEAN NOT NULL DEFAULT TRUE`, campos de auditoria NOT NULL. `DELETE` SHALL ser bloqueado por trigger — a única forma de remover um tópico da seleção é desativá-lo via `ACTIVE`.

#### Scenario: BACKEND restrito ao vocabulário fechado
- **WHEN** INSERT em `SCOS_OUTBOX_TOPIC` é tentado com `BACKEND = 'RABBITMQ'`
- **THEN** banco rejeita por violação do `CHECK` `CHK_OUTBOX_TOPIC_BACKEND`

#### Scenario: DELETE bloqueado
- **WHEN** `DELETE FROM SCOS_OUTBOX_TOPIC` é tentado em qualquer linha
- **THEN** banco rejeita via trigger `TRG_BLOCK_DELETE_OUTBOX_TOPIC`

### Requirement: SCOS_OUTBOX_EVENT centraliza eventos e tarefas assíncronas
O sistema SHALL ter `SCOS_OUTBOX_EVENT` com `OUTBOX_EVENT_ID BIGINT PK` (identity), `EVENT_HASH CHAR(64) UK NOT NULL` (dedup de inserção), `TOPIC VARCHAR(255) FK NOT NULL → SCOS_OUTBOX_TOPIC`, `AGGREGATE_ID TEXT NOT NULL` (sem FK), `PAYLOAD JSONB NOT NULL`, `RESPONSE_DATA JSONB NULL`, `STATUS VARCHAR(20) NOT NULL DEFAULT 'PENDING'` restrito a `PENDING`/`PROCESSING`/`PROCESSED`/`FAILED` via `CHECK`, `RETRY_COUNT INT NOT NULL DEFAULT 0`, `MAX_RETRIES INT NOT NULL DEFAULT 3`, `MESSAGE VARCHAR(2500) NULL`, `REQUESTING VARCHAR(255) NOT NULL`, `STARTED_AT`/`PROCESSED_AT TIMESTAMPTZ NULL`, `CREATED_AT TIMESTAMPTZ NOT NULL DEFAULT NOW()`. Deve existir índice parcial `(TOPIC, CREATED_AT) WHERE STATUS = 'PENDING'` para o polling de workers consumidores.

#### Scenario: EVENT_HASH único impede duplicata de inserção
- **WHEN** dois INSERTs usam o mesmo `EVENT_HASH`
- **THEN** o segundo é rejeitado por violação da UK

#### Scenario: STATUS restrito ao vocabulário fechado
- **WHEN** INSERT/UPDATE grava `STATUS = 'DONE'`
- **THEN** banco rejeita por violação do `CHECK` `CHK_OUTBOX_EVENT_STATUS`

#### Scenario: Índice de polling cobre busca por pendentes
- **WHEN** tabela é criada
- **THEN** índice parcial `(TOPIC, CREATED_AT) WHERE STATUS = 'PENDING'` existe

### Requirement: SCOS_OUTBOX_EVENT_LOG registra tentativas, imutável
O sistema SHALL ter `SCOS_OUTBOX_EVENT_LOG` com `OUTBOX_EVENT_LOG_ID BIGINT PK`, `OUTBOX_EVENT_ID BIGINT FK NOT NULL → SCOS_OUTBOX_EVENT`, `CONSUMER VARCHAR(255) NOT NULL`, `SUCCESS BOOLEAN NOT NULL`, `RESPONSE VARCHAR(5000) NULL`, `CREATED_AT TIMESTAMPTZ NOT NULL DEFAULT NOW()`, `USER_AT VARCHAR(100) NULL`. Sem `UPDATED_AT` — tabela imutável, apenas inserção. Índice `IDX_OUTBOX_EVENT_LOG_EVENT_ID` em `OUTBOX_EVENT_ID`.

#### Scenario: Uma tentativa por linha
- **WHEN** um evento é reprocessado 3 vezes
- **THEN** existem 3 linhas em `SCOS_OUTBOX_EVENT_LOG` referenciando o mesmo `OUTBOX_EVENT_ID`

#### Scenario: Índice de consulta por evento
- **WHEN** tabela é criada
- **THEN** `IDX_OUTBOX_EVENT_LOG_EVENT_ID` existe em `OUTBOX_EVENT_ID`

### Requirement: SCOS_OUTBOX_EVENT_DEAD_LETTER guarda mensagens não processáveis, imutável
O sistema SHALL ter `SCOS_OUTBOX_EVENT_DEAD_LETTER` com `OUTBOX_EVENT_DEAD_LETTER_ID BIGINT PK`, `OUTBOX_EVENT_ID BIGINT FK NULL → SCOS_OUTBOX_EVENT` (nulo quando o payload nunca chegou a virar um evento válido), `TOPIC VARCHAR(255) NOT NULL`, `SOURCE VARCHAR(255) NOT NULL`, `PAYLOAD TEXT NOT NULL`, `ERROR_TYPE VARCHAR(100) NOT NULL`, `RETRY_COUNT INT NOT NULL DEFAULT 0`, `CREATED_AT TIMESTAMPTZ NOT NULL DEFAULT NOW()`, `USER_AT VARCHAR(100) NULL`. Sem `UPDATED_AT` — imutável. Índice `IDX_OUTBOX_DEAD_LETTER_TOPIC_CREATED` em `(TOPIC, CREATED_AT DESC)`.

#### Scenario: OUTBOX_EVENT_ID nulo é aceito
- **WHEN** um payload corrompido chega antes de virar um `SCOS_OUTBOX_EVENT` válido
- **THEN** o registro em `SCOS_OUTBOX_EVENT_DEAD_LETTER` é aceito com `OUTBOX_EVENT_ID` nulo

#### Scenario: Índice de revisão manual
- **WHEN** tabela é criada
- **THEN** `IDX_OUTBOX_DEAD_LETTER_TOPIC_CREATED` existe em `(TOPIC, CREATED_AT DESC)`
