## Domain Model

```mermaid
erDiagram

%% ─── ORGANIZAÇÃO ────────────────────────────────────────────────────────

    SCOS_DEPARTMENT  ||--o{ SCOS_POSITION         : "0..* positions"
    SCOS_POSITION    ||--o{ SCOS_EMPLOYEE          : "0..* employees"
    SCOS_COMPANY     ||--o{ SCOS_EMPLOYEE          : "0..* employees"
    SCOS_EMPLOYEE    |o--o{ SCOS_EMPLOYEE          : "supervisor"
    SCOS_EMPLOYEE    ||--o{ SCOS_EMPLOYEE_CONTACT  : "0..* contacts"
    SCOS_EMPLOYEE    ||--o{ SCOS_EMPLOYEE_ADDRESS  : "0..* addresses"
    SCOS_COMPANY     ||--o{ SCOS_COMPANY_CONTACT   : "0..* contacts"
    SCOS_COMPANY     ||--o{ SCOS_COMPANY_ADDRESS   : "0..* addresses"
    SCOS_COMPANY     ||--o{ SCOS_COMPANY           : "0..* branches"
    SCOS_POSITION    ||--o{ SCOS_POSITION_WORK_SCHEDULE : "0..* horários (template)"
    SCOS_EMPLOYEE    ||--o{ SCOS_EMPLOYEE_WORK_SCHEDULE : "0..* horários (cópia)"

%% ─── ACESSO E PERMISSÕES ─────────────────────────────────────────────────

    SCOS_PROFILE     ||--o{ SCOS_LOGIN             : "0..* logins (principal)"
    SCOS_EMPLOYEE    |o--o{ SCOS_LOGIN             : "0..* logins"
    SCOS_SYSTEM      ||--o{ SCOS_RESOURCE          : "0..* resources"
    SCOS_PROFILE     ||--o{ SCOS_PROFILE_RESOURCE  : "0..* permissions"
    SCOS_RESOURCE    ||--o{ SCOS_PROFILE_RESOURCE  : "0..* profiles"
    SCOS_LOGIN       ||--o{ SCOS_LOGIN_PROFILE     : "0..* perfis adicionais"
    SCOS_PROFILE     ||--o{ SCOS_LOGIN_PROFILE     : "0..* logins"

%% ─── HISTÓRICO DE CARGO ──────────────────────────────────────────────────

    SCOS_EMPLOYEE               ||--o{ SCOS_EMPLOYEE_POSITION_HISTORY  : "0..* histórico"
    SCOS_POSITION                ||--o{ SCOS_EMPLOYEE_POSITION_HISTORY  : "0..* atribuições"
    SCOS_REASON_POSITION_CHANGE  ||--o{ SCOS_EMPLOYEE_POSITION_HISTORY  : "motivo"

%% ─── OUTBOX E INTEGRAÇÕES EXTERNAS ──────────────────────────────────────

    SCOS_OUTBOX_TOPIC  ||--o{ SCOS_OUTBOX_EVENT              : "0..* eventos"
    SCOS_OUTBOX_EVENT  ||--o{ SCOS_OUTBOX_EVENT_LOG          : "0..* logs"
    SCOS_OUTBOX_EVENT  |o--o{ SCOS_OUTBOX_EVENT_DEAD_LETTER  : "0..* dead letters"

%% ─── HISTÓRICO DE STATUS E MOTIVOS ──────────────────────────────────────

    SCOS_COMPANY            ||--o{ SCOS_COMPANY_STATUS_HISTORY   : "0..* histórico"
    SCOS_EMPLOYEE           ||--o{ SCOS_EMPLOYEE_STATUS_HISTORY  : "0..* histórico"
    SCOS_LOGIN              ||--o{ SCOS_LOGIN_STATUS_HISTORY     : "0..* histórico"
    SCOS_REASON_ACTIVATE    |o--o{ SCOS_COMPANY_STATUS_HISTORY   : "motivo ativação"
    SCOS_REASON_ACTIVATE    |o--o{ SCOS_EMPLOYEE_STATUS_HISTORY  : "motivo ativação"
    SCOS_REASON_ACTIVATE    |o--o{ SCOS_LOGIN_STATUS_HISTORY     : "motivo ativação"
    SCOS_REASON_INACTIVATE  |o--o{ SCOS_COMPANY_STATUS_HISTORY   : "motivo inativação"
    SCOS_REASON_INACTIVATE  |o--o{ SCOS_EMPLOYEE_STATUS_HISTORY  : "motivo inativação"
    SCOS_REASON_INACTIVATE  |o--o{ SCOS_LOGIN_STATUS_HISTORY     : "motivo inativação"
    SCOS_REASON_DISABLE     |o--o{ SCOS_COMPANY_STATUS_HISTORY   : "motivo bloqueio"
    SCOS_REASON_DISABLE     |o--o{ SCOS_EMPLOYEE_STATUS_HISTORY  : "motivo bloqueio"
    SCOS_REASON_DISABLE     |o--o{ SCOS_LOGIN_STATUS_HISTORY     : "motivo bloqueio"
    SCOS_REASON_ENABLE      |o--o{ SCOS_COMPANY_STATUS_HISTORY   : "motivo desbloqueio"
    SCOS_REASON_ENABLE      |o--o{ SCOS_EMPLOYEE_STATUS_HISTORY  : "motivo desbloqueio"
    SCOS_REASON_ENABLE      |o--o{ SCOS_LOGIN_STATUS_HISTORY     : "motivo desbloqueio"

%% ─── DADOS FISCAIS DA EMPRESA ───────────────────────────────────────────

    SCOS_LEGAL_NATURE   |o--o{ SCOS_COMPANY                  : "0..* empresas"
    SCOS_CNAE           |o--o{ SCOS_COMPANY                  : "0..* empresas (CNAE principal)"
    SCOS_COMPANY        ||--o{ SCOS_COMPANY_CNAE_SECONDARY   : "0..* CNAEs secundários"
    SCOS_CNAE            ||--o{ SCOS_COMPANY_CNAE_SECONDARY  : "0..* empresas"

%% ─── TIPOS DE ENDEREÇO E CONTATO ────────────────────────────────────────

    SCOS_ADDRESS_TYPE  ||--o{ SCOS_EMPLOYEE_ADDRESS  : "tipo"
    SCOS_ADDRESS_TYPE  ||--o{ SCOS_COMPANY_ADDRESS   : "tipo"
    SCOS_CONTACT_TYPE  ||--o{ SCOS_EMPLOYEE_CONTACT  : "tipo"
    SCOS_CONTACT_TYPE  ||--o{ SCOS_COMPANY_CONTACT   : "tipo"

%% ─── TABELAS ─────────────────────────────────────────────────────────────

    SCOS_DEPARTMENT {
        BIGINT         DEPARTMENT_ID PK
        VARCHAR(30)    CODE          UK  "NOT NULL"
        VARCHAR(255)   DESCRIPTION       "NOT NULL"
        BOOLEAN        ACTIVE            "NOT NULL DEFAULT TRUE"
        TIMESTAMPTZ  CREATED_AT        "NOT NULL"
        TIMESTAMPTZ  UPDATED_AT        "NOT NULL"
        VARCHAR(255)   USER_AT           "NOT NULL"
    }

    SCOS_POSITION {
        BIGINT         POSITION_ID       PK
        BIGINT         DEPARTMENT_ID     FK  "NOT NULL"
        VARCHAR(30)    CODE              UK  "NOT NULL -- UK composta com DEPARTMENT_ID"
        VARCHAR(255)   DESCRIPTION           "NOT NULL"
        BOOLEAN        IS_TRUST_POSITION     "NOT NULL DEFAULT FALSE -- cargo de confiança"
        BOOLEAN        ACTIVE                "NOT NULL DEFAULT TRUE"
        TIMESTAMPTZ  CREATED_AT            "NOT NULL"
        TIMESTAMPTZ  UPDATED_AT            "NOT NULL"
        VARCHAR(255)   USER_AT               "NOT NULL"
    }

    SCOS_EMPLOYEE {
        BIGINT         EMPLOYEE_ID     PK
        BIGINT         SUPERVISOR_ID   FK  "NULL"
        BIGINT         POSITION_ID     FK  "NOT NULL"
        BIGINT         COMPANY_ID      FK  "NOT NULL"
        VARCHAR(250)   NAME                "NOT NULL"
        VARCHAR(100)   NAME_TREATMENT      "NOT NULL"
        VARCHAR(11)    TAX_IDENTIFIER  UK  "NOT NULL --CPF"
        VARCHAR(255)   EMAIL           UK  "NOT NULL"
        DATE      BIRTH_DATE          "NOT NULL"
        DATE      DATE_OF_HIRING      "NOT NULL"
        VARCHAR(20)    CONTRACT_TYPE       "NOT NULL --CLT/PJ/ESTAGIO/TEMPORARIO"
        DATE      PROBATION_END_DATE      "NULL -- fim do período de experiência"
        TEXT           OBSERVATION
        VARCHAR(20)    STATUS              "NOT NULL --ACTIVE/INACTIVE/DISABLED"
        TIMESTAMPTZ  CREATED_AT          "NOT NULL"
        TIMESTAMPTZ  UPDATED_AT          "NOT NULL"
        VARCHAR(255)   USER_AT             "NOT NULL"
    }

    SCOS_EMPLOYEE_CONTACT {
        BIGINT         EMPLOYEE_ID_CONTACT PK
        BIGINT         EMPLOYEE_ID         FK  "NOT NULL"
        VARCHAR(50)    PHONE               UK  "NOT NULL -- UK composta com EMPLOYEE_ID"
        BIGINT         CONTACT_TYPE_ID     UK,FK  "NOT NULL -- UK composta com EMPLOYEE_ID"
        TIMESTAMPTZ  CREATED_AT              "NOT NULL"
        TIMESTAMPTZ  UPDATED_AT              "NOT NULL"
        VARCHAR(255)   USER_AT                 "NOT NULL"
    }

    SCOS_EMPLOYEE_ADDRESS {
        BIGINT          EMPLOYEE_ID_ADDRESS PK    "external reference -- no FK"
        BIGINT          EMPLOYEE_ID         PK,FK
        BIGINT          ADDRESS_TYPE_ID     UK,FK  "NOT NULL -- UK composta com EMPLOYEE_ID"
        INT             NUMBER                    "NOT NULL"
        VARCHAR(250)    COMPLEMENT
        POINT GEOLOCATION               "NOT NULL"
        TIMESTAMPTZ   CREATED_AT                "NOT NULL"
        TIMESTAMPTZ   UPDATED_AT                "NOT NULL"
        VARCHAR(255)    USER_AT                   "NOT NULL"
    }

    SCOS_COMPANY {
        BIGINT         COMPANY_ID         PK
        BIGINT         PARENT_COMPANY_ID  FK  "NULL --NULL = matriz"
        VARCHAR(250)   NAME                   "NOT NULL"
        VARCHAR(100)   NAME_TREATMENT         "NOT NULL"
        VARCHAR(14)    TAX_IDENTIFIER     UK  "NOT NULL --CNPJ"
        BIGINT         LEGAL_NATURE_ID    FK  "NULL"
        BIGINT         CNAE_PRINCIPAL_ID  FK  "NULL"
        VARCHAR(20)    STATE_REGISTRATION     "NULL --Inscrição Estadual, aceita ISENTO"
        VARCHAR(20)    MUNICIPAL_REGISTRATION "NULL --Inscrição Municipal"
        DATE      FOUNDATION_DATE        "NOT NULL"
        VARCHAR(100)   SECTOR_OF_ACTIVITY     "NOT NULL"
        TEXT           OBSERVATION
        VARCHAR(20)    STATUS                 "NOT NULL --ACTIVE/INACTIVE/DISABLED"
        TIMESTAMPTZ  CREATED_AT             "NOT NULL"
        TIMESTAMPTZ  UPDATED_AT             "NOT NULL"
        VARCHAR(255)   USER_AT                "NOT NULL"
    }

    SCOS_COMPANY_ADDRESS {
        BIGINT          COMPANY_ID_ADDRESS PK    "external reference -- no FK"
        BIGINT          COMPANY_ID         PK,FK
        BIGINT          ADDRESS_TYPE_ID    UK,FK  "NOT NULL -- UK composta com COMPANY_ID"
        INT             NUMBER                   "NOT NULL"
        VARCHAR(250)    COMPLEMENT               "NULL"
        POINT GEOLOCATION              "NOT NULL"
        TIMESTAMPTZ   CREATED_AT               "NOT NULL"
        TIMESTAMPTZ   UPDATED_AT               "NOT NULL"
        VARCHAR(255)    USER_AT                  "NOT NULL"
    }

    SCOS_COMPANY_CONTACT {
        BIGINT         COMPANY_ID_CONTACT  PK
        BIGINT         COMPANY_ID          FK  "NOT NULL"
        VARCHAR(50)    PHONE               UK  "NOT NULL -- UK composta com COMPANY_ID"
        VARCHAR(255)   EMAIL               UK  "NOT NULL -- UK composta com COMPANY_ID"
        BIGINT         CONTACT_TYPE_ID     UK,FK  "NOT NULL -- UK composta com COMPANY_ID"
        VARCHAR(255)   RESPONSIBLE_PERSON      "NOT NULL"
        TIMESTAMPTZ  CREATED_AT              "NOT NULL"
        TIMESTAMPTZ  UPDATED_AT              "NOT NULL"
        VARCHAR(255)   USER_AT                 "NOT NULL"
    }

    SCOS_PROFILE {
        BIGINT         PROFILE_ID  PK
        VARCHAR(30)    CODE        UK  "NOT NULL"
        VARCHAR(255)   DESCRIPTION     "NOT NULL"
        BOOLEAN        ACTIVE          "NOT NULL DEFAULT TRUE"
        TIMESTAMPTZ  CREATED_AT      "NOT NULL"
        TIMESTAMPTZ  UPDATED_AT      "NOT NULL"
        VARCHAR(255)   USER_AT         "NOT NULL"
    }

    SCOS_LOGIN {
        BIGINT         LOGIN_ID     PK
        BIGINT         PROFILE_ID   FK  "NOT NULL"
        BIGINT         EMPLOYEE_ID  FK  "NULL"
        UUID           EXTERNAL_ID      "NULL"
        VARCHAR(255)   LOGIN            "NOT NULL"
        VARCHAR(50)    STATUS           "NOT NULL --ACTIVE/INACTIVE/BLOCKED"
        VARCHAR(50)    TYPE             "NOT NULL --EMPLOYEE/EXTERNAL/SERVICE"
        TIMESTAMPTZ  LAST_USED_AT     "NULL"
        TIMESTAMPTZ  CREATED_AT       "NOT NULL"
        TIMESTAMPTZ  UPDATED_AT       "NOT NULL"
        VARCHAR(255)   USER_AT          "NOT NULL"
    }

    SCOS_CONFIGURATION {
        VARCHAR(50)    CONFIGURATION_ID PK
        TEXT           VALUE               "NOT NULL"
        VARCHAR(50)    TYPE                "NOT NULL"
        TIMESTAMPTZ  CREATED_AT          "NOT NULL"
        TIMESTAMPTZ  UPDATED_AT          "NOT NULL"
        VARCHAR(255)   USER_AT             "NOT NULL"
    }

    SCOS_SYSTEM {
        UUID           SYSTEM_ID    PK
        VARCHAR(250)   NAME             "NOT NULL"
        VARCHAR(25)    CODE         UK  "NOT NULL"
        VARCHAR(200)   DESCRIPTION      "NOT NULL"
        TEXT           SECRET_KEY       "NOT NULL"
        VARCHAR(50)    STATUS           "NOT NULL --ACTIVE/INACTIVE"
        VARCHAR(50)    VERSION          "NOT NULL"
        TIMESTAMPTZ    CREATED_AT       "NOT NULL"
        TIMESTAMPTZ    UPDATED_AT       "NOT NULL"
        VARCHAR(255)   USER_AT          "NOT NULL"
    }

    SCOS_RESOURCE {
        UUID           RESOURCE_ID  PK
        UUID           SYSTEM_ID    FK  "NOT NULL"
        VARCHAR(50)    CODE         UK  "NOT NULL -- UK composta com SYSTEM_ID"
        VARCHAR(255)   DESCRIPTION_PT   "NOT NULL"
        VARCHAR(255)   DESCRIPTION_EN   "NOT NULL"
        BOOLEAN        ACTIVE           "NOT NULL DEFAULT TRUE"
        TIMESTAMPTZ    CREATED_AT       "NOT NULL"
        TIMESTAMPTZ    UPDATED_AT       "NOT NULL"
        VARCHAR(255)   USER_AT          "NOT NULL"
    }

    SCOS_PROFILE_RESOURCE {
        BIGINT         PROFILE_ID   PK,FK  "NOT NULL"
        UUID           RESOURCE_ID  PK,FK  "NOT NULL"
        TIMESTAMPTZ  CREATED_AT          "NOT NULL"
        VARCHAR(255)   USER_AT             "NOT NULL"
    }

    SCOS_OUTBOX_TOPIC {
        VARCHAR(255)   TOPIC                PK
        VARCHAR(20)    BACKEND                  "NOT NULL --PGMQ/KAFKA/DIRECT_API"
        VARCHAR(50)    TARGET_SYSTEM            "NULL -- obrigatório apenas quando BACKEND=DIRECT_API"
        INT            DEFAULT_MAX_RETRIES      "NOT NULL DEFAULT 3"
        BOOLEAN        ACTIVE                   "NOT NULL DEFAULT TRUE"
        TIMESTAMPTZ    CREATED_AT               "NOT NULL"
        TIMESTAMPTZ    UPDATED_AT               "NOT NULL"
        VARCHAR(255)   USER_AT                  "NOT NULL"
    }

    SCOS_OUTBOX_EVENT {
        BIGINT         OUTBOX_EVENT_ID  PK
        CHAR(64)       EVENT_HASH       UK  "NOT NULL -- dedup de inserção"
        VARCHAR(255)   TOPIC            FK  "NOT NULL"
        TEXT           AGGREGATE_ID         "NOT NULL -- ID da entidade de origem"
        JSONB          PAYLOAD              "NOT NULL"
        JSONB          RESPONSE_DATA        "NULL -- resposta do destino, quando BACKEND=DIRECT_API"
        VARCHAR(20)    STATUS               "NOT NULL DEFAULT PENDING --PENDING/PROCESSING/PROCESSED/FAILED"
        INT            RETRY_COUNT          "NOT NULL DEFAULT 0"
        INT            MAX_RETRIES          "NOT NULL DEFAULT 3"
        VARCHAR(2500)  MESSAGE              "NULL -- descrição do resultado/erro"
        VARCHAR(255)   REQUESTING           "NOT NULL -- quem/o que originou o evento"
        TIMESTAMPTZ    STARTED_AT           "NULL"
        TIMESTAMPTZ    PROCESSED_AT         "NULL"
        TIMESTAMPTZ    CREATED_AT           "NOT NULL DEFAULT NOW()"
        TIMESTAMPTZ    UPDATED_AT           "NULL"
        VARCHAR(255)   USER_AT              "NULL"
    }

    SCOS_OUTBOX_EVENT_LOG {
        BIGINT         OUTBOX_EVENT_LOG_ID  PK
        BIGINT         OUTBOX_EVENT_ID      FK  "NOT NULL"
        VARCHAR(255)   CONSUMER                 "NOT NULL -- ex: KEYCLOAK, SISTEMA_PONTO, PGMQ_RELAY"
        BOOLEAN        SUCCESS                  "NOT NULL"
        VARCHAR(5000)  RESPONSE                 "NULL"
        TIMESTAMPTZ    CREATED_AT               "NOT NULL DEFAULT NOW() --imutável"
        VARCHAR(100)   USER_AT                  "NULL"
    }

    SCOS_OUTBOX_EVENT_DEAD_LETTER {
        BIGINT         OUTBOX_EVENT_DEAD_LETTER_ID  PK
        BIGINT         OUTBOX_EVENT_ID              FK  "NULL -- NULL se o payload chegou corrompido/ilegível"
        VARCHAR(255)   TOPIC                            "NOT NULL"
        VARCHAR(255)   SOURCE                           "NOT NULL -- ex: DIRECT_API:KEYCLOAK, PGMQ:employee_events"
        TEXT           PAYLOAD                          "NOT NULL"
        VARCHAR(100)   ERROR_TYPE                       "NOT NULL -- ex: MAX_RETRIES_EXCEEDED, VALIDATION, TIMEOUT"
        INT            RETRY_COUNT                      "NOT NULL DEFAULT 0"
        TIMESTAMPTZ    CREATED_AT                       "NOT NULL DEFAULT NOW() --imutável"
        VARCHAR(100)   USER_AT                          "NULL"
    }

    SCOS_REASON_ACTIVATE {
        BIGINT         REASON_ACTIVATE_ID  PK
        VARCHAR(30)    CODE                UK  "NOT NULL -- UK composta com ENTITY_TYPE"
        VARCHAR(255)   DESCRIPTION             "NOT NULL"
        VARCHAR(20)    ENTITY_TYPE         UK  "NOT NULL --COMPANY/EMPLOYEE/LOGIN"
        BOOLEAN        ACTIVE                  "NOT NULL DEFAULT TRUE"
        TIMESTAMPTZ    CREATED_AT              "NOT NULL"
        TIMESTAMPTZ    UPDATED_AT              "NOT NULL"
        VARCHAR(255)   USER_AT                 "NOT NULL"
    }

    SCOS_REASON_INACTIVATE {
        BIGINT         REASON_INACTIVATE_ID  PK
        VARCHAR(30)    CODE                   UK  "NOT NULL -- UK composta com ENTITY_TYPE"
        VARCHAR(255)   DESCRIPTION                "NOT NULL"
        VARCHAR(20)    ENTITY_TYPE            UK  "NOT NULL --COMPANY/EMPLOYEE/LOGIN"
        BOOLEAN        ACTIVE                     "NOT NULL DEFAULT TRUE"
        TIMESTAMPTZ    CREATED_AT                 "NOT NULL"
        TIMESTAMPTZ    UPDATED_AT                 "NOT NULL"
        VARCHAR(255)   USER_AT                    "NOT NULL"
    }

    SCOS_REASON_DISABLE {
        BIGINT         REASON_DISABLE_ID  PK
        VARCHAR(30)    CODE               UK  "NOT NULL -- UK composta com ENTITY_TYPE"
        VARCHAR(255)   DESCRIPTION            "NOT NULL"
        VARCHAR(20)    ENTITY_TYPE        UK  "NOT NULL --COMPANY/EMPLOYEE/LOGIN"
        BOOLEAN        ACTIVE                 "NOT NULL DEFAULT TRUE"
        TIMESTAMPTZ    CREATED_AT             "NOT NULL"
        TIMESTAMPTZ    UPDATED_AT             "NOT NULL"
        VARCHAR(255)   USER_AT                "NOT NULL"
    }

    SCOS_REASON_ENABLE {
        BIGINT         REASON_ENABLE_ID  PK
        VARCHAR(30)    CODE              UK  "NOT NULL -- UK composta com ENTITY_TYPE"
        VARCHAR(255)   DESCRIPTION           "NOT NULL"
        VARCHAR(20)    ENTITY_TYPE       UK  "NOT NULL --COMPANY/EMPLOYEE/LOGIN"
        BOOLEAN        ACTIVE                "NOT NULL DEFAULT TRUE"
        TIMESTAMPTZ    CREATED_AT            "NOT NULL"
        TIMESTAMPTZ    UPDATED_AT            "NOT NULL"
        VARCHAR(255)   USER_AT               "NOT NULL"
    }

    SCOS_COMPANY_STATUS_HISTORY {
        BIGINT         COMPANY_STATUS_HISTORY_ID  PK
        BIGINT         COMPANY_ID                  FK  "NOT NULL"
        VARCHAR(20)    STATUS                          "NOT NULL --ACTIVE/INACTIVE/DISABLED"
        VARCHAR(20)    PREVIOUS_STATUS                 "NULL -- preenchido por trigger"
        BIGINT         REASON_ACTIVATE_ID          FK  "NULL"
        BIGINT         REASON_INACTIVATE_ID        FK  "NULL"
        BIGINT         REASON_DISABLE_ID           FK  "NULL"
        BIGINT         REASON_ENABLE_ID            FK  "NULL"
        TEXT           OBSERVATION                     "NULL"
        TIMESTAMPTZ    CREATED_AT                      "NOT NULL"
        VARCHAR(255)   USER_AT                         "NOT NULL"
    }

    SCOS_EMPLOYEE_STATUS_HISTORY {
        BIGINT         EMPLOYEE_STATUS_HISTORY_ID  PK
        BIGINT         EMPLOYEE_ID                  FK  "NOT NULL"
        VARCHAR(20)    STATUS                           "NOT NULL --ACTIVE/INACTIVE/DISABLED"
        VARCHAR(20)    PREVIOUS_STATUS                  "NULL -- preenchido por trigger"
        BIGINT         REASON_ACTIVATE_ID           FK  "NULL"
        BIGINT         REASON_INACTIVATE_ID         FK  "NULL"
        BIGINT         REASON_DISABLE_ID            FK  "NULL"
        BIGINT         REASON_ENABLE_ID             FK  "NULL"
        TEXT           OBSERVATION                      "NULL"
        TIMESTAMPTZ    CREATED_AT                       "NOT NULL"
        VARCHAR(255)   USER_AT                          "NOT NULL"
    }

    SCOS_LOGIN_STATUS_HISTORY {
        BIGINT         LOGIN_STATUS_HISTORY_ID  PK
        BIGINT         LOGIN_ID                  FK  "NOT NULL"
        VARCHAR(20)    STATUS                        "NOT NULL --ACTIVE/INACTIVE/BLOCKED"
        VARCHAR(20)    PREVIOUS_STATUS               "NULL -- preenchido por trigger"
        BIGINT         REASON_ACTIVATE_ID        FK  "NULL"
        BIGINT         REASON_INACTIVATE_ID      FK  "NULL"
        BIGINT         REASON_DISABLE_ID         FK  "NULL"
        BIGINT         REASON_ENABLE_ID          FK  "NULL"
        TEXT           OBSERVATION                   "NULL"
        TIMESTAMPTZ    CREATED_AT                    "NOT NULL"
        VARCHAR(255)   USER_AT                       "NOT NULL"
    }

    SCOS_LEGAL_NATURE {
        BIGINT         LEGAL_NATURE_ID  PK
        VARCHAR(10)    CODE             UK  "NOT NULL -- ex: 206-2"
        VARCHAR(255)   DESCRIPTION          "NOT NULL"
        TIMESTAMPTZ    CREATED_AT           "NOT NULL"
    }

    SCOS_CNAE {
        BIGINT         CNAE_ID      PK
        VARCHAR(10)    CODE         UK  "NOT NULL -- ex: 6201-5/01"
        VARCHAR(255)   DESCRIPTION      "NOT NULL"
        TIMESTAMPTZ    CREATED_AT       "NOT NULL"
    }

    SCOS_COMPANY_CNAE_SECONDARY {
        BIGINT         COMPANY_ID  PK,FK
        BIGINT         CNAE_ID     PK,FK
        TIMESTAMPTZ    CREATED_AT      "NOT NULL"
        VARCHAR(255)   USER_AT         "NOT NULL"
    }

    SCOS_ADDRESS_TYPE {
        BIGINT         ADDRESS_TYPE_ID  PK
        VARCHAR(30)    CODE             UK  "NOT NULL -- UK composta com ENTITY_TYPE"
        VARCHAR(255)   DESCRIPTION          "NOT NULL"
        VARCHAR(20)    ENTITY_TYPE      UK  "NOT NULL --COMPANY/EMPLOYEE"
        BOOLEAN        ACTIVE               "NOT NULL DEFAULT TRUE"
        TIMESTAMPTZ    CREATED_AT           "NOT NULL"
        TIMESTAMPTZ    UPDATED_AT           "NOT NULL"
        VARCHAR(255)   USER_AT              "NOT NULL"
    }

    SCOS_CONTACT_TYPE {
        BIGINT         CONTACT_TYPE_ID  PK
        VARCHAR(30)    CODE             UK  "NOT NULL -- UK composta com ENTITY_TYPE"
        VARCHAR(255)   DESCRIPTION          "NOT NULL"
        VARCHAR(20)    ENTITY_TYPE      UK  "NOT NULL --COMPANY/EMPLOYEE"
        BOOLEAN        ACTIVE               "NOT NULL DEFAULT TRUE"
        TIMESTAMPTZ    CREATED_AT           "NOT NULL"
        TIMESTAMPTZ    UPDATED_AT           "NOT NULL"
        VARCHAR(255)   USER_AT              "NOT NULL"
    }

    SCOS_LOGIN_PROFILE {
        BIGINT         LOGIN_ID    PK,FK
        BIGINT         PROFILE_ID  PK,FK
        TIMESTAMPTZ    CREATED_AT      "NOT NULL"
        VARCHAR(255)   USER_AT         "NOT NULL"
    }

    SCOS_REASON_POSITION_CHANGE {
        BIGINT         REASON_POSITION_CHANGE_ID  PK
        VARCHAR(30)    CODE                        UK  "NOT NULL"
        VARCHAR(255)   DESCRIPTION                     "NOT NULL"
        BOOLEAN        ACTIVE                          "NOT NULL DEFAULT TRUE"
        TIMESTAMPTZ    CREATED_AT                      "NOT NULL"
        TIMESTAMPTZ    UPDATED_AT                      "NOT NULL"
        VARCHAR(255)   USER_AT                         "NOT NULL"
    }

    SCOS_EMPLOYEE_POSITION_HISTORY {
        BIGINT         EMPLOYEE_POSITION_HISTORY_ID  PK
        BIGINT         EMPLOYEE_ID                    FK  "NOT NULL"
        BIGINT         POSITION_ID                    FK  "NOT NULL"
        DATE           START_DATE                         "NOT NULL"
        DATE           END_DATE                           "NULL -- NULL = cargo atual"
        BIGINT         REASON_POSITION_CHANGE_ID      FK  "NOT NULL"
        TIMESTAMPTZ    CREATED_AT                         "NOT NULL"
        VARCHAR(255)   USER_AT                            "NOT NULL"
    }

    SCOS_POSITION_WORK_SCHEDULE {
        BIGINT         POSITION_WORK_SCHEDULE_ID  PK
        BIGINT         POSITION_ID                 UK,FK  "NOT NULL -- UK composta com DAY_OF_WEEK"
        VARCHAR(10)    DAY_OF_WEEK                 UK     "NOT NULL -- UK composta com POSITION_ID"
        TIME           START_TIME                         "NOT NULL"
        TIME           LUNCH_START                        "NOT NULL"
        TIME           LUNCH_END                          "NOT NULL"
        TIME           END_TIME                           "NOT NULL"
        TIMESTAMPTZ    CREATED_AT                         "NOT NULL"
        TIMESTAMPTZ    UPDATED_AT                         "NOT NULL"
        VARCHAR(255)   USER_AT                            "NOT NULL"
    }

    SCOS_EMPLOYEE_WORK_SCHEDULE {
        BIGINT         EMPLOYEE_WORK_SCHEDULE_ID  PK
        BIGINT         EMPLOYEE_ID                 UK,FK  "NOT NULL -- UK composta com DAY_OF_WEEK"
        VARCHAR(10)    DAY_OF_WEEK                 UK     "NOT NULL -- UK composta com EMPLOYEE_ID"
        TIME           START_TIME                         "NOT NULL"
        TIME           LUNCH_START                        "NOT NULL"
        TIME           LUNCH_END                          "NOT NULL"
        TIME           END_TIME                           "NOT NULL"
        TIMESTAMPTZ    CREATED_AT                         "NOT NULL"
        TIMESTAMPTZ    UPDATED_AT                         "NOT NULL"
        VARCHAR(255)   USER_AT                            "NOT NULL"
    }
```

