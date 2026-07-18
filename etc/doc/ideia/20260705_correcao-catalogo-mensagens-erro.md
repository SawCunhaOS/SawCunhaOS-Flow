# Correção do Catálogo de Mensagens de Erro (PT/EN)

**Data**: 2026-07-05
**Status**: ✅ Aprovado
**Tipo**: 🐛 Bug Fix

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

> **Regra**: Uma ideia = uma funcionalidade. Features independentes → arquivos separados.

- **Nome da funcionalidade**: `correcao-catalogo-mensagens-erro`
- **Resumo em uma frase**: Sincronizar o catálogo bilíngue de mensagens de erro (`etc/doc/usecase/07-mensagens-erro-pt-en.md`) com o estado real do código (`ExceptionCodeError.java` + `scos_message_organization(.properties/_en.properties)` + `scos_message_validation(.properties/_en.properties)`), sem alterar numeração de códigos.

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade (catálogo de mensagens de erro)
- [x] Não mistura com a feature `crud-cargo-position` (idea separada, já em andamento)
- [x] Nome específico

---

## 1️⃣ Visão

### Problema
O documento `07-mensagens-erro-pt-en.md` ficou defasado em relação ao código-fonte real:

1. `scos_message_organization_en.properties` está **incompleto** — faltam traduções para `SCOS_DEPARTMENT_004/005/006`, `SCOS_POSITION_004/005`.
2. Várias entradas do arquivo `_en.properties` contêm **texto em português copiado por engano** (não traduzido): `SCOS_AUTHORITY_001`, `SCOS_LOGIN_010/011`.
3. `scos_message_validation_en.properties` tem o mesmo problema em `SCOS_VALIDATION_010/011/012` (PT colado no arquivo EN), além de duas frases incompletas em `SCOS_VALIDATION_002/004` ("...is below/above the specified.").
4. `scos_message_validation.properties` (PT) tem erro de digitação em `SCOS_VALIDATION_009` ("O CEP Informado e invalido.") e inversão de parâmetros `{1}`/`{2}` em `SCOS_VALIDATION_006` frente ao padrão usado em `SCOS_VALIDATION_008`.
5. O enum `ExceptionCodeError.java` **já contém** todos os códigos de negócio citados no doc07 (`DEPARTMENT_004/005/006`, `POSITION_004/005`, `CONFIGURATION_001/002`, `LOGIN_013`, com Javadoc) — commit `20338f9` já aplicou isso. **Nenhuma alteração de enum é necessária nesta rodada.**
6. `SCOS_COMPANY_003`/`006` — texto atual usa "excluir" e cita "departamentos"; confirmado (ver `00-indice-central.md` seção 3.1) que **nenhuma classe Java lança esses códigos hoje**. Decisão: reescrever para "bloquear", sem "departamentos", com nota de transparência.
7. `SCOS_DEPARTMENT_003`/`SCOS_POSITION_003` (EN) — bug real de tradução, já presente no `scos_message_organization_en.properties` de produção: PT usa "inativar" (reversível, endpoint `/disable`), EN usa **"delete"** (exclusão permanente), contradizendo a regra de domínio de que Department/Position não têm exclusão física/lógica irreversível. EN também perdia o qualificador "ativas"/"ativos" presente no PT.

### Objetivo
- `scos_message_organization_en.properties` e `scos_message_validation_en.properties` sem nenhuma entrada com texto em português colado por engano.
- Traduções reais para os códigos que hoje só existem em PT.
- Typos e frases incompletas corrigidos.
- `07-mensagens-erro-pt-en.md` (tabela EN + notas) sincronizado com o texto final.
- Números de código (`SCOS_XXX_NNN`) inalterados. Enum inalterado (já completo).

### Fora de Escopo
- Implementar as regras de negócio ainda não codificadas (`SCOS_COMPANY_004/005/006`) — código de validação nova, não catálogo de mensagem. Pertence a idea própria futura.
- Editar o enum de mensagens de validação genérica (`SCOS_VALIDATION_*`) — esse código vive em `scos-foundation-utils` (dependência externa), fora deste repositório.
- Corrigir `ScosOrganizationPermission.java` (nomenclatura antiga de permissões, achado 3.6 do índice central) — assunto de permissões, não de mensagens de erro.
- Restaurar as 2 linhas removidas da tabela "Resumo" (seção 3 do doc07) — decisão explícita: manter como está.

