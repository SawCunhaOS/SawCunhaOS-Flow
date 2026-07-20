# Metadados da Permissão no Fluxo de Registro + Descrição i18n

**Data**: 2026-07-09  
**Status**: 🔄 Em Análise  
**Tipo**: 🔧 Refatoração

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

> **Regra**: Uma ideia = uma funcionalidade. Features independentes → arquivos separados.

- **Nome da funcionalidade**: `permission-metadata-registry-enrichment`
- **Resumo em uma frase**: Enriquece a definição da permissão (`group`, `subGroup`, `version`, `updatedAt` e descrição i18n via bundle de mensagens) e propaga esses metadados por todo o fluxo de registro gRPC até a tabela `SCOS_RESOURCE` — com **upsert nativo condicional PostgreSQL** (só atualiza quando `active` ou a data da definição muda), **transação atômica no lote** e **propagação do motivo de erro entre módulos** — habilitando separação/agrupamento no front e controle de atualização/desativação.

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade (metadados da permissão no registro)
- [x] Não mistura features independentes no mesmo arquivo
- [x] O nome é específico

**Fora deste arquivo (SRP — cada um vira sua própria ideia):**
- Endpoint REST de listagem de `resources` agrupados (`group`/`subGroup`) para atribuição no front (`GET_RESOURCE` já existe, sem delegate).
- Atribuição de permissões a perfil no front.
- Reconciliação/diff de permissões ausentes (deletadas do enum) no registry.

---

## 1️⃣ Visão

### Problema
A interface `ScosPermission` já foi editada com campos novos (`getCodeDescription`, `getGroup`, `getSubGroup`, `getVersion`, `getUpdatedAt`), mas o resto da cadeia não acompanhou:

- **Não compila.** O enum `ScosOrganizationPermission` só implementa `getPermission`; via `@Getter` expõe `getDescriptionPtBr`/`getDescriptionEng`/`getActive`. Faltam `getCodeDescription/getGroup/getSubGroup/getVersion/getUpdatedAt`.
- `ScosSystemRegistrationService` (L112-113) ainda chama `getDescriptionPtBr()/getDescriptionEng()`, que saíram da interface.
- As descrições pt/en estão **hardcoded** em cada uma das ~110 constantes do enum, sem controle i18n como os arquivos de erro.
- Sem `group`/`subGroup` persistidos, o front não consegue separar/agrupar permissões para atribuição, nem há `version`/`updatedAt` para controlar atualização/desativação da definição.

O proto `Resource` **já** tem os campos (`group`, `sub_group`, `version`, `updateAt`), mas nada os preenche nem persiste.

### Objetivo
Definição da permissão passa a carregar metadados completos (grupo, subgrupo, versão, data de atualização, descrição via chave i18n) e esses dados chegam íntegros à tabela `SCOS_RESOURCE`, no insert **e** no update. Sucesso = projeto compila; registro no startup grava `RESOURCE_GROUP`, `SUB_GROUP`, `VERSION`, `DEFINITION_UPDATED_AT` e descrição pt/en resolvida do bundle; constante com `active=false` marca `ACTIVE=false` na linha.

### Fora de Escopo
- Endpoint REST de listagem para o front (arquivo próprio).
- Login/validação: `vw_authority_response` e `vw_login_context` usam apenas o array `permission` — **não tocam**, sem impacto.
- Reconciliar permissões deletadas do enum (registry só faz upsert).

---

## 2️⃣ Requisitos

