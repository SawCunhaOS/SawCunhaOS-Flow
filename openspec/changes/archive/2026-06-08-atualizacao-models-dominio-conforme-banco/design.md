## Context

O módulo `scos-organization-domain` contém as entidades JPA e repositórios que mapeiam o schema PostgreSQL gerenciado por Liquibase. As migrations (`v1.0.0`) já estão corretas e alinhadas ao `domain_model.md`. O problema está no código Java: os models foram criados antes das migrations serem finalizadas e nunca foram sincronizados.

Resultado atual: Hibernate em modo `validate` lançaria erros ao iniciar — campos mapeados inexistentes no banco, colunas PK com nomes errados, e seis tabelas sem entidade correspondente.

**Padrões do projeto** que devem ser seguidos integralmente (não criar novos):
- Entidades com `CREATED_AT + UPDATED_AT + USER_AT` → estender `BaseEntity` (scos-foundation)
- Entidades imutáveis (sem `UPDATED_AT`) → declarar `CREATED_AT` e `USER_AT` diretamente, sem estender `BaseEntity`
- Todas as entidades → `@Auditable` + Lombok (`@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`)
- Entidades com dados pessoais (LGPD) → `@Auditable(auditRead = true)`
- PK composta → classe `@Embeddable` separada com `@EmbeddedId`
- Value Objects da foundation (`Cnpj`, `Cpf`, `Email`) → `@Embedded + @AttributeOverride`
- Campos `JSONB` → `@Type(JsonBinaryType.class)` do Hypersistence Utils
- Repositórios → `interface @Repository` estendendo `BaseJpaRepository<T,ID> + JpaSpecificationExecutor<T> + QuerydslPredicateExecutor<T>`
- Predicados QueryDSL → campo estático do Q-type na própria interface

## Goals / Non-Goals

**Goals:**
- Sincronizar 100% dos models JPA com o schema Liquibase existente
- Criar as 6 entidades JPA ausentes com repositórios correspondentes
- Corrigir enums, PKs e campos divergentes
- Aplicar classificação LGPD (`auditRead`) em todas as entidades
- Zero alteração no schema do banco

**Non-Goals:**
- Modificar migrations Liquibase
- Alterar camadas fora de `domain/model` e `domain/repository`
- Implementar lógica de negócio nova além do ajuste de métodos afetados pelos campos removidos/adicionados

## Decisions

### D-01: Fonte da verdade é o Liquibase, não o `domain_model.md`

Em caso de divergência entre `domain_model.md` e as migrations `.yml`, as migrations são a fonte definitiva — elas definem o schema real do banco. O `domain_model.md` é documentação descritiva.

**Alternativa descartada**: Usar `domain_model.md` como fonte e gerar novas migrations. Descartado porque as migrations já existem e estão corretas; criar migrations redundantes geraria conflito de schema.

### D-02: `ProfileResource`, `IntegrationKeycloakLog` e `IntegrationMessageInvalid` NÃO estendem `BaseEntity`

Essas três entidades não possuem coluna `UPDATED_AT` no banco. `BaseEntity` mapeia `UPDATED_AT` com `@UpdateTimestamp` — incluí-la faria o Hibernate tentar escrever em coluna inexistente.

**Solução**: Declarar `CREATED_AT` (`@CreationTimestamp`) e `USER_AT` diretamente na entidade, sem herança.

### D-03: `CompanyAddress` migra para PK composta (`CompanyAddressPk`)

O banco define `COMPANY_ID_ADDRESS` (ID externo, sem auto-geração) + `COMPANY_ID` (FK) como PK composta — exatamente o mesmo padrão já usado em `EmployeeAddress` + `EmployeeAddressPk`. Usar PK simples auto-gerada seria um mapeamento incorreto.

**Impacto**: `CompanyAddressRepository` muda de `BaseJpaRepository<CompanyAddress, Long>` para `BaseJpaRepository<CompanyAddress, CompanyAddressPk>`. Predicados QueryDSL precisam ser revisados.

### D-04: `StatusCompany.DELETED` renomeado para `DISABLED`

O banco usa `ACTIVE/INACTIVE/DISABLED` para companies e employees. O enum atual tem `DELETED` em vez de `DISABLED`, divergindo do schema. Todos os repositórios que referenciam `StatusCompany.DELETED` devem ser atualizados para `DISABLED` na mesma tarefa para evitar quebra de compilação.

### D-05: `JSONB` mapeado com `JsonBinaryType` do Hypersistence Utils

O campo `REQUEST` de `IntegrationKeycloak` é `JSONB` no PostgreSQL. O Hypersistence Utils já é dependência do projeto — usar `@Type(JsonBinaryType.class)` com tipo `String` é o padrão mais simples e consistente.

**Alternativa descartada**: Mapear como `Map<String, Object>` com Jackson. Adiciona complexidade desnecessária para uma tabela de log/fila.

### D-06: Entidades com dados pessoais usam `@Auditable(auditRead = true)`

Para conformidade LGPD, as entidades `Employee`, `EmployeeContact`, `EmployeeAddress`, `Login` e `IntegrationKeycloak` ativam o rastreio de leitura via Hibernate `PostLoad`, que emite `ActionType.SELECT` na trilha de auditoria.

Entidades sem PII (dados corporativos, configurações, permissões) usam `@Auditable` padrão (`auditRead = false`).

## Risks / Trade-offs

- **Quebra de compilação em outras camadas** → Campos removidos (ex: `Employee.active`, `Login.password`) são possivelmente referenciados em application/infrastructure/api. Essas camadas vão quebrar ao compilar. *Mitigação*: Escopo desta change é apenas domain — camadas externas devem ser corrigidas na sequência, em change separada.

- **`CompanyAddressRepository` com PK composta** → `deleteByCompanyIdAndId` usa derivação Spring Data com `Long id` — após mudança para `CompanyAddressPk`, o método não compilará. *Mitigação*: Substituir por `deleteById(new CompanyAddressPk(...))` ou `@Query` explícita.

- **Ordem de execução das tarefas** → `StatusCompany.DELETED → DISABLED` (T-01) deve ser feito antes de qualquer outra tarefa que envolva repositórios de company, para evitar erro de compilação em cascata.

- **`PartnersConfiguration` tem método `getValueOrDefaultValue()`** que depende do campo `defaultValue` a ser removido. *Mitigação*: Simplificar para retornar `value` diretamente.

## Migration Plan

Nenhuma migration de banco necessária. Apenas código Java:

1. Executar T-01 primeiro (corrigir `StatusCompany`) para garantir compilação dos repositórios
2. Demais tarefas podem ser executadas em qualquer ordem dentro do mesmo escopo
3. Validar com Hibernate `spring.jpa.hibernate.ddl-auto: validate` após todas as mudanças
4. Rollback: revert do branch — nenhuma alteração de dados ou schema

## Open Questions

- `IntegrationMessageInvalid.messageInvalid`: o campo é `VARCHAR(5000)` no banco. Mapear como `String` é suficiente ou deve ser `TEXT`? (Confirmar com Liquibase — a migration usa `varchar(5000)`, então `String` é correto.)