---

## Descrição das Tabelas

---

### SCOS_DEPARTMENT

**Descrição:** Representa os departamentos da empresa. Um departamento agrupa cargos relacionados a uma mesma área de atuação, como Recursos Humanos, Tecnologia ou Financeiro.

| Campo | Tipo | Restrição | Descrição |
|---|---|---|---|
| DEPARTMENT_ID | BIGINT | PK | Identificador único do departamento, gerado automaticamente |
| CODE | VARCHAR(30) | UK, NOT NULL | Código único do departamento — ex: `RH`, `TI`, `FIN` |
| DESCRIPTION | VARCHAR(255) | NOT NULL | Descrição completa do departamento |
| ACTIVE | BOOLEAN | NOT NULL, DEFAULT TRUE | Indica se o departamento está ativo. Departamentos inativos mantêm o histórico mas não aparecem para seleção |
| CREATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora em que o registro foi criado |
| UPDATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora da última atualização do registro |
| USER_AT | VARCHAR(255) | NOT NULL | Login do usuário que realizou a última alteração |

---

### SCOS_POSITION

**Descrição:** Representa os cargos existentes dentro de cada departamento. Um cargo define a função que um funcionário exerce. Cada cargo pertence a exatamente um departamento.

| Campo | Tipo | Restrição | Descrição |
|---|---|---|---|
| POSITION_ID | BIGINT | PK | Identificador único do cargo, gerado automaticamente |
| DEPARTMENT_ID | BIGINT | FK, NOT NULL | Referência ao departamento ao qual este cargo pertence |
| CODE | VARCHAR(30) | UK*, NOT NULL | Código do cargo — ex: `ANALISTA_SR`, `GERENTE_TI`. *UK composta com `DEPARTMENT_ID` — mesmo código pode existir em departamentos diferentes |
| DESCRIPTION | VARCHAR(255) | NOT NULL | Descrição completa do cargo |
| IS_TRUST_POSITION | BOOLEAN | NOT NULL, DEFAULT FALSE | Indica se é cargo de confiança — afeta regras trabalhistas como isenção de controle de jornada |
| ACTIVE | BOOLEAN | NOT NULL, DEFAULT TRUE | Indica se o cargo está ativo. Cargos inativos não podem ser atribuídos a novos funcionários |
| CREATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora em que o registro foi criado |
| UPDATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora da última atualização do registro |
| USER_AT | VARCHAR(255) | NOT NULL | Login do usuário que realizou a última alteração |