### Funcionais
- [ ] **RF-01**: Interface `ScosPermission` expõe o set final: `getPermission`, `getCodeDescription`, `getGroup`, `getSubGroup`, `getActive`, `getVersion`, `getUpdatedAt` (`LocalDate`).
- [ ] **RF-02**: Enum `ScosOrganizationPermission` passa a carregar por constante: `codeDescription` (chave de mensagem), `group`, `subGroup`, `version`, `updatedAt`, `active` — sem mais strings pt/en inline.
- [ ] **RF-03**: Novo bundle `messages_permission.properties` + `messages_permission_en.properties` no módulo do enum (infrastructure), chaveado por `getCodeDescription`.
- [ ] **RF-04**: `ScosSystemRegistrationService` resolve pt/en via `MessageSource` no startup e preenche o proto `Resource` completo (`code`, `description_pt`, `description_en`, `group`, `sub_group`, `version`, `updated_at`, `active`).
- [ ] **RF-05**: Cadeia servidora (`RegistreServiceImpl` → `RegistryResourceUseCaseBean` → `ResourceServiceBean`) propaga e persiste `group`, `subGroup`, `version`, `updatedAt` no **insert e no update**.
- [ ] **RF-06**: Desativação — constante com `active=false` re-registrada seta `ACTIVE=false` na tabela (soft), sem deletar a linha.
- [ ] **RF-07**: `ResourceServiceBean.register` usa **upsert nativo condicional** `INSERT ... ON CONFLICT (CODE) DO UPDATE ... WHERE ...` (1 query `@Modifying @Query nativeQuery=true` no `ResourceRepository`, substitui `findByCode().get()` + `findIdByCodeAndSystemCode` + `merge`/`update`); colunas de auditoria (`CREATED_AT`/`UPDATED_AT`/`USER_AT`) setadas no próprio SQL (`CREATED_AT` já tem default `NOW()`; `UPDATED_AT`/`USER_AT` são `NOT NULL` sem default → preencher explícito). Constraint de conflito: `UK_CODE_SCOS_RESOURCE` (só em `CODE`).
- [ ] **RF-08**: Registro do lote é **atômico** — `@Transactional(rollbackFor = ScosException.class)` cobre a iteração inteira (fronteira sobe para um `RegistryResourcesUseCase` batch; falha em 1 resource faz rollback de todos).
- [ ] **RF-09**: Motivo do erro **visível entre módulos** — `GrpcGlobalExceptionHandler` mapeia `ScosException` para `Status` de negócio (não `INTERNAL`) resolvendo `code`→`ExceptionCodeError.httpCode`→`Status` (404 `NOT_FOUND`, 409 `ALREADY_EXISTS`, 422 `FAILED_PRECONDITION`, 401 `UNAUTHENTICATED`, 400 `INVALID_ARGUMENT`, resto `INTERNAL`) + anexa o `code` em `io.grpc.Metadata` (trailer); `getMessage` passa `ex.getArgs()`; client (`ScosSystemRegistrationService`) lê `description` + `code` do trailer e loga/propaga o motivo real em vez de mensagem genérica.
- [ ] **RF-09b**: `ResourceServiceBean` troca `scosSystemRepository.findByCode(...).get()` por `orElseThrow(() -> new ScosException(SCOS_SYSTEM_001, systemCode))` — sistema inexistente vira erro de negócio `404 NOT_FOUND` visível, não `NoSuchElementException` → `INTERNAL "Internal server error"`. Novo código `SCOS_SYSTEM_001` no enum + mensagem pt/en.
- [ ] **RF-10**: **Conditional Upsert** — o ramo `DO UPDATE` só executa quando `active` **ou** `DEFINITION_UPDATED_AT` (data da definição) diferem da linha atual (`WHERE SCOS_RESOURCE.ACTIVE IS DISTINCT FROM EXCLUDED.ACTIVE OR SCOS_RESOURCE.DEFINITION_UPDATED_AT IS DISTINCT FROM EXCLUDED.DEFINITION_UPDATED_AT`). Sem mudança nesses campos → nenhum UPDATE, `UPDATED_AT` de auditoria não é bumpado à toa e não há dead tuple. `IS DISTINCT FROM` cobre null-safety.

### Não-Funcionais
- [ ] **RNF-01**: Zero impacto no login/validação (array `permission` inalterado).
- [ ] **RNF-02**: Sistema não publicado → editar `scos_resource.yml` direto (sem migration incremental).
- [ ] **RNF-03**: Compilação restaurada — interface ↔ enum ↔ registration service alinhados.
- [ ] **RNF-04**: Trilha de auditoria (`@Auditable`, listener Hibernate) **não dispara** no upsert nativo — aceito: `Resource` é metadado de sistema, auto-registrado; auditoria da linha fica nas colunas próprias.

---

## 3️⃣ Arquitetura