---

## 2️⃣ Requisitos

### Funcionais
- [ ] **RF-01**: Completar `scos_message_organization_en.properties` com traduções reais (não PT colado) para `DEPARTMENT_004/005/006`, `POSITION_004/005`, `AUTHORITY_001`, `LOGIN_010/011`.
- [ ] **RF-02**: Completar `scos_message_validation_en.properties` com traduções reais para `VALIDATION_010/011/012`; corrigir frases incompletas em `VALIDATION_002/004`.
- [ ] **RF-03**: Corrigir typo em `scos_message_validation.properties` (`VALIDATION_009`).
- [ ] **RF-04**: Corrigir ordem de parâmetros `{1}`(mín)/`{2}`(máx) em `SCOS_VALIDATION_006` (PT), alinhando ao padrão do `VALIDATION_008` e ao EN.
- [ ] **RF-05**: Reescrever `SCOS_COMPANY_003`/`006` (PT+EN) trocando "excluir"→"bloquear" e removendo referência a "departamentos" em `003`; manter nota de transparência dizendo que não há gatilho Java ainda.
- [ ] **RF-06**: Manter `SCOS_CONFIGURATION_001/002` com o texto atual (bate com o código real) — **não** adotar a redação do doc07 para `002` ("tipo incompatível"), pois descreve regra de negócio inexistente.
- [ ] **RF-07**: Atualizar `07-mensagens-erro-pt-en.md` para refletir o texto final acordado (PT + EN), sem alterar a tabela de resumo (seção 3).
- [ ] **RF-08**: Corrigir `SCOS_DEPARTMENT_003`/`SCOS_POSITION_003` (EN) em `scos_message_organization_en.properties` — troca de verbo "delete"→"disable" + inclusão do qualificador "active", alinhado à regra de domínio (sem exclusão física/lógica).

### Não-Funcionais
- [ ] **RNF-01**: Nenhuma mudança de numeração de código — só texto de mensagem.
- [ ] **RNF-02**: Nenhum texto de catálogo deve implicar comportamento não implementado sem nota explícita de transparência (convenção já usada no doc07/00-indice-central).

---

## 3️⃣ Arquitetura

### Componentes Afetados
```
flow-organization-shared
├── resources/scos_message_organization.properties     : modificação (COMPANY_003/006 wording)
├── resources/scos_message_organization_en.properties  : modificação (completar + corrigir)
├── resources/scos_message_validation.properties       : modificação (typo 009 + fix 006)
└── resources/scos_message_validation_en.properties     : modificação (completar + corrigir)

exception/ExceptionCodeError.java                        : SEM alteração (já completo)

etc/doc/usecase/07-mensagens-erro-pt-en.md               : modificação (sincronizar PT/EN + notas)
```

### Decisões Técnicas (já resolvidas com o usuário)
| Decisão | Escolha | Motivo |
|---------|---------|--------|
| `SCOS_CONFIGURATION_002` | Manter texto atual ("não cadastrada") | Bate com `ConfigurationServiceBean.java:96`; texto do doc07 descreve cenário que o código não valida |
| `SCOS_COMPANY_003/006` | Reescrever já (bloquear, sem "departamentos") | Documenta o alvo correto; sem gatilho Java hoje, então reescrever é seguro — nota de transparência obrigatória |
| `SCOS_VALIDATION_006` | Corrigir `{1}`/`{2}` assumindo padrão do `008` | Risco aceito pelo usuário; bind real fica fora do repo (`scos-foundation-utils`) |
| Tabela "Resumo" (seção 3, doc07) | Não restaurar linhas removidas | Decisão explícita do usuário — manter versão atual reduzida |

### Banco de Dados
- **Impacto**: ❌ Não

---

## 4️⃣ Implementação

### Arquivos