---

### SCOS_POSITION_WORK_SCHEDULE

**Descrição:** Horário de trabalho **template** de um cargo — existe só pra ser copiado pro funcionário (`SCOS_EMPLOYEE_WORK_SCHEDULE`).

| Campo | Tipo | Restrição | Descrição |
|---|---|---|---|
| POSITION_WORK_SCHEDULE_ID | BIGINT | PK | Identificador único do registro |
| POSITION_ID | BIGINT | UK*, FK, NOT NULL | Referência ao cargo. *UK composta com `DAY_OF_WEEK` — um cargo não pode ter duas linhas pro mesmo dia |
| DAY_OF_WEEK | VARCHAR(10) | UK*, NOT NULL | Dia da semana — `MONDAY` a `SUNDAY`. *UK composta com `POSITION_ID` |
| START_TIME | TIME | NOT NULL | Horário de entrada |
| LUNCH_START | TIME | NOT NULL | Início do intervalo de almoço |
| LUNCH_END | TIME | NOT NULL | Fim do intervalo de almoço |
| END_TIME | TIME | NOT NULL | Horário de saída |
| CREATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora em que o registro foi criado |
| UPDATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora da última atualização |
| USER_AT | VARCHAR(255) | NOT NULL | Login do usuário que realizou a última alteração |

**Restrição de integridade:** dentro do mesmo dia, a ordem precisa ser `START_TIME < LUNCH_START < LUNCH_END < END_TIME`.

> **Check:** `CHK_POSITION_WORK_SCHEDULE_DAY` trava `DAY_OF_WEEK` ao vocabulário fechado (ver seção Checks de Domínio)

---

### SCOS_EMPLOYEE

**Descrição:** Representa os funcionários da empresa. Sempre vinculado a um cargo e a uma empresa ou filial. O campo `SUPERVISOR_ID` é uma auto-referência que monta a hierarquia organizacional — o funcionário no topo não possui supervisor e o campo fica nulo.

| Campo | Tipo | Restrição | Descrição |
|---|---|---|---|
| EMPLOYEE_ID | BIGINT | PK | Identificador único do funcionário, gerado automaticamente |
| SUPERVISOR_ID | BIGINT | FK, NULL | Referência ao funcionário supervisor direto. Nulo indica o topo da hierarquia |
| POSITION_ID | BIGINT | FK, NOT NULL | Cargo atual do funcionário. Cache mantido por trigger a partir de `SCOS_EMPLOYEE_POSITION_HISTORY` — não atualizar diretamente, inserir no histórico |
| COMPANY_ID | BIGINT | FK, NOT NULL | Referência à empresa ou filial onde o funcionário trabalha |
| NAME | VARCHAR(250) | NOT NULL | Nome completo conforme documento oficial |
| NAME_TREATMENT | VARCHAR(100) | NOT NULL | Nome de tratamento — nome preferido ou apelido usado no dia a dia |
| TAX_IDENTIFIER | VARCHAR(11) | UK, NOT NULL | CPF do funcionário — identificador fiscal único |
| EMAIL | VARCHAR(255) | UK, NOT NULL | E-mail corporativo do funcionário |
| BIRTH_DATE | DATE | NOT NULL | Data de nascimento |
| DATE_OF_HIRING | DATE | NOT NULL | Data de contratação — início do vínculo empregatício. Pode divergir do `START_DATE` da primeira linha em `SCOS_EMPLOYEE_POSITION_HISTORY` (ex: contratado numa data, assume o cargo formalmente depois) — são conceitos distintos, não redundantes |
| CONTRACT_TYPE | VARCHAR(20) | NOT NULL | Tipo de vínculo: `CLT`, `PJ`, `ESTAGIO` ou `TEMPORARIO` |
| PROBATION_END_DATE | DATE | NULL | Fim do período de experiência. Nulo se não aplicável ao tipo de contrato (ex: `PJ`) ou se o período já foi efetivado/encerrado. Não há campo de início — é `DATE_OF_HIRING` |
| OBSERVATION | TEXT | NULL | Observações gerais sobre o funcionário. Campo livre |
| STATUS | VARCHAR(20) | NOT NULL | Situação atual. Cache mantido por trigger a partir de `SCOS_EMPLOYEE_STATUS_HISTORY` — não atualizar diretamente, inserir no histórico |
| CREATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora em que o registro foi criado |
| UPDATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora da última atualização do registro |
| USER_AT | VARCHAR(255) | NOT NULL | Login do usuário que realizou a última alteração |

**Restrição de integridade:** quando preenchido, `PROBATION_END_DATE` deve ser posterior a `DATE_OF_HIRING`.

**Recontratação (rehire):** `TAX_IDENTIFIER` e `EMAIL` continuam com `UK` de coluna inteira — nenhuma mudança de schema aqui. A recontratação **reativa a linha `INACTIVE` existente** (mesmo `EMPLOYEE_ID`, mesmo CPF/e-mail) em vez de inserir um funcionário novo, então a unicidade nunca é violada — quem resolve isso é a camada de aplicação (endpoint dedicado de rehire, ver `ScosOrganization_Employee.yml`), não o banco. `POSITION_ID`, `COMPANY_ID` e `CONTRACT_TYPE` podem ser atualizados nesse fluxo (a pessoa pode voltar em condições diferentes) — isso é um `UPDATE` simples na linha existente, mais uma nova linha em `EMPLOYEE_POSITION_HISTORY` (mecanismo já existente, `TRG_CLOSE_PREVIOUS_POSITION` fecha a linha anterior automaticamente) e uma nova linha em `EMPLOYEE_STATUS_HISTORY` (`INACTIVE → ACTIVE`, via `SCOS_REASON_ACTIVATE`).

> **Check:** `CHK_EMPLOYEE_STATUS` trava `STATUS` e `CHK_EMPLOYEE_CONTRACT_TYPE` trava `CONTRACT_TYPE`, ambos ao vocabulário fechado (ver seção Checks de Domínio)

---

### SCOS_REASON_POSITION_CHANGE

**Descrição:** Motivos pré-cadastrados para mudança de cargo. Diferente das 4 tabelas de motivo de status, não tem `ENTITY_TYPE` — só se aplica a `EMPLOYEE`, não faz sentido compartilhar com `COMPANY`/`LOGIN`. Padronizada com as demais: `UPDATED_AT` e proteção contra `DELETE`.

| Campo | Tipo | Restrição | Descrição |
|---|---|---|---|
| REASON_POSITION_CHANGE_ID | BIGINT | PK | Identificador único do motivo, gerado automaticamente |
| CODE | VARCHAR(30) | UK, NOT NULL | Código do motivo — ex: `PROMOTION`, `TRANSFER`, `NEW_HIRE` |
| DESCRIPTION | VARCHAR(255) | NOT NULL | Descrição legível do motivo |
| ACTIVE | BOOLEAN | NOT NULL, DEFAULT TRUE | Indica se o motivo está disponível para seleção |
| CREATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora em que o registro foi criado |
| UPDATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora da última atualização |
| USER_AT | VARCHAR(255) | NOT NULL | Login do usuário que cadastrou o motivo |

> **Trigger:** `TRG_BLOCK_DELETE_REASON_POSITION_CHANGE` — bloqueia `DELETE` (ver seção Triggers)

---

### SCOS_EMPLOYEE_POSITION_HISTORY

**Descrição:** Histórico completo de cargos do funcionário. `END_DATE = NULL` identifica a linha do cargo atual; fechada automaticamente por trigger quando uma nova linha é inserida (`END_DATE = NEW.START_DATE`), a aplicação não seta isso manualmente. Diferente de `*_STATUS_HISTORY`, não tem conceito de "transição reversível" (não existe `PREVIOUS_POSITION`/`CHECK` de 6 ramos) — é uma linha do tempo simples, um registro por período no cargo. Tabela imutável.

| Campo | Tipo | Restrição | Descrição |
|---|---|---|---|
| EMPLOYEE_POSITION_HISTORY_ID | BIGINT | PK | Identificador único do registro |
| EMPLOYEE_ID | BIGINT | FK, NOT NULL | Referência ao funcionário |
| POSITION_ID | BIGINT | FK, NOT NULL | Cargo atribuído neste período |
| START_DATE | DATE | NOT NULL | Início da vigência neste cargo |
| END_DATE | DATE | NULL | Fim da vigência. `NULL` = cargo atual — preenchido por trigger quando a próxima linha é inserida |
| REASON_POSITION_CHANGE_ID | BIGINT | FK, NOT NULL | Motivo da mudança |
| CREATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora em que o registro foi criado |
| USER_AT | VARCHAR(255) | NOT NULL | Login do usuário que realizou a mudança |

**Restrições de integridade:** `END_DATE`, quando preenchido, precisa ser posterior a `START_DATE`. Também existe um índice único parcial em `EMPLOYEE_ID` filtrado por `END_DATE IS NULL` — garante a nível de banco que um funcionário nunca tenha duas linhas "abertas" simultaneamente, mesmo que a trigger de fechamento seja bypassada por algum motivo.

> **Trigger:** `TRG_CLOSE_PREVIOUS_POSITION` fecha a linha anterior; `TRG_SYNC_EMPLOYEE_POSITION` sincroniza `SCOS_EMPLOYEE.POSITION_ID` (ver seção Triggers)

---

### SCOS_EMPLOYEE_WORK_SCHEDULE

**Descrição:** Horário de trabalho efetivo do funcionário — a fonte de verdade, independente do cargo depois de gravada. Populada por cópia do template do cargo (`SCOS_POSITION_WORK_SCHEDULE`), ação da aplicação, não automática. A partir daí é editável livremente, sem relação com o cargo — mudar o template do cargo depois não afeta quem já tem linha própria aqui. Dia sem linha aqui significa **não definido** — não é fallback pro cargo nem "segue o padrão", é ausência de dado mesmo. Sem histórico: mudança sobrescreve via `UPDATE`.

| Campo | Tipo | Restrição | Descrição |
|---|---|---|---|
| EMPLOYEE_WORK_SCHEDULE_ID | BIGINT | PK | Identificador único do registro |
| EMPLOYEE_ID | BIGINT | UK*, FK, NOT NULL | Referência ao funcionário. *UK composta com `DAY_OF_WEEK` |
| DAY_OF_WEEK | VARCHAR(10) | UK*, NOT NULL | Dia da semana — `MONDAY` a `SUNDAY`. *UK composta com `EMPLOYEE_ID` |
| START_TIME | TIME | NOT NULL | Horário de entrada |
| LUNCH_START | TIME | NOT NULL | Início do intervalo de almoço |
| LUNCH_END | TIME | NOT NULL | Fim do intervalo de almoço |
| END_TIME | TIME | NOT NULL | Horário de saída |
| CREATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora em que o registro foi criado |
| UPDATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora da última atualização |
| USER_AT | VARCHAR(255) | NOT NULL | Login do usuário que realizou a última alteração |

**Restrição de integridade:** mesma regra de `SCOS_POSITION_WORK_SCHEDULE` — `START_TIME < LUNCH_START < LUNCH_END < END_TIME`.

> **Check:** `CHK_EMPLOYEE_WORK_SCHEDULE_DAY` trava `DAY_OF_WEEK` ao vocabulário fechado (ver seção Checks de Domínio)

---

### SCOS_ADDRESS_TYPE

**Descrição:** Tabela de referência dinâmica para os tipos de endereço, substituindo os valores fixos em texto livre usados anteriormente em `SCOS_EMPLOYEE_ADDRESS.TYPE` e `SCOS_COMPANY_ADDRESS.TYPE`. Compartilhada entre as duas entidades via `ENTITY_TYPE`. **Registros não podem ser excluídos** — apenas desativados via o campo `ACTIVE`; qualquer tentativa de `DELETE` é bloqueada por trigger (ver seção Triggers).