### Componentes Afetados
```
scos-security-starter (CONSUMIDOR/genérico)
├── specification/ScosPermission: ✅ já modificado (set final de métodos)
├── service/ScosSystemRegistrationService: modificação (resolver i18n + preencher proto)
└── config MessageSource do bundle de permissão: adição

flow-organization-infrastructure
├── enumaration/ScosOrganizationPermission: modificação (~110 constantes)
├── resources/messages_permission.properties: adição
└── resources/messages_permission_en.properties: adição

grpc/flow-organization-grpc-proto
└── proto/registry.proto: modificação (Resource: renomear updateAt→updated_at; manter description_pt/en)

grpc/flow-organization-grpc-boot
└── delegate/RegistreServiceImpl: modificação (mapear campos novos)

flow-organization-usecase
├── .../resource/registry/RegistryResourceInput: modificação (+campos)
└── .../resource/registry/RegistryResourceUseCaseBean: modificação (pass-through)

flow-organization-domain
├── .../resource/dto/RegisterResourceInput: modificação (+campos)
├── .../resource/internal/Resource: modificação (+colunas)
└── .../resource/service/ResourceServiceBean: modificação (persistir no insert e update)

flow-organization-boot
└── db/changelog/v1.0.0/tables/scos_resource.yml: modificação (+colunas)
```

### Fluxo Principal
```
ScosOrganizationPermission (enum)  →  codeDescription(key) + group + subGroup + version + updatedAt + active
        │ startup (starter)
        ▼
ScosSystemRegistrationService: MessageSource resolve key→pt/en  →  proto Resource completo
        │ gRPC registryResources()
        ▼
RegistreServiceImpl  →  RegistryResourceInput  →  RegistryResourceUseCaseBean (+systemCode)
        ▼
ResourceServiceBean.register(): upsert com todos os campos (insert E update)
        ▼
SCOS_RESOURCE: CODE, DESCRIPTION_PT/EN, RESOURCE_GROUP, SUB_GROUP, VERSION, DEFINITION_UPDATED_AT, ACTIVE
```

### Decisões Técnicas
| Decisão | Escolha | Alternativa Descartada | Motivo |
|---------|---------|------------------------|--------|
| Resolução i18n da descrição | `ScosSystemRegistrationService` resolve `key`→pt/en via `MessageSource`, envia texto resolvido | Registry guarda só o código; front resolve | Consumidor é dono do i18n (igual mensagens de erro); registry continua com texto pesquisável e sem depender de bundle no front |
| `version` + `updatedAt` da permissão | Colunas próprias (`VERSION`, `DEFINITION_UPDATED_AT`) | Reusar `UPDATED_AT` de auditoria | Versionamento da **definição** da permissão ≠ auditoria de linha; misturar os dois corrompe o significado do audit |
| Local do bundle de descrição | Novo `messages_permission*` no módulo do enum (infrastructure) | Reusar `messages_security` do starter | Cada sistema é dono das suas permissões; `messages_security` é do starter genérico (mensagens de auth) |
| Nome da coluna de grupo | `RESOURCE_GROUP` | `GROUP` | `GROUP` é palavra reservada SQL |
| Desativação de permissão | Constante mantida com `active=false` → upsert marca `ACTIVE=false` | Deletar constante do enum | Registry só faz upsert; deletar deixa linha órfã `ACTIVE=true` |
| Estratégia do upsert | **Conditional Upsert**: `DO UPDATE ... WHERE active/definition_updated_at IS DISTINCT FROM EXCLUDED` | Update incondicional a cada registro | Startup re-registra o lote inteiro toda vez; sem condição, todo restart bumpa `UPDATED_AT` e gera dead tuple mesmo sem mudança real |
| Alvo do `ON CONFLICT` | `(CODE)` | `(CODE, SYSTEM_ID)` | Constraint existente é `UK_CODE_SCOS_RESOURCE` só em `CODE`; `CODE` já é globalmente único |
| Mapeamento de erro gRPC | `code`→`ExceptionCodeError.httpCode`→`Status` de negócio + `code` no trailer `Metadata` | Sempre `Status.INTERNAL` com descrição | Client distingue NOT_FOUND/CONFLICT/regra de negócio e lê o motivo real; hoje tudo vira `INTERNAL` |
| Sistema inexistente no registro | `orElseThrow(new ScosException(SCOS_SYSTEM_001))` (404) | `.get()` → `NoSuchElementException` | `.get()` cai no fallback `Exception` → `INTERNAL "Internal server error"`, motivo escondido |

### Banco de Dados
- **Impacto**: ✅ Sim.
- Tabela `SCOS_RESOURCE` — adicionar colunas (edição direta do `scos_resource.yml`, sistema não publicado):
  - `RESOURCE_GROUP` `varchar(100)`
  - `SUB_GROUP` `varchar(100)`
  - `VERSION` `varchar(20)`
  - `DEFINITION_UPDATED_AT` `date`