**Modificados** (nenhum novo arquivo):
- `etc/doc/usecase/07-mensagens-erro-pt-en.md`
- `flow-organization-shared/src/main/resources/scos_message_organization.properties`
- `flow-organization-shared/src/main/resources/scos_message_organization_en.properties`
- `flow-organization-shared/src/main/resources/scos_message_validation.properties`
- `flow-organization-shared/src/main/resources/scos_message_validation_en.properties`

### Tarefas
- [x] **T-01**: Atualizar `07-mensagens-erro-pt-en.md` (tabela EN + notas) com o texto final acordado — feito (seção 2.4 corrigida; resto já batia)
- [ ] **T-02**: Completar `scos_message_organization_en.properties` (DEPARTMENT_004-006, POSITION_004/005, AUTHORITY_001, LOGIN_010/011, CONFIGURATION_001/002, COMPANY_003/006, USER_001-004)
- [ ] **T-03**: Completar `scos_message_validation_en.properties` (VALIDATION_010/011/012) + corrigir 002/004
- [ ] **T-04**: Corrigir `scos_message_validation.properties` (typo 009, ordem de parâmetros 006)
- [ ] **T-05**: Reescrever `SCOS_COMPANY_003/006` e polir `SCOS_USER_001-004` em PT (`scos_message_organization.properties`)
- [ ] **T-06**: Build/teste local para garantir que nenhuma properties key ficou órfã ou duplicada

### Mapeamento Detalhado de Mudanças (old → new)

Base: conteúdo atual dos arquivos vs. texto final do `07-mensagens-erro-pt-en.md` (já atualizado). Chaves não listadas aqui não mudam.

**`scos_message_organization.properties` (PT)**
| Chave | De | Para |
|---|---|---|
| `SCOS_COMPANY_003` | `Não foi possível excluir a empresa, pois ela possui vínculos com colaboradores ou departamentos.` | `Não é possível bloquear a empresa, pois ela possui colaboradores vinculados.` |
| `SCOS_COMPANY_006` | `Não é permitido excluir a única empresa ativa do sistema.` | `Não é permitido bloquear a única empresa ativa do sistema.` |
| `SCOS_USER_001` | `Ocorreu erro ao tentar cadastrar o Usuário no Keycloak` | `Ocorreu um erro ao tentar cadastrar o usuário no Keycloak.` |
| `SCOS_USER_002` | `Ocorreu erro ao tentar atualizar o Usuário no Keycloak` | `Ocorreu um erro ao tentar atualizar o usuário no Keycloak.` |
| `SCOS_USER_003` | `Ocorreu erro ao tentar apagar o Usuário no Keycloak` | `Ocorreu um erro ao tentar excluir o usuário no Keycloak.` |
| `SCOS_USER_004` | `Erro desconhecido ao processar usuário` | `Erro desconhecido ao processar o usuário.` |

`SCOS_CONFIGURATION_001/002` (PT): **sem mudança** — já batem com o texto final do doc07.

**`scos_message_organization_en.properties` (EN)**
| Chave | De | Para |
|---|---|---|
| `SCOS_DEPARTMENT_004` | *(ausente)* | `The informed department is already active.` |
| `SCOS_DEPARTMENT_005` | *(ausente)* | `The informed department is already inactive.` |
| `SCOS_DEPARTMENT_006` | *(ausente)* | `It is not possible to associate the position with an inactive department.` |
| `SCOS_POSITION_004` | *(ausente)* | `The informed position is already active.` |
| `SCOS_POSITION_005` | *(ausente)* | `The informed position is already inactive.` |
| `SCOS_COMPANY_003` | `It was not possible to delete the company because it has links with employees or departments.` | `It is not possible to block the company because it has linked employees.` |
| `SCOS_COMPANY_006` | `It is not allowed to delete the only active company in the system.` | `It is not allowed to block the only active company in the system.` |
| `SCOS_CONFIGURATION_001` | `A Key informada não existe no sistema.` *(PT colado)* | `The informed key does not exist in the system.` |
| `SCOS_CONFIGURATION_002` | `A Configuração informada não está cadastrada no sistema.` *(PT colado)* | `The informed configuration is not registered in the system.` |
| `SCOS_USER_001` | `An error occurred while trying to register the User in Keycloak` | `An error occurred while trying to register the user in Keycloak.` |
| `SCOS_USER_002` | `An error occurred while trying to update the User in Keycloak` | `An error occurred while trying to update the user in Keycloak.` |
| `SCOS_USER_003` | `An error occurred while trying to delete the User in Keycloak` | `An error occurred while trying to delete the user in Keycloak.` |
| `SCOS_USER_004` | `Unknown error while processing user` | `Unknown error while processing the user.` |
| `SCOS_AUTHORITY_001` | `O Login informado não existe no sistema.` *(PT colado)* | `The informed Login does not exist in the system.` |
| `SCOS_LOGIN_010` | `O Login informado está inativo no sistema.` *(PT colado)* | `The informed Login is inactive in the system.` |
| `SCOS_LOGIN_011` | `O Login informado está bloqueado no sistema.` *(PT colado)* | `The informed Login is blocked in the system.` |

