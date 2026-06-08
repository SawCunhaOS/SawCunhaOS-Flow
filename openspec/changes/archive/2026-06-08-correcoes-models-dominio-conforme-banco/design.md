## Context

Change `atualizacao-models-dominio-conforme-banco` foi aplicado com 52 tarefas. Três problemas identificados após revisão:
1. `DELETED` foi omitido dos enums — a semântica de soft delete permanente ficou sem representação
2. VOs de `Email` não foram aplicados, violando o padrão estabelecido pelo projeto
3. `RESPONSIBLE_PERSON` foi removido de `CompanyContact` indevidamente — campo necessário para identificar o responsável de contato

## Goals / Non-Goals

**Goals:**
- Adicionar `DELETED` como status de soft delete permanente em `StatusCompany` e `StatusEmployee`
- Aplicar VO `Email` em todos os campos de email nas entidades JPA (`CompanyContact`, `Employee`, `IntegrationKeycloak`)
- Restaurar `RESPONSIBLE_PERSON` em `CompanyContact` + Liquibase + `domain_model.md`
- Atualizar o arquivo de ideia original

**Non-Goals:**
- Alterar camadas de application, infrastructure ou api
- Criar novos enums de status para outras entidades
- Modificar a lógica de validação do VO `Email`

## Decisions

### D-01: Semântica DELETED vs DISABLED

`DISABLED` = desativação reversível (pode chamar `activate()`). `DELETED` = soft delete permanente (sem transição de saída). Guards de `activate()`/`inactivate()` bloqueiam transição a partir de `DELETED` com mesma exceção `SCOS_COMPANY_007` usada para `DISABLED`.

### D-02: Email VO com @AttributeOverride

Padrão já definido: `@Embedded @AttributeOverride(name = "email", column = @Column(name = "EMAIL"))`. O campo interno do VO `Email` se chama `email`. Não usar `insertable/updatable = false` — é o único campo do VO nesta coluna.

### D-03: IntegrationKeycloak usa Email VO

Mesmo sendo entidade de integração (dados de fila externa), o VO garante que e-mails inválidos sejam rejeitados em tempo de criação. Hibernate usa `protected Email()` ao carregar do banco — sem validação na leitura. Comportamento correto.

### D-04: RESPONSIBLE_PERSON nullable

Novo campo `RESPONSIBLE_PERSON VARCHAR(255)` adicionado como `NOT NULL` no Liquibase v1.0.0. O projeto está em desenvolvimento (nenhuma instância de produção). Modifica o changeset existente (id `20260606-Samuel.Cunha-005`) pois a migration ainda não foi aplicada em produção.

### D-05: delete() method in Company/Employee

`disable()` → `DISABLED`. Novo `delete()` → `DELETED`. Guards em `activate()`/`inactivate()` verificam tanto `DISABLED` quanto `DELETED`, lançando exceção em ambos os casos.

## Risks / Trade-offs

- **Email VO quebra callers de `getEmail()` como String** → código fora do escopo deste change precisa migrar para `getEmail().getEmail()`. Documentado na proposal.
- **Modificação de changeset existente** → Aceitável pois projeto está em desenvolvimento e sem deploy em produção. Se já houver instância, criar novo changeset.

## Migration Plan

1. Aplicar mudanças nas entidades Java
2. Liquibase executa automaticamente na próxima inicialização da aplicação
3. Rollback: `dropColumn RESPONSIBLE_PERSON` em `scos_company_contact.yml` já está no bloco `rollback`