- Mantém `DESCRIPTION_PT`/`DESCRIPTION_EN` (decisão i18n resolve no consumidor).
- Nulabilidade de `RESOURCE_GROUP`/`SUB_GROUP`/`VERSION`/`DEFINITION_UPDATED_AT`: definir na proposta (provável `nullable: false` com valor em todas as constantes).

### SQL do Upsert Condicional
```sql
INSERT INTO scos.SCOS_RESOURCE
    (RESOURCE_ID, SYSTEM_ID, CODE, DESCRIPTION_PT, DESCRIPTION_EN,
     RESOURCE_GROUP, SUB_GROUP, VERSION, DEFINITION_UPDATED_AT,
     ACTIVE, CREATED_AT, UPDATED_AT, USER_AT)
VALUES (gen_random_uuid(), :systemId, :code, :descriptionPt, :descriptionEn,
        :resourceGroup, :subGroup, :version, :definitionUpdatedAt,
        :active, NOW(), NOW(), :userAt)
ON CONFLICT (CODE) DO UPDATE SET
    DESCRIPTION_PT        = EXCLUDED.DESCRIPTION_PT,
    DESCRIPTION_EN        = EXCLUDED.DESCRIPTION_EN,
    RESOURCE_GROUP        = EXCLUDED.RESOURCE_GROUP,
    SUB_GROUP             = EXCLUDED.SUB_GROUP,
    VERSION               = EXCLUDED.VERSION,
    DEFINITION_UPDATED_AT = EXCLUDED.DEFINITION_UPDATED_AT,
    ACTIVE                = EXCLUDED.ACTIVE,
    UPDATED_AT            = NOW(),
    USER_AT               = EXCLUDED.USER_AT
WHERE  SCOS_RESOURCE.ACTIVE                IS DISTINCT FROM EXCLUDED.ACTIVE
   OR  SCOS_RESOURCE.DEFINITION_UPDATED_AT IS DISTINCT FROM EXCLUDED.DEFINITION_UPDATED_AT;
```
- `gen_random_uuid()` = mesmo `uuid_function` do Liquibase (PostgreSQL ≥13, core).
- `CREATED_AT` poderia usar o default `NOW()` da tabela, mas é setado no `INSERT` para não depender do default em ambientes com a coluna alterada.
- `WHERE` no ramo `DO UPDATE` = Conditional Upsert (RF-10): só grava quando `active`/`definition_updated_at` mudam.

---

## 4️⃣ Implementação

### Arquivos

**Novos**:
- `flow-organization-infrastructure/src/main/resources/messages_permission.properties` — descrições pt-BR chaveadas por `codeDescription`.
- `flow-organization-infrastructure/src/main/resources/messages_permission_en.properties` — descrições en.
- Config `MessageSource` do bundle de permissão (no starter ou exposto pelo consumidor) — definir na proposta.

**Modificados**:
- `scos-security-starter/.../specification/ScosPermission.java` — set final de métodos (já iniciado).
- `scos-security-starter/.../service/ScosSystemRegistrationService.java` — resolver i18n + preencher proto completo.
- `flow-organization-infrastructure/.../enumaration/ScosOrganizationPermission.java` — refatorar ~110 constantes.
- `grpc/flow-organization-grpc-proto/.../registry.proto` — `Resource`: renomear `updateAt`→`updated_at`; manter `description_pt/en`.
- `grpc/flow-organization-grpc-boot/.../delegate/RegistreServiceImpl.java` — mapear campos novos.
- `flow-organization-usecase/.../resource/registry/RegistryResourceInput.java` + `RegistryResourceUseCaseBean.java` — pass-through dos campos.
- `flow-organization-domain/.../resource/dto/RegisterResourceInput.java` — +campos.
- `flow-organization-domain/.../resource/internal/Resource.java` — +colunas.
- `flow-organization-domain/.../resource/internal/ResourceRepository.java` — +`upsert(...)` `@Modifying @Query(nativeQuery=true)`; remover `findIdByCodeAndSystemCode` (dead após upsert).
- `flow-organization-domain/.../resource/service/ResourceServiceBean.java` — `orElseThrow(SCOS_SYSTEM_001)` + chamar `upsert` condicional (substitui find + merge/update).
- `flow-organization-shared/.../exception/ExceptionCodeError.java` — +`SCOS_SYSTEM_001` (404, `SCOS_TITLE_NOT_FOUND`).
- `flow-organization-shared/.../resources/scos_message_organization.properties` + `_en.properties` — +mensagem `SCOS_SYSTEM_001`.
- `grpc/flow-organization-grpc-boot/.../handler/GrpcGlobalExceptionHandler.java` — `handleScosException` mapeia `httpCode`→`Status` de negócio + `code` no trailer; passa `ex.getArgs()` ao `getMessage`.
- `flow-organization-boot/.../tables/scos_resource.yml` — +colunas.