| `SCOS_DEPARTMENT_003` | `It was not possible to delete the department because it has links with positions.` | `It was not possible to disable the department because it has active positions linked to it.` |
| `SCOS_POSITION_003` | `It was not possible to delete the position because it has links with employees.` | `It was not possible to disable the position because it has active employees linked to it.` |

`SCOS_LOGIN_013`, `SCOS_EMPLOYEE_001`, `SCOS_DEPARTMENT_001/002`, `SCOS_POSITION_001/002`, `SCOS_COMPANY_001/002/004/005/007` (EN): **sem mudança** — já corretos.

**`scos_message_organization.properties` (PT) — nota adicional**: `SCOS_DEPARTMENT_003` já está correto na fonte real (`posições ativas`, plural); só o doc07 tinha o typo de concordância, já corrigido. Nenhuma mudança de código PT aqui.

**`scos_message_validation.properties` (PT)**
| Chave | De | Para |
|---|---|---|
| `SCOS_VALIDATION_006` | `...deve ser maior ou igual a {2} e menor ou igual a {1}.` | `...deve ser maior ou igual a {1} e menor ou igual a {2}.` |
| `SCOS_VALIDATION_009` | `O CEP Informado e invalido.` | `O CEP informado é inválido.` |

**`scos_message_validation_en.properties` (EN)**
| Chave | De | Para |
|---|---|---|
| `SCOS_VALIDATION_002` | `...is below the specified.` | `...must be greater than or equal to {1}.` |
| `SCOS_VALIDATION_004` | `...is above the specified.` | `...must be less than or equal to {1}.` |
| `SCOS_VALIDATION_010` | `O CNPJ informado é inválido.` *(PT colado)* | `The provided CNPJ is invalid.` |
| `SCOS_VALIDATION_011` | `O CPF informado é inválido.` *(PT colado)* | `The provided CPF is invalid.` |
| `SCOS_VALIDATION_012` | `O documento informado é inválido.` *(PT colado)* | `The provided document is invalid.` |

`ExceptionCodeError.java`: **sem mudança** — todos os códigos já existem no enum.

### Riscos e Edge Cases
1. `SCOS_VALIDATION_006` — mudar ordem dos parâmetros sem confirmação 100% do binding real (fora deste repo) pode inverter o sentido da mensagem caso o binding real seja diferente do assumido. Risco aceito pelo usuário.
2. Reescrever `SCOS_COMPANY_003/006` documenta comportamento (`block`) que ainda não existe no código — nota de transparência é obrigatória para não confundir quem for implementar depois.
3. `SCOS_CONFIGURATION_002` do doc07 ficará marcado como divergente do código real — deixar isso explícito no doc07 para não gerar confusão futura.

---

## 📎 Referências
- `etc/doc/usecase/00-indice-central.md` (seções 2.2, 3.1)
- `etc/doc/usecase/02-departamento-cargo.md`
- Commit `20338f9` (já adicionou boa parte dos códigos ao enum + properties PT)