| Campo | Tipo | Restrição | Descrição |
|---|---|---|---|
| ADDRESS_TYPE_ID | BIGINT | PK | Identificador único do tipo, gerado automaticamente |
| CODE | VARCHAR(30) | UK*, NOT NULL | Código do tipo — ex: `HOME`, `WORK`, `BILLING`, `BRANCH`. *UK composta com `ENTITY_TYPE` |
| DESCRIPTION | VARCHAR(255) | NOT NULL | Descrição legível do tipo |
| ENTITY_TYPE | VARCHAR(20) | UK*, NOT NULL | Entidade à qual o tipo se aplica: `COMPANY` ou `EMPLOYEE` |
| ACTIVE | BOOLEAN | NOT NULL, DEFAULT TRUE | Indica se o tipo está disponível para seleção. Único mecanismo de "remoção" — `DELETE` é bloqueado |
| CREATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora em que o registro foi criado |
| UPDATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora da última atualização (inclui desativação) |
| USER_AT | VARCHAR(255) | NOT NULL | Login do usuário que realizou a última alteração |

> **Índice:** `IDX_ADDRESS_TYPE_ENTITY_TYPE` parcial em `(ENTITY_TYPE) WHERE ACTIVE = TRUE`
> **Trigger:** `TRG_BLOCK_DELETE_ADDRESS_TYPE` — bloqueia `DELETE` (ver seção Triggers)
> **Check:** `CHK_ADDRESS_TYPE_ENTITY_TYPE` trava `ENTITY_TYPE` ao vocabulário fechado (ver seção Checks de Domínio)

---

### SCOS_CONTACT_TYPE

**Descrição:** Tabela de referência dinâmica para os tipos de contato, substituindo os valores fixos em texto livre usados anteriormente em `SCOS_EMPLOYEE_CONTACT.TYPE` e `SCOS_COMPANY_CONTACT.TYPE`. Compartilhada entre as duas entidades via `ENTITY_TYPE`. **Registros não podem ser excluídos** — apenas desativados via o campo `ACTIVE`; qualquer tentativa de `DELETE` é bloqueada por trigger (ver seção Triggers).

| Campo | Tipo | Restrição | Descrição |
|---|---|---|---|
| CONTACT_TYPE_ID | BIGINT | PK | Identificador único do tipo, gerado automaticamente |
| CODE | VARCHAR(30) | UK*, NOT NULL | Código do tipo — ex: `MOBILE`, `WORK`, `COMMERCIAL`, `SUPPORT`. *UK composta com `ENTITY_TYPE` |
| DESCRIPTION | VARCHAR(255) | NOT NULL | Descrição legível do tipo |
| ENTITY_TYPE | VARCHAR(20) | UK*, NOT NULL | Entidade à qual o tipo se aplica: `COMPANY` ou `EMPLOYEE` |
| ACTIVE | BOOLEAN | NOT NULL, DEFAULT TRUE | Indica se o tipo está disponível para seleção. Único mecanismo de "remoção" — `DELETE` é bloqueado |
| CREATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora em que o registro foi criado |
| UPDATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora da última atualização (inclui desativação) |
| USER_AT | VARCHAR(255) | NOT NULL | Login do usuário que realizou a última alteração |

> **Índice:** `IDX_CONTACT_TYPE_ENTITY_TYPE` parcial em `(ENTITY_TYPE) WHERE ACTIVE = TRUE`
> **Trigger:** `TRG_BLOCK_DELETE_CONTACT_TYPE` — bloqueia `DELETE` (ver seção Triggers)
> **Check:** `CHK_CONTACT_TYPE_ENTITY_TYPE` trava `ENTITY_TYPE` ao vocabulário fechado (ver seção Checks de Domínio)

---

### SCOS_EMPLOYEE_CONTACT

**Descrição:** Armazena os contatos telefônicos do funcionário. Um funcionário pode ter múltiplos contatos de diferentes tipos.

| Campo | Tipo | Restrição | Descrição |
|---|---|---|---|
| EMPLOYEE_ID_CONTACT | BIGINT | PK | Identificador único do contato, gerado automaticamente |
| EMPLOYEE_ID | BIGINT | FK, NOT NULL | Referência ao funcionário dono do contato |
| PHONE | VARCHAR(50) | UK*, NOT NULL | Número de telefone com DDD. *UK composta com `EMPLOYEE_ID` — mesmo telefone pode se repetir entre funcionários diferentes |
| CONTACT_TYPE_ID | BIGINT | UK*, FK, NOT NULL | Referência ao tipo de contato (`SCOS_CONTACT_TYPE`, `ENTITY_TYPE = EMPLOYEE`). *UK composta com `EMPLOYEE_ID` |
| CREATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora em que o registro foi criado |
| UPDATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora da última atualização do registro |
| USER_AT | VARCHAR(255) | NOT NULL | Login do usuário que realizou a última alteração |

> **Trigger:** `TRG_VALIDATE_EMPLOYEE_CONTACT_TYPE` — valida `ENTITY_TYPE = EMPLOYEE` em `CONTACT_TYPE_ID` (ver seção Triggers)

---

### SCOS_EMPLOYEE_ADDRESS

**Descrição:** Armazena os endereços do funcionário por referência a um sistema externo. O ID do endereço vem de fora — não é gerado aqui. A chave composta garante que o mesmo endereço externo não seja vinculado duas vezes ao mesmo funcionário. A integridade do `EMPLOYEE_ID_ADDRESS` é responsabilidade da aplicação.

| Campo | Tipo | Restrição | Descrição |
|---|---|---|---|
| EMPLOYEE_ID_ADDRESS | BIGINT | PK | ID do endereço no sistema externo. Sem FK — integridade garantida pela aplicação |
| EMPLOYEE_ID | BIGINT | PK, FK | Referência ao funcionário. Forma chave composta com `EMPLOYEE_ID_ADDRESS` |
| ADDRESS_TYPE_ID | BIGINT | UK*, FK, NOT NULL | Referência ao tipo de endereço (`SCOS_ADDRESS_TYPE`, `ENTITY_TYPE = EMPLOYEE`). *UK composta com `EMPLOYEE_ID` |
| NUMBER | INT | NOT NULL | Número do endereço |
| COMPLEMENT | VARCHAR(250) | NULL | Complemento — apartamento, bloco, sala etc |
| GEOLOCATION | POINT | NOT NULL | Coordenadas geográficas — latitude e longitude |
| CREATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora em que o registro foi criado |
| UPDATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora da última atualização do registro |
| USER_AT | VARCHAR(255) | NOT NULL | Login do usuário que realizou a última alteração |

> **Trigger:** `TRG_VALIDATE_EMPLOYEE_ADDRESS_TYPE` — valida `ENTITY_TYPE = EMPLOYEE` em `ADDRESS_TYPE_ID` (ver seção Triggers)

---

### SCOS_COMPANY

**Descrição:** Representa tanto a empresa matriz quanto suas filiais. A auto-referência via `PARENT_COMPANY_ID` estrutura a hierarquia — nulo indica matriz, preenchido indica filial.

| Campo | Tipo | Restrição | Descrição |
|---|---|---|---|
| COMPANY_ID | BIGINT | PK | Identificador único da empresa, gerado automaticamente |
| PARENT_COMPANY_ID | BIGINT | FK, NULL | Referência à empresa mãe. Nulo indica que é a matriz |
| NAME | VARCHAR(250) | NOT NULL | Razão social da empresa |
| NAME_TREATMENT | VARCHAR(100) | NOT NULL | Nome fantasia ou nome de tratamento |
| TAX_IDENTIFIER | VARCHAR(14) | UK, NOT NULL | CNPJ da empresa — identificador fiscal único |
| LEGAL_NATURE_ID | BIGINT | FK, NULL | Referência à natureza jurídica (`SCOS_LEGAL_NATURE`) |
| CNAE_PRINCIPAL_ID | BIGINT | FK, NULL | Referência ao CNAE principal (`SCOS_CNAE`) |
| STATE_REGISTRATION | VARCHAR(20) | NULL | Inscrição Estadual — aceita `ISENTO` para empresas sem IE |
| MUNICIPAL_REGISTRATION | VARCHAR(20) | NULL | Inscrição Municipal |
| FOUNDATION_DATE | DATE | NOT NULL | Data de fundação |
| SECTOR_OF_ACTIVITY | VARCHAR(100) | NOT NULL | Setor de atividade — ex: Tecnologia, Varejo, Saúde |
| OBSERVATION | TEXT | NULL | Observações gerais. Campo livre |
| STATUS | VARCHAR(20) | NOT NULL | Situação atual. Cache mantido por trigger a partir de `SCOS_COMPANY_STATUS_HISTORY` — não atualizar diretamente, inserir no histórico |
| CREATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora em que o registro foi criado |
| UPDATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora da última atualização do registro |
| USER_AT | VARCHAR(255) | NOT NULL | Login do usuário que realizou a última alteração |

> **Check:** `CHK_COMPANY_STATUS` trava `STATUS` ao vocabulário fechado (ver seção Checks de Domínio)

---

### SCOS_COMPANY_ADDRESS

**Descrição:** Armazena os endereços da empresa por referência a um sistema externo. Segue o mesmo padrão de `SCOS_EMPLOYEE_ADDRESS`. A integridade do `COMPANY_ID_ADDRESS` é responsabilidade da aplicação.

| Campo | Tipo | Restrição | Descrição |
|---|---|---|---|
| COMPANY_ID_ADDRESS | BIGINT | PK | ID do endereço no sistema externo. Sem FK — integridade garantida pela aplicação |
| COMPANY_ID | BIGINT | PK, FK | Referência à empresa. Forma chave composta com `COMPANY_ID_ADDRESS` |
| ADDRESS_TYPE_ID | BIGINT | UK*, FK, NOT NULL | Referência ao tipo de endereço (`SCOS_ADDRESS_TYPE`, `ENTITY_TYPE = COMPANY`). *UK composta com `COMPANY_ID` |
| NUMBER | INT | NOT NULL | Número do endereço |
| COMPLEMENT | VARCHAR(250) | NULL | Complemento — sala, andar, bloco etc |
| GEOLOCATION | POINT | NOT NULL | Coordenadas geográficas — latitude e longitude |
| CREATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora em que o registro foi criado |
| UPDATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora da última atualização do registro |
| USER_AT | VARCHAR(255) | NOT NULL | Login do usuário que realizou a última alteração |

> **Trigger:** `TRG_VALIDATE_COMPANY_ADDRESS_TYPE` — valida `ENTITY_TYPE = COMPANY` em `ADDRESS_TYPE_ID` (ver seção Triggers)

---

### SCOS_COMPANY_CONTACT

**Descrição:** Armazena os contatos da empresa, como telefone comercial, e-mail financeiro ou canal de suporte. Uma empresa pode ter múltiplos contatos de diferentes tipos.

| Campo | Tipo | Restrição | Descrição |
|---|---|---|---|
| COMPANY_ID_CONTACT | BIGINT | PK | Identificador único do contato, gerado automaticamente |
| COMPANY_ID | BIGINT | FK, NOT NULL | Referência à empresa dona do contato |
| PHONE | VARCHAR(50) | UK*, NOT NULL | Número de telefone com DDD. *UK composta com `COMPANY_ID` — mesmo telefone pode se repetir entre empresas diferentes (ex: filiais com central única) |
| EMAIL | VARCHAR(255) | UK*, NOT NULL | E-mail de contato da empresa. *UK composta com `COMPANY_ID` — mesmo e-mail pode se repetir entre empresas diferentes |
| CONTACT_TYPE_ID | BIGINT | UK*, FK, NOT NULL | Referência ao tipo de contato (`SCOS_CONTACT_TYPE`, `ENTITY_TYPE = COMPANY`). *UK composta com `COMPANY_ID` |
| RESPONSIBLE_PERSON | VARCHAR(255) | NOT NULL | Nome da pessoa responsável pelo contato na empresa |
| CREATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora em que o registro foi criado |
| UPDATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora da última atualização do registro |
| USER_AT | VARCHAR(255) | NOT NULL | Login do usuário que realizou a última alteração |

> **Trigger:** `TRG_VALIDATE_COMPANY_CONTACT_TYPE` — valida `ENTITY_TYPE = COMPANY` em `CONTACT_TYPE_ID` (ver seção Triggers)

---

### SCOS_LEGAL_NATURE

**Descrição:** Tabela de referência com os códigos oficiais de Natureza Jurídica (classificação IBGE/Receita Federal). Populada via carga inicial (seed); alterada raramente.

| Campo | Tipo | Restrição | Descrição |
|---|---|---|---|
| LEGAL_NATURE_ID | BIGINT | PK | Identificador único, gerado automaticamente |
| CODE | VARCHAR(10) | UK, NOT NULL | Código oficial — ex: `206-2` (Sociedade Empresária Limitada) |
| DESCRIPTION | VARCHAR(255) | NOT NULL | Descrição oficial da natureza jurídica |
| CREATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora em que o registro foi criado |

---

### SCOS_CNAE

**Descrição:** Tabela de referência com os códigos oficiais de CNAE (Classificação Nacional de Atividades Econômicas). Populada via carga inicial (seed). Referenciada por `SCOS_COMPANY.CNAE_PRINCIPAL_ID` (CNAE principal, 1:1) e por `SCOS_COMPANY_CNAE_SECONDARY` (CNAEs secundários, N:N).

| Campo | Tipo | Restrição | Descrição |
|---|---|---|---|
| CNAE_ID | BIGINT | PK | Identificador único, gerado automaticamente |
| CODE | VARCHAR(10) | UK, NOT NULL | Código oficial — ex: `6201-5/01` |
| DESCRIPTION | VARCHAR(255) | NOT NULL | Descrição oficial da atividade |
| CREATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora em que o registro foi criado |

---

### SCOS_COMPANY_CNAE_SECONDARY

**Descrição:** Vínculo N:N entre empresa e CNAEs secundários — por definição legal, uma empresa tem exatamente um CNAE principal e pode ter zero ou mais CNAEs secundários. Não possui `UPDATED_AT`: o vínculo é criado ou removido, não atualizado.

| Campo | Tipo | Restrição | Descrição |
|---|---|---|---|
| COMPANY_ID | BIGINT | PK, FK | Referência à empresa. Parte da chave composta |
| CNAE_ID | BIGINT | PK, FK | Referência ao CNAE secundário. Parte da chave composta |
| CREATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora em que o vínculo foi criado |
| USER_AT | VARCHAR(255) | NOT NULL | Login do usuário que realizou o vínculo |

> **Índice:** `IDX_COMPANY_CNAE_SECONDARY_REVERSE` em `(CNAE_ID, COMPANY_ID)` — suporta a consulta reversa "quais empresas têm este CNAE secundário"

---

### SCOS_PROFILE

**Descrição:** Representa um perfil de acesso — um pacote nomeado de permissões atribuído a usuários. Centraliza a definição de permissões evitando configuração individual por usuário. Exemplos: `ADMIN`, `OPERATOR`, `VIEWER`, `AUDITOR`.

