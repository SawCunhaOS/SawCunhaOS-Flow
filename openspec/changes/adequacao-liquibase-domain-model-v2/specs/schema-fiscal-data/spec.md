## ADDED Requirements

### Requirement: SCOS_LEGAL_NATURE armazena natureza jurídica oficial
O sistema SHALL ter `SCOS_LEGAL_NATURE` com `LEGAL_NATURE_ID BIGINT PK` (identity), `CODE VARCHAR(10) UK NOT NULL`, `DESCRIPTION VARCHAR(255) NOT NULL`, `CREATED_AT TIMESTAMPTZ NOT NULL`. Sem `ACTIVE`, sem `UPDATED_AT`/`USER_AT` — tabela de código oficial (IBGE/Receita Federal), sem conceito de desativação. Sobe **vazia** nesta change; carga dos códigos oficiais é seed separado, fora de escopo.

#### Scenario: Tabela criada sem dados de seed
- **WHEN** `liquibase update` é executado em banco limpo
- **THEN** `SCOS_LEGAL_NATURE` existe com o schema especificado e zero linhas

#### Scenario: CODE único
- **WHEN** dois registros usam o mesmo `CODE`
- **THEN** o segundo INSERT é rejeitado por violação de UK

### Requirement: SCOS_CNAE armazena classificação de atividade econômica oficial
O sistema SHALL ter `SCOS_CNAE` com `CNAE_ID BIGINT PK` (identity), `CODE VARCHAR(10) UK NOT NULL`, `DESCRIPTION VARCHAR(255) NOT NULL`, `CREATED_AT TIMESTAMPTZ NOT NULL`. Mesmo padrão de `SCOS_LEGAL_NATURE` — sem `ACTIVE`, sobe vazia nesta change.

#### Scenario: Tabela criada sem dados de seed
- **WHEN** `liquibase update` é executado em banco limpo
- **THEN** `SCOS_CNAE` existe com o schema especificado e zero linhas

### Requirement: SCOS_COMPANY_CNAE_SECONDARY vincula empresa a CNAEs secundários
O sistema SHALL ter `SCOS_COMPANY_CNAE_SECONDARY` com PK composta `(COMPANY_ID BIGINT FK → SCOS_COMPANY, CNAE_ID BIGINT FK → SCOS_CNAE)`, `CREATED_AT TIMESTAMPTZ NOT NULL`, `USER_AT VARCHAR(255) NOT NULL`. Sem `UPDATED_AT` — o vínculo é criado ou removido, não atualizado. Índice `IDX_COMPANY_CNAE_SECONDARY_REVERSE` em `(CNAE_ID, COMPANY_ID)` para a consulta reversa.

#### Scenario: Mesmo par não pode se repetir
- **WHEN** o mesmo par `(COMPANY_ID, CNAE_ID)` é inserido duas vezes
- **THEN** a segunda inserção falha por violação da PK composta

#### Scenario: Índice reverso criado
- **WHEN** tabela é criada
- **THEN** `IDX_COMPANY_CNAE_SECONDARY_REVERSE` existe em `(CNAE_ID, COMPANY_ID)`