### Tarefas
- [ ] **T-01**: Fechar set final da interface `ScosPermission`.
- [ ] **T-02**: Refatorar enum + criar bundle `messages_permission*` (keys ↔ constantes).
- [ ] **T-03**: `MessageSource` dedicado do bundle de permissão (bean qualificado, sem colidir com o primário).
- [ ] **T-04**: `ScosSystemRegistrationService` resolve i18n e preenche proto completo.
- [ ] **T-05**: Proto `registry.proto` + regenerar stubs; ajustar `RegistreServiceImpl`.
- [ ] **T-06**: Propagar campos em usecase (`RegistryResourceInput`/Bean) e domain (`RegisterResourceInput`).
- [ ] **T-07**: `Resource` entity + `scos_resource.yml` + `ResourceRepository.upsert` (nativo condicional) + `ResourceServiceBean` (`orElseThrow(SCOS_SYSTEM_001)` + upsert).
- [ ] **T-08**: `ExceptionCodeError.SCOS_SYSTEM_001` + mensagens pt/en + `GrpcGlobalExceptionHandler` (map `httpCode`→`Status` + trailer `code`) + leitura no client `ScosSystemRegistrationService`.
- [ ] **T-09**: Teste de integração (Testcontainers) — startup registra e persiste metadados; `active=false` marca linha inativa; **re-registro sem mudança não bumpa `UPDATED_AT`** (prova do Conditional Upsert); sistema inexistente → erro `NOT_FOUND` com motivo.

### Riscos e Edge Cases
1. **`MessageSource` cross-módulo**: o starter é genérico e o bundle vive no consumidor (infrastructure). Precisa de bean `MessageSource` com basename `messages_permission`, **qualificado**, para não colidir com o `MessageSource` primário da aplicação. Definir quem declara o bean (starter via propriedade de basename × consumidor expõe o bean).
2. **Proto `updateAt`**: campo é `string` e com typo. proto3 não tem Date → transportar ISO-8601 (`string`) e parsear para `LocalDate` no servidor. Renomear `updateAt`→`updated_at` exige **regenerar stubs** em grpc-proto, grpc-boot e starter.
3. **`GROUP` reservado SQL** → coluna `RESOURCE_GROUP`.
4. **`updatedAt` `LocalDate` hardcoded por constante**: exige manutenção manual quando a definição muda; considerar derivar de `version` ou documentar a convenção.
5. **Deletar constante do enum** deixa linha `ACTIVE=true` órfã (registry só faz upsert, não reconcilia ausentes) → política oficial: manter constante com `active=false`.
6. **Null-safety** entre `getActive()` (`Boolean`), proto `active` (`bool`) e `Resource.active` (`boolean` primitivo) — garantir não-nulo na origem.
7. **Conditional Upsert só olha `active`/`DEFINITION_UPDATED_AT`**: se `DESCRIPTION_PT/EN`, `RESOURCE_GROUP`, `SUB_GROUP` ou `VERSION` mudarem **sem** bump de `DEFINITION_UPDATED_AT` (nem `active`), o `DO UPDATE` **não dispara** e a edição não persiste. Convenção obrigatória: **qualquer** alteração da definição da permissão bumpa `updatedAt` (liga com o risco 4). Alternativa a avaliar na proposta: incluir `VERSION` no `WHERE` ou comparar todos os campos com `IS DISTINCT FROM`.
8. **`@Modifying @Query` nativo no `BaseJpaRepository`** (hypersistence-utils) — validar que a fragment de `@Query` convive com os métodos `merge`/`update` da lib; considerar `@Modifying(clearAutomatically = true)` se houver leitura da entidade na mesma transação após o upsert.

---

## 📎 Referências
- `scos-security-starter/.../specification/ScosPermission.java` (interface em edição)
- `flow-organization-infrastructure/.../enumaration/ScosOrganizationPermission.java` (enum)
- `grpc/flow-organization-grpc-proto/.../registry.proto` (proto `Resource`)
- `flow-organization-boot/.../view/vw_authority_response.sql` (prova de não-impacto no login)
- Padrão i18n de referência: `messages_security.properties` (formato dos arquivos de erro)

---