| Campo | Tipo | Restrição | Descrição |
|---|---|---|---|
| PROFILE_ID | BIGINT | PK | Identificador único do perfil, gerado automaticamente |
| CODE | VARCHAR(30) | UK, NOT NULL | Código único do perfil — ex: `ADMIN`, `OPERATOR`, `VIEWER` |
| DESCRIPTION | VARCHAR(255) | NOT NULL | Descrição do perfil e seu propósito |
| ACTIVE | BOOLEAN | NOT NULL, DEFAULT TRUE | Indica se o perfil está ativo. Perfis inativos não podem ser atribuídos a novos usuários |
| CREATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora em que o registro foi criado |
| UPDATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora da última atualização do registro |
| USER_AT | VARCHAR(255) | NOT NULL | Login do usuário que realizou a última alteração |

---

### SCOS_LOGIN

**Descrição:** Representa o usuário de acesso ao sistema. Pode ou não estar vinculado a um funcionário — usuários externos, parceiros e contas de serviço existem sem funcionário associado. A autenticação é gerenciada por um provedor de identidade externo (atualmente Keycloak). O campo `EXTERNAL_ID` é o vínculo com o usuário criado nesse provedor — processado via `SCOS_OUTBOX_EVENT` (tópicos `keycloak.user.*`, hoje; qualquer outro provedor no futuro usa o mesmo mecanismo sem precisar renomear coluna).

| Campo | Tipo | Restrição | Descrição |
|---|---|---|---|
| LOGIN_ID | BIGINT | PK | Identificador único do login, gerado automaticamente |
| PROFILE_ID | BIGINT | FK, NOT NULL | Perfil **principal** do login. Perfis adicionais ficam em `SCOS_LOGIN_PROFILE` — não duplicar este aqui |
| EMPLOYEE_ID | BIGINT | FK, NULL | Referência ao funcionário. Nulo para usuários externos ou contas de serviço |
| EXTERNAL_ID | UUID | NULL | ID do usuário no provedor de identidade externo (Keycloak, hoje) — preenchido pelo worker consumidor após processar `SCOS_OUTBOX_EVENT` (`RESPONSE_DATA->>'id'`) com sucesso |
| LOGIN | VARCHAR(255) | NOT NULL | E-mail ou username utilizado para acesso |
| STATUS | VARCHAR(50) | NOT NULL | Situação atual. Cache mantido por trigger a partir de `SCOS_LOGIN_STATUS_HISTORY` — não atualizar diretamente, inserir no histórico |
| TYPE | VARCHAR(50) | NOT NULL | Tipo do usuário: `EMPLOYEE`, `EXTERNAL` ou `SERVICE` |
| LAST_USED_AT | TIMESTAMPTZ | NULL | Data e hora do último uso do login. `NULL` = nunca usado (ex: recém-criado). Base para a auto-inativação por desuso (`LOGIN_INACTIVITY_TIMEOUT_DAYS`) — **mecanismo de atualização (endpoint dedicado, evento de Outbox, etc.) ainda não definido**, coluna adicionada só para destravar o schema |
| CREATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora em que o registro foi criado |
| UPDATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora da última atualização do registro |
| USER_AT | VARCHAR(255) | NOT NULL | Login do usuário que realizou a última alteração |

> **Check:** `CHK_LOGIN_STATUS` e `CHK_LOGIN_TYPE` travam `STATUS`/`TYPE` ao vocabulário fechado (ver seção Checks de Domínio)
> **Índice:** `IDX_LOGIN_LAST_USED_AT` em `(LAST_USED_AT) WHERE STATUS = 'ACTIVE'` — suporta o job de auto-inativação varrendo só logins ativos com uso antigo (ver seção Índices de Performance)

---

### SCOS_LOGIN_PROFILE

**Descrição:** Perfis **adicionais** de um login, além do principal (`SCOS_LOGIN.PROFILE_ID`). Espelha `SCOS_PROFILE_RESOURCE` — sem `UPDATED_AT`, pois o vínculo é criado ou removido, nunca atualizado. "Todos os perfis de um login" é `PROFILE_ID` (principal) **UNION** `SELECT PROFILE_ID FROM SCOS_LOGIN_PROFILE WHERE LOGIN_ID = ...`.

| Campo | Tipo | Restrição | Descrição |
|---|---|---|---|
| LOGIN_ID | BIGINT | PK, FK | Referência ao login. Parte da chave composta |
| PROFILE_ID | BIGINT | PK, FK | Referência ao perfil adicional. Parte da chave composta |
| CREATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora em que o vínculo foi criado |
| USER_AT | VARCHAR(255) | NOT NULL | Login do usuário que realizou o vínculo |

> **Trigger:** `TRG_VALIDATE_LOGIN_PROFILE_NOT_PRIMARY` — impede duplicar aqui o perfil que já é o principal em `SCOS_LOGIN.PROFILE_ID` (ver seção Triggers)

---

### SCOS_CONFIGURATION

**Descrição:** Armazena as configurações gerais do sistema SCOS. Cada configuração é identificada por uma chave textual com seu valor e tipo.

| Campo | Tipo | Restrição | Descrição |
|---|---|---|---|
| CONFIGURATION_ID | VARCHAR(50) | PK | Chave identificadora da configuração — ex: `TOKEN_EXPIRY_MINUTES`, `MAX_LOGIN_ATTEMPTS` |
| VALUE | TEXT | NOT NULL | Valor da configuração |
| TYPE | VARCHAR(50) | NOT NULL | Tipo do valor para interpretação: `STRING`, `INTEGER`, `BOOLEAN`, `JSON` |
| CREATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora em que o registro foi criado |
| UPDATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora da última atualização do registro |
| USER_AT | VARCHAR(255) | NOT NULL | Login do usuário que realizou a última alteração |

---

### SCOS_SYSTEM

**Descrição:** Representa os sistemas externos cadastrados e autorizados a usar a central de permissões. Todo sistema precisa se registrar aqui antes de qualquer consulta. A `SECRET_KEY` é a credencial de autenticação nas chamadas à API.

| Campo | Tipo | Restrição | Descrição |
|---|---|---|---|
| SYSTEM_ID | UUID | PK | Identificador único do sistema, gerado automaticamente como UUID |
| NAME | VARCHAR(250) | NOT NULL | Nome completo do sistema |
| CODE | VARCHAR(25) | UK, NOT NULL | Código único do sistema — ex: `STOCK_SYSTEM`, `STREAMING_APP` |
| DESCRIPTION | VARCHAR(200) | NOT NULL | Descrição do sistema e sua finalidade |
| SECRET_KEY | TEXT | NOT NULL | Chave secreta para autenticação do sistema nas chamadas à API de permissões |
| STATUS | VARCHAR(50) | NOT NULL | Situação: `ACTIVE` ou `INACTIVE`. Sistemas inativos têm acesso bloqueado |
| VERSION | VARCHAR(50) | NOT NULL | Versão atual do sistema — ex: `1.0.0`, `2.3.1` |
| CREATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora em que o registro foi criado |
| UPDATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora da última atualização do registro |
| USER_AT | VARCHAR(255) | NOT NULL | Login do usuário que realizou o cadastro |

---

### SCOS_RESOURCE

**Descrição:** Representa as permissões disponíveis em cada sistema. Cada sistema define suas próprias permissões. O código já carrega a ação embutida — ex: `PRODUCT_READ`, `VIDEO_WATCH`. O sistema externo recebe a lista e aplica conforme seu negócio.

| Campo | Tipo | Restrição | Descrição |
|---|---|---|---|
| RESOURCE_ID | UUID | PK | Identificador único da permissão, gerado automaticamente como UUID |
| SYSTEM_ID | UUID | FK, NOT NULL | Referência ao sistema ao qual essa permissão pertence |
| CODE | VARCHAR(50) | UK*, NOT NULL | Código da permissão com ação embutida — ex: `PRODUCT_READ`, `ORDER_APPROVE`, `VIDEO_WATCH`. *UK composta com `SYSTEM_ID` — mesmo código pode existir em sistemas diferentes |
| DESCRIPTION_PT | VARCHAR(255) | NOT NULL | Descrição da permissão em português |
| DESCRIPTION_EN | VARCHAR(255) | NOT NULL | Descrição da permissão em inglês |
| ACTIVE | BOOLEAN | NOT NULL, DEFAULT TRUE | Indica se a permissão está disponível para ser atribuída a perfis |
| CREATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora em que o registro foi criado |
| UPDATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora da última atualização do registro |
| USER_AT | VARCHAR(255) | NOT NULL | Login do usuário que realizou o cadastro |

---

### SCOS_PROFILE_RESOURCE

**Descrição:** Tabela de vínculo entre perfil e permissões. Define o que cada perfil pode fazer. A chave composta garante que a mesma permissão não seja atribuída duas vezes ao mesmo perfil. Não possui `UPDATED_AT` pois o vínculo não é atualizado — é criado ou removido.

| Campo | Tipo | Restrição | Descrição |
|---|---|---|---|
| PROFILE_ID | BIGINT | PK, FK | Referência ao perfil. Parte da chave composta |
| RESOURCE_ID | UUID | PK, FK | Referência à permissão. Parte da chave composta |
| CREATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora em que a permissão foi atribuída ao perfil |
| USER_AT | VARCHAR(255) | NOT NULL | Login do usuário que realizou a atribuição |

---

## Motivos e Histórico de Status

**Contexto:** `SCOS_COMPANY`, `SCOS_EMPLOYEE` e `SCOS_LOGIN` possuem um campo `STATUS` com 3 situações possíveis, mas o valor isolado não basta para fins de auditoria — não registra *quem* alterou, *quando* e *por quê*. As tabelas desta seção resolvem isso: 4 tabelas de motivo pré-cadastrado (uma por tipo de transição) e 3 tabelas de histórico (uma por entidade), com um `CHECK` que garante coerência entre a transição de status e o motivo informado.

> **Atenção:** `SCOS_LOGIN.STATUS` usa `BLOCKED` como terceiro estado, enquanto `SCOS_COMPANY.STATUS` e `SCOS_EMPLOYEE.STATUS` usam `DISABLED`. O conceito é o mesmo (bloqueio administrativo/temporário, distinto de `INACTIVE` = encerramento definitivo) — só o literal muda. Os `CHECK` abaixo respeitam essa diferença.

---

### SCOS_REASON_ACTIVATE

**Descrição:** Motivos pré-cadastrados para a transição **para `ACTIVE`** vinda de `INACTIVE` (reativação — ex: recontratação). Compartilhada entre `COMPANY`, `EMPLOYEE` e `LOGIN` via `ENTITY_TYPE`; a aplicação filtra por esse campo ao popular a lista de seleção.

| Campo | Tipo | Restrição | Descrição |
|---|---|---|---|
| REASON_ACTIVATE_ID | BIGINT | PK | Identificador único do motivo, gerado automaticamente |
| CODE | VARCHAR(30) | UK*, NOT NULL | Código do motivo — ex: `NEW_HIRE`, `REINSTATEMENT`. *UK composta com `ENTITY_TYPE` |
| DESCRIPTION | VARCHAR(255) | NOT NULL | Descrição legível do motivo |
| ENTITY_TYPE | VARCHAR(20) | UK*, NOT NULL | Entidade à qual o motivo se aplica: `COMPANY`, `EMPLOYEE` ou `LOGIN` |
| ACTIVE | BOOLEAN | NOT NULL, DEFAULT TRUE | Indica se o motivo está disponível para seleção |
| CREATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora em que o registro foi criado |
| UPDATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora da última atualização |
| USER_AT | VARCHAR(255) | NOT NULL | Login do usuário que cadastrou o motivo |

> **Índice:** `IDX_REASON_ACTIVATE_ENTITY_TYPE` parcial em `(ENTITY_TYPE) WHERE ACTIVE = TRUE`
> **Trigger:** `TRG_BLOCK_DELETE_REASON_ACTIVATE` — bloqueia `DELETE` (ver seção Triggers)
> **Check:** `CHK_REASON_ACTIVATE_ENTITY_TYPE` trava `ENTITY_TYPE` ao vocabulário fechado (ver seção Checks de Domínio)

---

### SCOS_REASON_INACTIVATE

**Descrição:** Motivos pré-cadastrados para a transição **para `INACTIVE`** (encerramento definitivo — ex: demissão, encerramento de empresa).

| Campo | Tipo | Restrição | Descrição |
|---|---|---|---|
| REASON_INACTIVATE_ID | BIGINT | PK | Identificador único do motivo, gerado automaticamente |
| CODE | VARCHAR(30) | UK*, NOT NULL | Código do motivo — ex: `RESIGNATION`, `COMPANY_CLOSED`. *UK composta com `ENTITY_TYPE` |
| DESCRIPTION | VARCHAR(255) | NOT NULL | Descrição legível do motivo |
| ENTITY_TYPE | VARCHAR(20) | UK*, NOT NULL | Entidade à qual o motivo se aplica: `COMPANY`, `EMPLOYEE` ou `LOGIN` |
| ACTIVE | BOOLEAN | NOT NULL, DEFAULT TRUE | Indica se o motivo está disponível para seleção |
| CREATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora em que o registro foi criado |
| UPDATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora da última atualização |
| USER_AT | VARCHAR(255) | NOT NULL | Login do usuário que cadastrou o motivo |

> **Índice:** `IDX_REASON_INACTIVATE_ENTITY_TYPE` parcial em `(ENTITY_TYPE) WHERE ACTIVE = TRUE`
> **Trigger:** `TRG_BLOCK_DELETE_REASON_INACTIVATE` — bloqueia `DELETE` (ver seção Triggers)
> **Check:** `CHK_REASON_INACTIVATE_ENTITY_TYPE` trava `ENTITY_TYPE` ao vocabulário fechado (ver seção Checks de Domínio)

---

### SCOS_REASON_DISABLE

**Descrição:** Motivos pré-cadastrados para a transição **para `DISABLED`/`BLOCKED`** (bloqueio temporário — ex: suspensão em auditoria, tentativas de login inválidas).

| Campo | Tipo | Restrição | Descrição |
|---|---|---|---|
| REASON_DISABLE_ID | BIGINT | PK | Identificador único do motivo, gerado automaticamente |
| CODE | VARCHAR(30) | UK*, NOT NULL | Código do motivo — ex: `UNDER_AUDIT`, `INVALID_LOGIN_ATTEMPTS`. *UK composta com `ENTITY_TYPE` |
| DESCRIPTION | VARCHAR(255) | NOT NULL | Descrição legível do motivo |
| ENTITY_TYPE | VARCHAR(20) | UK*, NOT NULL | Entidade à qual o motivo se aplica: `COMPANY`, `EMPLOYEE` ou `LOGIN` |
| ACTIVE | BOOLEAN | NOT NULL, DEFAULT TRUE | Indica se o motivo está disponível para seleção |
| CREATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora em que o registro foi criado |
| UPDATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora da última atualização |
| USER_AT | VARCHAR(255) | NOT NULL | Login do usuário que cadastrou o motivo |

> **Índice:** `IDX_REASON_DISABLE_ENTITY_TYPE` parcial em `(ENTITY_TYPE) WHERE ACTIVE = TRUE`
> **Trigger:** `TRG_BLOCK_DELETE_REASON_DISABLE` — bloqueia `DELETE` (ver seção Triggers)
> **Check:** `CHK_REASON_DISABLE_ENTITY_TYPE` trava `ENTITY_TYPE` ao vocabulário fechado (ver seção Checks de Domínio)

---

### SCOS_REASON_ENABLE

**Descrição:** Motivos pré-cadastrados para a transição **de `DISABLED`/`BLOCKED` de volta para `ACTIVE`** (desbloqueio — ex: revisão concluída). Tabela própria, separada de `SCOS_REASON_ACTIVATE`, porque motivo de desbloqueio e motivo de reativação após encerramento definitivo são semanticamente diferentes.

| Campo | Tipo | Restrição | Descrição |
|---|---|---|---|
| REASON_ENABLE_ID | BIGINT | PK | Identificador único do motivo, gerado automaticamente |
| CODE | VARCHAR(30) | UK*, NOT NULL | Código do motivo — ex: `AUDIT_CLEARED`, `UNBLOCKED_BY_SUPPORT`. *UK composta com `ENTITY_TYPE` |
| DESCRIPTION | VARCHAR(255) | NOT NULL | Descrição legível do motivo |
| ENTITY_TYPE | VARCHAR(20) | UK*, NOT NULL | Entidade à qual o motivo se aplica: `COMPANY`, `EMPLOYEE` ou `LOGIN` |
| ACTIVE | BOOLEAN | NOT NULL, DEFAULT TRUE | Indica se o motivo está disponível para seleção |
| CREATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora em que o registro foi criado |
| UPDATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora da última atualização |
| USER_AT | VARCHAR(255) | NOT NULL | Login do usuário que cadastrou o motivo |

> **Índice:** `IDX_REASON_ENABLE_ENTITY_TYPE` parcial em `(ENTITY_TYPE) WHERE ACTIVE = TRUE`
> **Trigger:** `TRG_BLOCK_DELETE_REASON_ENABLE` — bloqueia `DELETE` (ver seção Triggers)
> **Check:** `CHK_REASON_ENABLE_ENTITY_TYPE` trava `ENTITY_TYPE` ao vocabulário fechado (ver seção Checks de Domínio)

---

### SCOS_COMPANY_STATUS_HISTORY

**Descrição:** Histórico completo de mudanças de status de `SCOS_COMPANY`, com motivo e responsável por cada transição. `PREVIOUS_STATUS` é preenchido automaticamente por trigger a partir do último registro real — não deve ser informado pela aplicação. O `CHECK` de transição garante que exatamente a FK de motivo coerente com `PREVIOUS_STATUS → STATUS` esteja preenchida. Tabela imutável — apenas inserção.

| Campo | Tipo | Restrição | Descrição |
|---|---|---|---|
| COMPANY_STATUS_HISTORY_ID | BIGINT | PK | Identificador único do registro de histórico |
| COMPANY_ID | BIGINT | FK, NOT NULL | Referência à empresa |
| STATUS | VARCHAR(20) | NOT NULL | Novo status: `ACTIVE`, `INACTIVE` ou `DISABLED` |
| PREVIOUS_STATUS | VARCHAR(20) | NULL | Status anterior — preenchido por trigger; `NULL` apenas no primeiro registro da empresa |
| REASON_ACTIVATE_ID | BIGINT | FK, NULL | Preenchido quando `PREVIOUS_STATUS = INACTIVE` e `STATUS = ACTIVE` |
| REASON_INACTIVATE_ID | BIGINT | FK, NULL | Preenchido quando `STATUS = INACTIVE` |
| REASON_DISABLE_ID | BIGINT | FK, NULL | Preenchido quando `STATUS = DISABLED` |
| REASON_ENABLE_ID | BIGINT | FK, NULL | Preenchido quando `PREVIOUS_STATUS = DISABLED` e `STATUS = ACTIVE` |
| OBSERVATION | TEXT | NULL | Complemento textual livre ao motivo selecionado |
| CREATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora da transição |
| USER_AT | VARCHAR(255) | NOT NULL | Login do usuário que realizou a transição |

> **CHECK:** `CHK_COMPANY_STATUS_TRANSITION` — valida a combinação `PREVIOUS_STATUS` / `STATUS` / FK de motivo (detalhe na seção Triggers)
> **Índice:** `IDX_COMPANY_STATUS_HISTORY_CURRENT` em `(COMPANY_ID, CREATED_AT DESC)` — consulta de status atual e usado pela própria trigger a cada INSERT
> **Trigger:** `TRG_SYNC_COMPANY_STATUS` sincroniza `SCOS_COMPANY.STATUS` após cada INSERT (ver seção Triggers)
> **Check:** `CHK_COMPANY_STATUS_HISTORY_STATUS`/`_PREVIOUS` travam `STATUS`/`PREVIOUS_STATUS` ao vocabulário fechado (ver seção Checks de Domínio)

---

### SCOS_EMPLOYEE_STATUS_HISTORY

**Descrição:** Histórico completo de mudanças de status de `SCOS_EMPLOYEE`. Mesma estrutura e regras de `SCOS_COMPANY_STATUS_HISTORY`.

| Campo | Tipo | Restrição | Descrição |
|---|---|---|---|
| EMPLOYEE_STATUS_HISTORY_ID | BIGINT | PK | Identificador único do registro de histórico |
| EMPLOYEE_ID | BIGINT | FK, NOT NULL | Referência ao funcionário |
| STATUS | VARCHAR(20) | NOT NULL | Novo status: `ACTIVE`, `INACTIVE` ou `DISABLED` |
| PREVIOUS_STATUS | VARCHAR(20) | NULL | Status anterior — preenchido por trigger; `NULL` apenas no primeiro registro do funcionário |
| REASON_ACTIVATE_ID | BIGINT | FK, NULL | Preenchido quando `PREVIOUS_STATUS = INACTIVE` e `STATUS = ACTIVE` |
| REASON_INACTIVATE_ID | BIGINT | FK, NULL | Preenchido quando `STATUS = INACTIVE` |
| REASON_DISABLE_ID | BIGINT | FK, NULL | Preenchido quando `STATUS = DISABLED` |
| REASON_ENABLE_ID | BIGINT | FK, NULL | Preenchido quando `PREVIOUS_STATUS = DISABLED` e `STATUS = ACTIVE` |
| OBSERVATION | TEXT | NULL | Complemento textual livre ao motivo selecionado |
| CREATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora da transição |
| USER_AT | VARCHAR(255) | NOT NULL | Login do usuário que realizou a transição |

> **CHECK:** `CHK_EMPLOYEE_STATUS_TRANSITION` — mesma lógica de `CHK_COMPANY_STATUS_TRANSITION`
> **Índice:** `IDX_EMPLOYEE_STATUS_HISTORY_CURRENT` em `(EMPLOYEE_ID, CREATED_AT DESC)`
> **Trigger:** `TRG_SYNC_EMPLOYEE_STATUS` sincroniza `SCOS_EMPLOYEE.STATUS` após cada INSERT (ver seção Triggers)
> **Check:** `CHK_EMPLOYEE_STATUS_HISTORY_STATUS`/`_PREVIOUS` travam `STATUS`/`PREVIOUS_STATUS` ao vocabulário fechado (ver seção Checks de Domínio)

---

### SCOS_LOGIN_STATUS_HISTORY

**Descrição:** Histórico completo de mudanças de status de `SCOS_LOGIN`. Estrutura idêntica às demais, **exceto pelo literal do terceiro estado**: aqui é `BLOCKED`, não `DISABLED`, para ficar consistente com `SCOS_LOGIN.STATUS`.

| Campo | Tipo | Restrição | Descrição |
|---|---|---|---|
| LOGIN_STATUS_HISTORY_ID | BIGINT | PK | Identificador único do registro de histórico |
| LOGIN_ID | BIGINT | FK, NOT NULL | Referência ao login |
| STATUS | VARCHAR(20) | NOT NULL | Novo status: `ACTIVE`, `INACTIVE` ou `BLOCKED` |
| PREVIOUS_STATUS | VARCHAR(20) | NULL | Status anterior — preenchido por trigger; `NULL` apenas no primeiro registro do login |
| REASON_ACTIVATE_ID | BIGINT | FK, NULL | Preenchido quando `PREVIOUS_STATUS = INACTIVE` e `STATUS = ACTIVE` |
| REASON_INACTIVATE_ID | BIGINT | FK, NULL | Preenchido quando `STATUS = INACTIVE` |
| REASON_DISABLE_ID | BIGINT | FK, NULL | Preenchido quando `STATUS = BLOCKED` |
| REASON_ENABLE_ID | BIGINT | FK, NULL | Preenchido quando `PREVIOUS_STATUS = BLOCKED` e `STATUS = ACTIVE` |
| OBSERVATION | TEXT | NULL | Complemento textual livre ao motivo selecionado |
| CREATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora da transição |
| USER_AT | VARCHAR(255) | NOT NULL | Login do usuário que realizou a transição |

> **CHECK:** `CHK_LOGIN_STATUS_TRANSITION` — mesma lógica, com `BLOCKED` no lugar de `DISABLED`
> **Índice:** `IDX_LOGIN_STATUS_HISTORY_CURRENT` em `(LOGIN_ID, CREATED_AT DESC)`
> **Trigger:** `TRG_SYNC_LOGIN_STATUS` sincroniza `SCOS_LOGIN.STATUS` após cada INSERT (ver seção Triggers)
> **Check:** `CHK_LOGIN_STATUS_HISTORY_STATUS`/`_PREVIOUS` travam `STATUS`/`PREVIOUS_STATUS` ao vocabulário fechado, com `BLOCKED` no lugar de `DISABLED` (ver seção Checks de Domínio)

---

## Outbox e Integrações Externas

**Contexto:** esta seção generaliza o antigo mecanismo específico de Keycloak para qualquer processo que precise de entrega assíncrona e confiável — seja publicando um evento de domínio num broker (`PGMQ`/`KAFKA`, ver decisão de arquitetura anterior sobre outbox pattern) ou chamando uma API externa diretamente (Keycloak, sistema de ponto, etc.), com retry e rastreabilidade. `SCOS_OUTBOX_TOPIC` decide o roteamento; o produtor da aplicação só grava em `SCOS_OUTBOX_EVENT`, sem saber qual o destino final.

---

### SCOS_OUTBOX_TOPIC

**Descrição:** Registro dos tópicos conhecidos e seu roteamento. Adicionar um novo processo de integração é uma linha nesta tabela, não um deploy de código — é o que torna o mecanismo genérico de fato. Convenção de nomenclatura: eventos de domínio usam `entidade.acao` (ex: `employee.deactivated`); integrações diretas usam `sistema.entidade.acao` (ex: `keycloak.user.create`).

| Campo | Tipo | Restrição | Descrição |
|---|---|---|---|
| TOPIC | VARCHAR(255) | PK | Nome do tópico — chave natural, sem ID substituto |
| BACKEND | VARCHAR(20) | NOT NULL | Destino: `PGMQ`, `KAFKA` ou `DIRECT_API` |
| TARGET_SYSTEM | VARCHAR(50) | NULL* | Sistema externo alvo — obrigatório quando `BACKEND = DIRECT_API`, nulo nos demais casos |
| DEFAULT_MAX_RETRIES | INT | NOT NULL, DEFAULT 3 | Valor padrão de `MAX_RETRIES` para eventos deste tópico |
| ACTIVE | BOOLEAN | NOT NULL, DEFAULT TRUE | Tópico desativado rejeita novos eventos (validar na aplicação antes de inserir) |
| CREATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora em que o registro foi criado |
| UPDATED_AT | TIMESTAMPTZ | NOT NULL | Data e hora da última atualização |
| USER_AT | VARCHAR(255) | NOT NULL | Login do usuário que cadastrou o tópico |

*Restrição de integridade: `TARGET_SYSTEM` deve ser preenchido quando `BACKEND = DIRECT_API` e nulo quando `BACKEND` é `PGMQ` ou `KAFKA`.

> **Trigger:** `TRG_BLOCK_DELETE_OUTBOX_TOPIC` — bloqueia `DELETE` (ver seção Triggers). Necessário porque `SCOS_OUTBOX_EVENT.TOPIC` é FK — excluir um tópico em uso quebraria eventos já gravados
> **Check:** `CHK_OUTBOX_TOPIC_BACKEND` trava `BACKEND` ao vocabulário fechado (ver seção Checks de Domínio)

---

### SCOS_OUTBOX_EVENT

**Descrição:** Tabela única para qualquer evento/tarefa assíncrona — substitui `SCOS_INTEGRATION_KEYCLOAK`. Campos específicos de Keycloak (`REALM`, `EMAIL`, `USERNAME`) migram para dentro de `PAYLOAD`; o antigo `TYPE` (criação/atualização/remoção) migra para o próprio nome do `TOPIC`. `RESPONSE_DATA` guarda o retorno de integrações `DIRECT_API` (ex: `{"id": "<id_do_usuario_no_provedor>"}`) — cada worker consumidor é responsável por ler esse campo e sincronizar de volta no seu próprio domínio (ex: gravar o id recebido em `SCOS_LOGIN.EXTERNAL_ID`); a tabela genérica não sabe nem precisa saber disso.

| Campo | Tipo | Restrição | Descrição |
|---|---|---|---|
| OUTBOX_EVENT_ID | BIGINT | PK | Identificador único do evento, gerado automaticamente |
| EVENT_HASH | CHAR(64) | UK, NOT NULL | Hash do payload — dedup de inserção duplicada |
| TOPIC | VARCHAR(255) | FK, NOT NULL | Referência a `SCOS_OUTBOX_TOPIC` — define o roteamento |
| AGGREGATE_ID | TEXT | NOT NULL | ID da entidade de origem no domínio (ex: `LOGIN_ID`, `EMPLOYEE_ID`) — sem FK, pois pode apontar pra qualquer tabela |
| PAYLOAD | JSONB | NOT NULL | Dados do evento/tarefa — schema livre, específico de cada tópico |
| RESPONSE_DATA | JSONB | NULL | Resposta do destino, preenchida quando `BACKEND = DIRECT_API`. Nulo para tópicos roteados a broker |
| STATUS | VARCHAR(20) | NOT NULL, DEFAULT PENDING | `PENDING`, `PROCESSING`, `PROCESSED` ou `FAILED` |
| RETRY_COUNT | INT | NOT NULL, DEFAULT 0 | Número de tentativas realizadas até o momento |
| MAX_RETRIES | INT | NOT NULL, DEFAULT 3 | Limite de tentativas antes de mover para dead letter |
| MESSAGE | VARCHAR(2500) | NULL | Descrição do resultado ou do último erro |
| REQUESTING | VARCHAR(255) | NOT NULL | Identificador de quem/o que originou o evento — sistema ou usuário |
| STARTED_AT | TIMESTAMPTZ | NULL | Início do processamento da tentativa atual |
| PROCESSED_AT | TIMESTAMPTZ | NULL | Conclusão (sucesso ou falha definitiva). Só é preenchido quando `STATUS` é `PROCESSED` ou `FAILED` — fica nulo em `PENDING`/`PROCESSING` |
| CREATED_AT | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Data e hora em que o registro foi criado |
| UPDATED_AT | TIMESTAMPTZ | NULL | Data e hora da última atualização |
| USER_AT | VARCHAR(255) | NULL | Login do usuário que originou o evento, quando aplicável |

> **Check:** `CHK_OUTBOX_EVENT_STATUS` trava `STATUS` ao vocabulário fechado (ver seção Checks de Domínio)

---

### SCOS_OUTBOX_EVENT_LOG

**Descrição:** Registra cada tentativa de processamento de um evento — substitui `SCOS_INTEGRATION_KEYCLOAK_LOG`. Uma tentativa por linha; um evento com retry tem múltiplas linhas. `CONSUMER` identifica qual worker processou (`KEYCLOAK`, `SISTEMA_PONTO`, `PGMQ_RELAY`...). Tabela imutável.

| Campo | Tipo | Restrição | Descrição |
|---|---|---|---|
| OUTBOX_EVENT_LOG_ID | BIGINT | PK | Identificador único do log, gerado automaticamente |
| OUTBOX_EVENT_ID | BIGINT | FK, NOT NULL | Referência ao evento que gerou esta tentativa |
| CONSUMER | VARCHAR(255) | NOT NULL | Worker/processo que executou a tentativa |
| SUCCESS | BOOLEAN | NOT NULL | Indica se esta tentativa específica foi bem-sucedida |
| RESPONSE | VARCHAR(5000) | NULL | Corpo da resposta ou mensagem de erro |
| CREATED_AT | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Data e hora exata da tentativa. Imutável |
| USER_AT | VARCHAR(100) | NULL | Login do usuário ou processo que gerou o log |

> **Índice:** `IDX_OUTBOX_EVENT_LOG_EVENT_ID` em `OUTBOX_EVENT_ID`

---

### SCOS_OUTBOX_EVENT_DEAD_LETTER

**Descrição:** Fila de mensagens mortas — substitui `SCOS_INTEGRATION_MESSAGE_INVALID`, agora com contexto suficiente pra reprocessamento manual (a versão antiga só guardava o texto cru). `OUTBOX_EVENT_ID` fica nulo apenas quando o payload chegou corrompido antes de virar um evento válido (ex: JSON malformado recebido por webhook). Tabela imutável.

| Campo | Tipo | Restrição | Descrição |
|---|---|---|---|
| OUTBOX_EVENT_DEAD_LETTER_ID | BIGINT | PK | Identificador único, gerado automaticamente |
| OUTBOX_EVENT_ID | BIGINT | FK, NULL | Referência ao evento de origem. Nulo se o payload nunca chegou a ser um evento válido |
| TOPIC | VARCHAR(255) | NOT NULL | Tópico de origem |
| SOURCE | VARCHAR(255) | NOT NULL | Origem específica — ex: `DIRECT_API:KEYCLOAK`, `PGMQ:employee_events` |
| PAYLOAD | TEXT | NOT NULL | Conteúdo que não pôde ser processado |
| ERROR_TYPE | VARCHAR(100) | NOT NULL | Classificação — ex: `MAX_RETRIES_EXCEEDED`, `VALIDATION`, `TIMEOUT` |
| RETRY_COUNT | INT | NOT NULL, DEFAULT 0 | Tentativas realizadas antes de desistir |
| CREATED_AT | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Data e hora do registro. Imutável |
| USER_AT | VARCHAR(100) | NULL | Login do processo que registrou |

> **Índice:** `IDX_OUTBOX_DEAD_LETTER_TOPIC_CREATED` em `(TOPIC, CREATED_AT DESC)`

---

## Triggers

### TRG_BEFORE_INSERT_*_STATUS_HISTORY

**Descrição:** Trigger `BEFORE INSERT`, aplicada às três tabelas de histórico de status (`SCOS_COMPANY_STATUS_HISTORY`, `SCOS_EMPLOYEE_STATUS_HISTORY`, `SCOS_LOGIN_STATUS_HISTORY`). Resolve dois problemas que o `CHECK` de tabela sozinho não cobre, por exigirem consulta a outras tabelas:

1. **Deriva `PREVIOUS_STATUS`** a partir do último registro real da própria tabela — elimina o risco da aplicação informar um valor incorreto ou desatualizado.
2. **Valida o `ENTITY_TYPE` do motivo** — garante que a FK preenchida (`REASON_ACTIVATE_ID`, `REASON_INACTIVATE_ID`, `REASON_DISABLE_ID` ou `REASON_ENABLE_ID`) referencia um motivo cadastrado para a entidade correta, impedindo, por exemplo, que um motivo de `COMPANY` seja usado em um histórico de `EMPLOYEE`.

Implementada como **uma função por entidade**, não uma função genérica com SQL dinâmico — mais verboso, porém mais simples de depurar e sem risco de erro de tipo em tempo de execução.

Exemplo completo para `SCOS_COMPANY_STATUS_HISTORY` — replicar para as outras duas trocando `company_id` → `employee_id`/`login_id` e `'COMPANY'` → `'EMPLOYEE'`/`'LOGIN'`:

```sql
CREATE OR REPLACE FUNCTION fn_before_insert_company_status_history()
RETURNS TRIGGER AS $$
DECLARE
  v_entity_type VARCHAR(20);
BEGIN
  -- (1) deriva o status anterior a partir do último registro real
  SELECT status INTO NEW.previous_status
  FROM scos_company_status_history
  WHERE company_id = NEW.company_id
  ORDER BY created_at DESC
  LIMIT 1;

  -- (2) valida que cada motivo informado pertence a ENTITY_TYPE = 'COMPANY'
  IF NEW.reason_activate_id IS NOT NULL THEN
    SELECT entity_type INTO v_entity_type FROM scos_reason_activate WHERE id = NEW.reason_activate_id;
    IF v_entity_type IS DISTINCT FROM 'COMPANY' THEN
      RAISE EXCEPTION 'reason_activate_id % não pertence a entity_type COMPANY', NEW.reason_activate_id;
    END IF;
  END IF;

  IF NEW.reason_inactivate_id IS NOT NULL THEN
    SELECT entity_type INTO v_entity_type FROM scos_reason_inactivate WHERE id = NEW.reason_inactivate_id;
    IF v_entity_type IS DISTINCT FROM 'COMPANY' THEN
      RAISE EXCEPTION 'reason_inactivate_id % não pertence a entity_type COMPANY', NEW.reason_inactivate_id;
    END IF;
  END IF;

  IF NEW.reason_disable_id IS NOT NULL THEN
    SELECT entity_type INTO v_entity_type FROM scos_reason_disable WHERE id = NEW.reason_disable_id;
    IF v_entity_type IS DISTINCT FROM 'COMPANY' THEN
      RAISE EXCEPTION 'reason_disable_id % não pertence a entity_type COMPANY', NEW.reason_disable_id;
    END IF;
  END IF;

  IF NEW.reason_enable_id IS NOT NULL THEN
    SELECT entity_type INTO v_entity_type FROM scos_reason_enable WHERE id = NEW.reason_enable_id;
    IF v_entity_type IS DISTINCT FROM 'COMPANY' THEN
      RAISE EXCEPTION 'reason_enable_id % não pertence a entity_type COMPANY', NEW.reason_enable_id;
    END IF;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_before_insert_company_status_history
  BEFORE INSERT ON scos_company_status_history
  FOR EACH ROW
  EXECUTE FUNCTION fn_before_insert_company_status_history();
```

**Tabelas que recebem esta trigger:** `SCOS_COMPANY_STATUS_HISTORY`, `SCOS_EMPLOYEE_STATUS_HISTORY`, `SCOS_LOGIN_STATUS_HISTORY`.

**Transições válidas** (`SCOS_LOGIN_STATUS_HISTORY` usa `BLOCKED` no lugar de `DISABLED`) — cada uma exige exatamente a FK de motivo indicada, as demais ficam nulas:

| PREVIOUS_STATUS | STATUS | Motivo obrigatório |
|---|---|---|
| *(nenhum — criação)* | ACTIVE | REASON_ACTIVATE_ID |
| INACTIVE | ACTIVE | REASON_ACTIVATE_ID |
| DISABLED | ACTIVE | REASON_ENABLE_ID |
| ACTIVE | INACTIVE | REASON_INACTIVATE_ID |
| DISABLED | INACTIVE | REASON_INACTIVATE_ID |
| ACTIVE | DISABLED | REASON_DISABLE_ID |

> **Premissa assumida:** `INACTIVE → DISABLED`/`BLOCKED` não é transição válida (não é coerente bloquear temporariamente algo já encerrado definitivamente) — por isso não consta na tabela acima. Se a premissa estiver errada, precisa de uma linha adicional.

---

### TRG_SYNC_*_STATUS

**Descrição:** Resolve o risco de dessincronia da decisão de manter `STATUS` como coluna cache em `SCOS_COMPANY`/`SCOS_EMPLOYEE`/`SCOS_LOGIN`. Trigger `AFTER INSERT` (roda depois que a linha de histórico já foi validada e gravada) que apenas propaga `NEW.status` para a tabela principal — não precisa saber nada sobre motivo ou regra de negócio, só copia o valor que a trigger `TRG_BEFORE_INSERT_*_STATUS_HISTORY` já validou. Como a aplicação **sempre** insere no histórico primeiro (nunca faz `UPDATE` direto na tabela principal), essa sincronização é determinística e não precisa de variável de sessão nem lógica adicional.

Exemplo completo para `SCOS_COMPANY` — replicar trocando `company_id`/`scos_company` por `employee_id`/`scos_employee` e `login_id`/`scos_login`:

```sql
CREATE OR REPLACE FUNCTION fn_sync_company_status()
RETURNS TRIGGER AS $$
BEGIN
  UPDATE scos_company
  SET status = NEW.status,
      updated_at = NEW.created_at
  WHERE company_id = NEW.company_id;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_sync_company_status
  AFTER INSERT ON scos_company_status_history
  FOR EACH ROW
  EXECUTE FUNCTION fn_sync_company_status();
```

Cada tabela de histórico agora dispara **duas** triggers em sequência no INSERT: `TRG_BEFORE_INSERT_*` (valida e deriva `PREVIOUS_STATUS`) e `TRG_SYNC_*_STATUS` (propaga pra tabela principal). São independentes, ordem entre elas não importa pro resultado final.

---

### TRG_BLOCK_DELETE_*

**Descrição:** Trigger `BEFORE DELETE` que bloqueia qualquer tentativa de exclusão em `SCOS_ADDRESS_TYPE`, `SCOS_CONTACT_TYPE`, `SCOS_OUTBOX_TOPIC` e nas 5 tabelas de motivo (`SCOS_REASON_ACTIVATE`, `SCOS_REASON_INACTIVATE`, `SCOS_REASON_DISABLE`, `SCOS_REASON_ENABLE`, `SCOS_REASON_POSITION_CHANGE`). A única forma de remover um registro da seleção é desativá-lo, marcando `ACTIVE` como falso. **Uma única função é reaproveitada em todas** — ela não depende de coluna específica, apenas impede a operação e usa `TG_TABLE_NAME` para identificar a tabela na mensagem de erro.

```sql
CREATE OR REPLACE FUNCTION fn_block_delete()
RETURNS TRIGGER AS $$
BEGIN
  RAISE EXCEPTION 'DELETE não permitido em %; utilize UPDATE ... SET active = false', TG_TABLE_NAME;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_block_delete_address_type
  BEFORE DELETE ON scos_address_type
  FOR EACH ROW
  EXECUTE FUNCTION fn_block_delete();

CREATE TRIGGER trg_block_delete_contact_type
  BEFORE DELETE ON scos_contact_type
  FOR EACH ROW
  EXECUTE FUNCTION fn_block_delete();

CREATE TRIGGER trg_block_delete_outbox_topic
  BEFORE DELETE ON scos_outbox_topic
  FOR EACH ROW
  EXECUTE FUNCTION fn_block_delete();

CREATE TRIGGER trg_block_delete_reason_activate
  BEFORE DELETE ON scos_reason_activate
  FOR EACH ROW
  EXECUTE FUNCTION fn_block_delete();

CREATE TRIGGER trg_block_delete_reason_inactivate
  BEFORE DELETE ON scos_reason_inactivate
  FOR EACH ROW
  EXECUTE FUNCTION fn_block_delete();

CREATE TRIGGER trg_block_delete_reason_disable
  BEFORE DELETE ON scos_reason_disable
  FOR EACH ROW
  EXECUTE FUNCTION fn_block_delete();

CREATE TRIGGER trg_block_delete_reason_enable
  BEFORE DELETE ON scos_reason_enable
  FOR EACH ROW
  EXECUTE FUNCTION fn_block_delete();

CREATE TRIGGER trg_block_delete_reason_position_change
  BEFORE DELETE ON scos_reason_position_change
  FOR EACH ROW
  EXECUTE FUNCTION fn_block_delete();
```

> **Nota:** a trigger bloqueia no nível do banco, independente de role/permissão de quem executa. Como camada complementar (defesa em profundidade), também é possível `REVOKE DELETE` nessas 8 tabelas `FROM <role_da_aplicação>` — não incluído aqui por não haver visibilidade sobre o nome da role usada em produção.
> **Nota:** `SCOS_LEGAL_NATURE`/`SCOS_CNAE` ficam de fora de propósito — são código oficial (IBGE), não têm `ACTIVE` nem faz sentido "desativar" (confirmado, não pendente).

---

### TRG_VALIDATE_*_TYPE

**Descrição:** Trigger `BEFORE INSERT OR UPDATE OF <fk_column>` nas 4 tabelas que referenciam `SCOS_ADDRESS_TYPE`/`SCOS_CONTACT_TYPE`. Garante que o tipo selecionado pertence ao `ENTITY_TYPE` correto — sem isso, nada impede um endereço de funcionário de referenciar um tipo cadastrado para `COMPANY` (a FK sozinha não distingue, já que a tabela é compartilhada). Dispara em `UPDATE` apenas quando a coluna de FK é alterada (`OF <coluna>`), não em qualquer update da linha.

Exemplo completo para `SCOS_EMPLOYEE_CONTACT` — replicar para as outras três trocando tabela/coluna/`ENTITY_TYPE` esperado conforme a tabela de referência abaixo:

```sql
CREATE OR REPLACE FUNCTION fn_validate_employee_contact_type()
RETURNS TRIGGER AS $$
DECLARE
  v_entity_type VARCHAR(20);
BEGIN
  SELECT entity_type INTO v_entity_type
  FROM scos_contact_type
  WHERE contact_type_id = NEW.contact_type_id;

  IF v_entity_type IS DISTINCT FROM 'EMPLOYEE' THEN
    RAISE EXCEPTION 'contact_type_id % não pertence a entity_type EMPLOYEE', NEW.contact_type_id;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_employee_contact_type
  BEFORE INSERT OR UPDATE OF contact_type_id ON scos_employee_contact
  FOR EACH ROW
  EXECUTE FUNCTION fn_validate_employee_contact_type();
```

**Replicação nas demais tabelas:**

| Tabela | Função/Trigger | Tabela de referência | Coluna FK | `ENTITY_TYPE` esperado |
|---|---|---|---|---|
| SCOS_EMPLOYEE_CONTACT | `fn/trg_validate_employee_contact_type` | `SCOS_CONTACT_TYPE` | `CONTACT_TYPE_ID` | `EMPLOYEE` *(exemplo acima)* |
| SCOS_COMPANY_CONTACT | `fn/trg_validate_company_contact_type` | `SCOS_CONTACT_TYPE` | `CONTACT_TYPE_ID` | `COMPANY` |
| SCOS_EMPLOYEE_ADDRESS | `fn/trg_validate_employee_address_type` | `SCOS_ADDRESS_TYPE` | `ADDRESS_TYPE_ID` | `EMPLOYEE` |
| SCOS_COMPANY_ADDRESS | `fn/trg_validate_company_address_type` | `SCOS_ADDRESS_TYPE` | `ADDRESS_TYPE_ID` | `COMPANY` |

---

### TRG_CLOSE_PREVIOUS_POSITION

**Descrição:** Trigger `BEFORE INSERT` em `SCOS_EMPLOYEE_POSITION_HISTORY`. Fecha a linha atualmente aberta do funcionário (`END_DATE = NEW.START_DATE`) antes da nova linha entrar — a aplicação só insere a linha nova, nunca precisa dar `UPDATE` na anterior.

```sql
CREATE OR REPLACE FUNCTION fn_close_previous_position()
RETURNS TRIGGER AS $$
BEGIN
  UPDATE scos_employee_position_history
  SET end_date = NEW.start_date
  WHERE employee_id = NEW.employee_id
    AND end_date IS NULL;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_close_previous_position
  BEFORE INSERT ON scos_employee_position_history
  FOR EACH ROW
  EXECUTE FUNCTION fn_close_previous_position();
```

---

### TRG_SYNC_EMPLOYEE_POSITION

**Descrição:** Trigger `AFTER INSERT` em `SCOS_EMPLOYEE_POSITION_HISTORY`. Mesma lógica de `TRG_SYNC_*_STATUS`: propaga `NEW.position_id` para `SCOS_EMPLOYEE.POSITION_ID` depois que a linha de histórico já foi gravada.

```sql
CREATE OR REPLACE FUNCTION fn_sync_employee_position()
RETURNS TRIGGER AS $$
BEGIN
  UPDATE scos_employee
  SET position_id = NEW.position_id,
      updated_at = NEW.created_at
  WHERE employee_id = NEW.employee_id;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_sync_employee_position
  AFTER INSERT ON scos_employee_position_history
  FOR EACH ROW
  EXECUTE FUNCTION fn_sync_employee_position();
```

---

### TRG_VALIDATE_LOGIN_PROFILE_NOT_PRIMARY

**Descrição:** Trigger `BEFORE INSERT OR UPDATE` em `SCOS_LOGIN_PROFILE`. Impede gravar ali um `PROFILE_ID` que já é o principal do login (`SCOS_LOGIN.PROFILE_ID`) — sem isso, o mesmo perfil poderia aparecer duplicado (uma vez como principal, outra como adicional), o que quebra a regra combinada por `UNION` usada pra listar "todos os perfis de um login".

```sql
CREATE OR REPLACE FUNCTION fn_validate_login_profile_not_primary()
RETURNS TRIGGER AS $$
DECLARE
  v_primary_profile_id BIGINT;
BEGIN
  SELECT profile_id INTO v_primary_profile_id
  FROM scos_login
  WHERE login_id = NEW.login_id;

  IF NEW.profile_id = v_primary_profile_id THEN
    RAISE EXCEPTION 'profile_id % já é o perfil principal do login % (SCOS_LOGIN.PROFILE_ID); não deve ser duplicado em SCOS_LOGIN_PROFILE', NEW.profile_id, NEW.login_id;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_login_profile_not_primary
  BEFORE INSERT OR UPDATE ON scos_login_profile
  FOR EACH ROW
  EXECUTE FUNCTION fn_validate_login_profile_not_primary();
```

---

## Checks de Domínio

Toda coluna de vocabulário fechado neste modelo (status, tipo, backend) é `VARCHAR` — o PostgreSQL não tem enforcement nativo sem um `CHECK` explícito. Sem isso, nada impede gravar um valor fora da lista documentada em qualquer um desses campos, por qualquer caminho de escrita (aplicação, migração manual, acesso direto ao banco). Os `CHECK` abaixo travam cada coluna ao seu vocabulário fechado. `CODE` nas tabelas de motivo/tipo fica de fora de propósito — é vocabulário aberto e extensível via `INSERT`, não um enum fixo de código, então não faz sentido travar com `CHECK`.

| Tabela | Coluna | Valores permitidos |
|---|---|---|
| SCOS_COMPANY | STATUS | `ACTIVE`, `INACTIVE`, `DISABLED` |
| SCOS_EMPLOYEE | STATUS | `ACTIVE`, `INACTIVE`, `DISABLED` |
| SCOS_EMPLOYEE | CONTRACT_TYPE | `CLT`, `PJ`, `ESTAGIO`, `TEMPORARIO` |
| SCOS_LOGIN | STATUS | `ACTIVE`, `INACTIVE`, `BLOCKED` |
| SCOS_LOGIN | TYPE | `EMPLOYEE`, `EXTERNAL`, `SERVICE` |
| SCOS_COMPANY_STATUS_HISTORY | STATUS | `ACTIVE`, `INACTIVE`, `DISABLED` |
| SCOS_COMPANY_STATUS_HISTORY | PREVIOUS_STATUS | `NULL`, `ACTIVE`, `INACTIVE`, `DISABLED` |
| SCOS_EMPLOYEE_STATUS_HISTORY | STATUS | `ACTIVE`, `INACTIVE`, `DISABLED` |
| SCOS_EMPLOYEE_STATUS_HISTORY | PREVIOUS_STATUS | `NULL`, `ACTIVE`, `INACTIVE`, `DISABLED` |
| SCOS_LOGIN_STATUS_HISTORY | STATUS | `ACTIVE`, `INACTIVE`, `BLOCKED` |
| SCOS_LOGIN_STATUS_HISTORY | PREVIOUS_STATUS | `NULL`, `ACTIVE`, `INACTIVE`, `BLOCKED` |
| SCOS_OUTBOX_EVENT | STATUS | `PENDING`, `PROCESSING`, `PROCESSED`, `FAILED` |
| SCOS_OUTBOX_TOPIC | BACKEND | `PGMQ`, `KAFKA`, `DIRECT_API` |
| SCOS_REASON_ACTIVATE | ENTITY_TYPE | `COMPANY`, `EMPLOYEE`, `LOGIN` |
| SCOS_REASON_INACTIVATE | ENTITY_TYPE | `COMPANY`, `EMPLOYEE`, `LOGIN` |
| SCOS_REASON_DISABLE | ENTITY_TYPE | `COMPANY`, `EMPLOYEE`, `LOGIN` |
| SCOS_REASON_ENABLE | ENTITY_TYPE | `COMPANY`, `EMPLOYEE`, `LOGIN` |
| SCOS_ADDRESS_TYPE | ENTITY_TYPE | `COMPANY`, `EMPLOYEE` |
| SCOS_CONTACT_TYPE | ENTITY_TYPE | `COMPANY`, `EMPLOYEE` |
| SCOS_POSITION_WORK_SCHEDULE | DAY_OF_WEEK | `MONDAY`, `TUESDAY`, `WEDNESDAY`, `THURSDAY`, `FRIDAY`, `SATURDAY`, `SUNDAY` |
| SCOS_EMPLOYEE_WORK_SCHEDULE | DAY_OF_WEEK | `MONDAY`, `TUESDAY`, `WEDNESDAY`, `THURSDAY`, `FRIDAY`, `SATURDAY`, `SUNDAY` |

```sql
ALTER TABLE scos_company  ADD CONSTRAINT chk_company_status  CHECK (status IN ('ACTIVE', 'INACTIVE', 'DISABLED'));
ALTER TABLE scos_employee
  ADD CONSTRAINT chk_employee_status        CHECK (status IN ('ACTIVE', 'INACTIVE', 'DISABLED')),
  ADD CONSTRAINT chk_employee_contract_type CHECK (contract_type IN ('CLT', 'PJ', 'ESTAGIO', 'TEMPORARIO'));

ALTER TABLE scos_login
  ADD CONSTRAINT chk_login_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED')),
  ADD CONSTRAINT chk_login_type   CHECK (type IN ('EMPLOYEE', 'EXTERNAL', 'SERVICE'));

ALTER TABLE scos_company_status_history
  ADD CONSTRAINT chk_company_status_history_status   CHECK (status IN ('ACTIVE', 'INACTIVE', 'DISABLED')),
  ADD CONSTRAINT chk_company_status_history_previous CHECK (previous_status IS NULL OR previous_status IN ('ACTIVE', 'INACTIVE', 'DISABLED'));

ALTER TABLE scos_employee_status_history
  ADD CONSTRAINT chk_employee_status_history_status   CHECK (status IN ('ACTIVE', 'INACTIVE', 'DISABLED')),
  ADD CONSTRAINT chk_employee_status_history_previous CHECK (previous_status IS NULL OR previous_status IN ('ACTIVE', 'INACTIVE', 'DISABLED'));

ALTER TABLE scos_login_status_history
  ADD CONSTRAINT chk_login_status_history_status   CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED')),
  ADD CONSTRAINT chk_login_status_history_previous CHECK (previous_status IS NULL OR previous_status IN ('ACTIVE', 'INACTIVE', 'BLOCKED'));

ALTER TABLE scos_outbox_event ADD CONSTRAINT chk_outbox_event_status  CHECK (status  IN ('PENDING', 'PROCESSING', 'PROCESSED', 'FAILED'));
ALTER TABLE scos_outbox_topic ADD CONSTRAINT chk_outbox_topic_backend CHECK (backend IN ('PGMQ', 'KAFKA', 'DIRECT_API'));

ALTER TABLE scos_reason_activate   ADD CONSTRAINT chk_reason_activate_entity_type   CHECK (entity_type IN ('COMPANY', 'EMPLOYEE', 'LOGIN'));
ALTER TABLE scos_reason_inactivate ADD CONSTRAINT chk_reason_inactivate_entity_type CHECK (entity_type IN ('COMPANY', 'EMPLOYEE', 'LOGIN'));
ALTER TABLE scos_reason_disable    ADD CONSTRAINT chk_reason_disable_entity_type    CHECK (entity_type IN ('COMPANY', 'EMPLOYEE', 'LOGIN'));
ALTER TABLE scos_reason_enable     ADD CONSTRAINT chk_reason_enable_entity_type     CHECK (entity_type IN ('COMPANY', 'EMPLOYEE', 'LOGIN'));

ALTER TABLE scos_address_type ADD CONSTRAINT chk_address_type_entity_type CHECK (entity_type IN ('COMPANY', 'EMPLOYEE'));
ALTER TABLE scos_contact_type ADD CONSTRAINT chk_contact_type_entity_type CHECK (entity_type IN ('COMPANY', 'EMPLOYEE'));

ALTER TABLE scos_position_work_schedule ADD CONSTRAINT chk_position_work_schedule_day CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'));
ALTER TABLE scos_employee_work_schedule ADD CONSTRAINT chk_employee_work_schedule_day CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'));
```

---

## Índices de Performance

Resumo de todos os índices adicionados nesta atualização. O índice antigo `IDX_INTEGRATION_KEYCLOAK_ID_SCOS_INTEGRATION_KEYCLOAK_LOG` foi substituído pelo equivalente em `SCOS_OUTBOX_EVENT_LOG` abaixo, já que a tabela original deixou de existir.

| Tabela | Índice | Tipo | Justificativa |
|---|---|---|---|
| SCOS_COMPANY_STATUS_HISTORY | `(COMPANY_ID, CREATED_AT DESC)` | B-tree | Consulta de status atual (`ORDER BY created_at DESC LIMIT 1`) e usado pela trigger a cada INSERT |
| SCOS_EMPLOYEE_STATUS_HISTORY | `(EMPLOYEE_ID, CREATED_AT DESC)` | B-tree | Idem |
| SCOS_LOGIN_STATUS_HISTORY | `(LOGIN_ID, CREATED_AT DESC)` | B-tree | Idem |
| SCOS_REASON_ACTIVATE | `(ENTITY_TYPE) WHERE ACTIVE = TRUE` | B-tree parcial | Lista de seleção filtrada por entidade, ignora motivos inativos |
| SCOS_REASON_INACTIVATE | `(ENTITY_TYPE) WHERE ACTIVE = TRUE` | B-tree parcial | Idem |
| SCOS_REASON_DISABLE | `(ENTITY_TYPE) WHERE ACTIVE = TRUE` | B-tree parcial | Idem |
| SCOS_REASON_ENABLE | `(ENTITY_TYPE) WHERE ACTIVE = TRUE` | B-tree parcial | Idem |
| SCOS_COMPANY | `(LEGAL_NATURE_ID)` | B-tree | Suporte a JOIN/filtro por natureza jurídica |
| SCOS_COMPANY | `(CNAE_PRINCIPAL_ID)` | B-tree | Suporte a JOIN/filtro por CNAE principal |
| SCOS_COMPANY_CNAE_SECONDARY | `(CNAE_ID, COMPANY_ID)` | B-tree | Consulta reversa: empresas por CNAE secundário |
| SCOS_ADDRESS_TYPE | `(ENTITY_TYPE) WHERE ACTIVE = TRUE` | B-tree parcial | Lista de seleção filtrada por entidade |
| SCOS_CONTACT_TYPE | `(ENTITY_TYPE) WHERE ACTIVE = TRUE` | B-tree parcial | Lista de seleção filtrada por entidade |
| SCOS_EMPLOYEE_ADDRESS | `(ADDRESS_TYPE_ID)` | B-tree | Consulta reversa: quem usa este tipo, antes de desativar |
| SCOS_COMPANY_ADDRESS | `(ADDRESS_TYPE_ID)` | B-tree | Idem |
| SCOS_EMPLOYEE_CONTACT | `(CONTACT_TYPE_ID)` | B-tree | Idem |
| SCOS_COMPANY_CONTACT | `(CONTACT_TYPE_ID)` | B-tree | Idem |
| SCOS_OUTBOX_EVENT | `(TOPIC, CREATED_AT) WHERE STATUS = 'PENDING'` | B-tree parcial | Índice principal do polling: worker busca pendentes de um tópico em ordem de chegada |
| SCOS_OUTBOX_EVENT_LOG | `(OUTBOX_EVENT_ID)` | B-tree | Consulta de histórico de tentativas de um evento |
| SCOS_OUTBOX_EVENT_DEAD_LETTER | `(TOPIC, CREATED_AT DESC)` | B-tree | Revisão manual filtrada por tópico/integração, mais recentes primeiro |
| SCOS_EMPLOYEE_POSITION_HISTORY | `(EMPLOYEE_ID) WHERE END_DATE IS NULL` | B-tree único, parcial | Cargo atual do funcionário — `UNIQUE` garante nunca haver 2 linhas abertas ao mesmo tempo |
| SCOS_EMPLOYEE_POSITION_HISTORY | `(POSITION_ID)` | B-tree | Consulta reversa: quem já ocupou este cargo, histórico ou atual |
| SCOS_LOGIN_PROFILE | `(PROFILE_ID)` | B-tree | Consulta reversa: quais logins têm este perfil como adicional |
| SCOS_LOGIN | `(LAST_USED_AT) WHERE STATUS = 'ACTIVE'` | B-tree parcial | Suporte ao job de auto-inativação por desuso — varre só logins ativos, ignora quem já está `INACTIVE`/`BLOCKED` |

**Deliberadamente não incluídos:** índice em `REASON_ACTIVATE_ID` / `REASON_INACTIVATE_ID` / `REASON_DISABLE_ID` / `REASON_ENABLE_ID` dentro das tabelas de histórico de status, e em `REASON_POSITION_CHANGE_ID` em `SCOS_EMPLOYEE_POSITION_HISTORY`. Fariam sentido **se** surgir necessidade de relatório por motivo (ex: "quantidade de desligamentos por motivo no trimestre") — nesse caso, usar índice parcial (`WHERE reason_x_id IS NOT NULL`) para manter o tamanho baixo. Não foram adicionados especulativamente por falta de visibilidade sobre esse padrão de consulta; recomenda-se monitorar via `pg_stat_user_tables`/`pg_stat_statements` antes de criar.

---
