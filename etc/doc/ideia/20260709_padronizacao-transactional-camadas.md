# Padronização de @Transactional entre Camadas (UseCase boundary + Domain participa)

**Data**: 2026-07-09  
**Status**: 🔄 Em Análise  
**Tipo**: 🔧 Refatoração

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

> **Regra**: Uma ideia = uma funcionalidade. Features independentes → arquivos separados.

- **Nome da funcionalidade**: `padronizacao-transactional-camadas`
- **Resumo em uma frase**: Define e aplica uma convenção única de posicionamento e configuração de `@Transactional` — fronteira de transação no UseCase Bean (application boundary) e domain services participando via propagation `REQUIRED` — em todos os beans de UseCase e domain service do módulo.

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade (convenção transacional + sweep de aplicação)
- [x] Não mistura features independentes (não implementa nenhum CRUD; só o cross-cutting transacional)
- [x] O nome é específico

**Fora deste arquivo (SRP — cada um vira sua própria ideia):**
- CRUD de Company e demais features de domínio — usam esta convenção, mas são ideias próprias (ex.: `20260707_cadastro-atualizacao-empresa-company-core.md`).
- Anomalia `ProfileRepository` com `@Transactional` no nível de repositório — investigar/remover em ideia separada.

---

## 1️⃣ Visão

### Problema
`@Transactional` está sendo adicionado de forma **inconsistente** ao módulo:
- Apenas 6 dos 15 domain `*ServiceBean` têm a anotação (AddressType, Cnae, ContactType, LegalNature, Resource, ScosSystem); ~9 não têm.
- **Nenhum** dos ~71 `*UseCaseBean` tem a anotação.
- Sem convenção escrita, cada agregado corre risco de adotar posicionamento/parametrização diferente (`readOnly`, `rollbackFor`, self-invocation), gerando bugs sutis: escritas multi-write não-atômicas, `LazyInitializationException`, `@Transactional` no-op por self-invocation.

Sem fronteira única, operações com **mais de um write** (ex.: Company create = `SCOS_COMPANY` + `SCOS_COMPANY_STATUS_HISTORY`) podem commitar parcialmente e deixar dados órfãos.

### Objetivo
Uma convenção única e documentada, aplicada em todo o módulo:
- Fronteira da transação no **UseCase Bean** (opção B — application boundary).
- Domain services anotados com `@Transactional` participam da tx do UseCase via propagation `REQUIRED` (default).
- Regras claras de `readOnly`, `rollbackFor` e self-invocation.

**Critério de sucesso**: todos os UseCase Beans e domain Service Beans seguem a convenção; operações multi-write são atômicas comprovadas por teste de integração (rollback total em falha do 2º write).

### Fora de Escopo
- Implementar qualquer CRUD novo (Company etc.).
- Tunar isolation level / timeouts por operação (default por ora).
- Corrigir a anomalia `ProfileRepository`.

---

## 2️⃣ Requisitos

### Funcionais
- [ ] **RF-01**: Todo `*UseCaseBean` de escrita anota a **classe** com `@Transactional(rollbackFor = ScosException.class)` (nível de tipo — cada UseCase Bean expõe 1 operação).
- [ ] **RF-02**: Todo `*UseCaseBean` de leitura anota a **classe** com `@Transactional(readOnly = true)`.
- [ ] **RF-03**: Todo domain `*ServiceBean` mantém `@Transactional` por método (write: `rollbackFor = ScosException.class`; read: `readOnly = true`), participando da tx do UseCase via `REQUIRED`.
- [ ] **RF-04**: Métodos helper privados de service **não** recebem `@Transactional` (self-invocation via `this.` não passa pelo proxy → anotação seria no-op).
- [ ] **RF-05**: Métodos de service promovidos a `public`/interface (ex.: `findAddressTypeById`) são justificados **apenas** por uso service↔service (outro bean); nunca para consumo pelo UseCase (UseCase recebe `Output`/DTO, não entidade JPA gerenciada).

### Não-Funcionais
- [ ] **RNF-01**: Nenhuma regressão de comportamento transacional — cobertura de integração para o cenário de rollback multi-write.
- [ ] **RNF-02**: `readOnly = true` nas leituras habilita `FlushMode.MANUAL` do Hibernate (sem dirty-check), sem custo de escrita.

---

## 3️⃣ Arquitetura

### Componentes Afetados
```
scos-organization-usecase
└── application/usecase/**/*UseCaseBean: modificação (adicionar @Transactional) — ~71 beans

scos-organization-domain
└── domain/**/*ServiceBean: modificação (padronizar @Transactional) — 15 beans
    ├── COM anotação (revisar): AddressType, Cnae, ContactType, LegalNature, Resource, ScosSystem
    └── SEM anotação (adicionar): AuthorityResponse, Configuration, Department,
        EmployeePositionQuery, Position, ReasonActivate, ReasonDisable,
        ReasonEnable, ReasonInactivate
```

### Fluxo Principal (write multi-tabela)
```
Delegate → @Transactional(rollbackFor=ScosException) UseCaseBean   ← anotação NA CLASSE, ABRE tx (boundary)
              .execute()
              │
              ▼
        DomainServiceBean.create()  @Transactional  → propagation REQUIRED → JOIN mesma tx
              │
              ▼  merge(entidadeA) + merge(entidadeB)  → all-or-nothing
```

### Regra central — posicionamento por camada

| Camada | Anota? | Write | Read |
|---|---|---|---|
| **UseCase Bean** (boundary) | ✅ **na classe** (nível de tipo) | `@Transactional(rollbackFor = ScosException.class)` | `@Transactional(readOnly = true)` |
| **Domain Service Bean** (participa) | ✅ por método público/interface (bean multi-método, read+write misto) | `@Transactional(rollbackFor = ScosException.class)` | `@Transactional(readOnly = true)` |
| **Helper privado de service** | ❌ | — (self-invocation = no-op) | — |
| **Entity / Value Object** | ❌ | guardas de invariante, sem tx | — |

### Decisões Técnicas
| Decisão | Escolha | Alternativa Descartada | Motivo |
|---------|---------|------------------------|--------|
| Fronteira da transação | **UseCase Bean** (opção B) | Só no domain service | Foco em estrutura DDD (application service = tx boundary); garante atomicidade de operações multi-service sem o UseCase precisar compor writes num único método de service. Aceito que nem toda regra DDD será seguida pela estrutura atual. |
| Propagation do domain service | `REQUIRED` (default) | `REQUIRES_NEW` | Deve **participar** da tx do UseCase, não abrir nova; rollback do boundary desfaz tudo. |
| `rollbackFor = ScosException.class` | **Manter** | Remover (redundante — `ScosException extends RuntimeException`, rollback já é default) | Serve como **documentação** de qual exception dispara rollback naquele método; ganho de legibilidade > custo de redundância. |
| `readOnly = true` nas leituras | Manter/adicionar | Omitir | `FlushMode.MANUAL`, sem dirty-check; sinaliza intenção. |
| Helper `find...ById` público | Permitido só p/ service↔service | Consumo pelo UseCase | UseCase não deve manusear entidade JPA gerenciada (risco detached/lazy). |

### Banco de Dados
- **Impacto**: ❌ Não (mudança puramente de camada de transação; nenhuma tabela/migration).

---

## 4️⃣ Implementação

### Arquivos

**Novos**:
- Nenhum arquivo de código. (Opcional) `docs`/skill de convenção transacional.

**Modificados**:
- `scos-organization-usecase/.../**/*UseCaseBean.java` — adicionar `@Transactional` no método público (write/read conforme tabela). ~71 beans.
- `scos-organization-domain/.../**/*ServiceBean.java` — padronizar `@Transactional`; garantir helpers privados sem anotação. 15 beans.

### Tarefas
- [ ] **T-01**: Escrever a convenção (esta tabela) na skill `scos-conventions` — fonte única da verdade — e referenciar esta ideia. (Decidido: skill + ideia.)
- [ ] **T-02**: Sweep nos domain `*ServiceBean` sem anotação (~9) aplicando o padrão.
- [ ] **T-03**: Revisar os 6 domain `*ServiceBean` que já têm anotação — remover `@Transactional` de helper privado; validar promoções a público são service↔service.
- [ ] **T-04**: Sweep nos ~71 `*UseCaseBean` adicionando `@Transactional` (write/read).
- [ ] **T-05**: Teste de integração (Testcontainers) do cenário multi-write: forçar falha no 2º write e assertar rollback total (nenhuma linha persistida).

### Riscos e Edge Cases
1. **readOnly não rebaixa**: se o UseCase boundary for read-write e chamar um método de service `readOnly = true`, o `readOnly` interno é ignorado (não dá pra rebaixar tx já aberta). Reads devem ter o boundary do UseCase também `readOnly = true`.
2. **rollback marking propaga**: inner service que lança `ScosException` marca a tx como rollback-only; o UseCase não consegue commitar mesmo se capturar a exception. Comportamento correto — documentar para evitar surpresa.
3. **Self-invocation**: `@Transactional` em método chamado via `this.` dentro do mesmo bean é no-op (bypassa o proxy). Nunca anotar helper privado esperando efeito interno.
4. **Entidade fora da tx**: retornar entidade JPA gerenciada do service para o UseCase pode causar `LazyInitializationException` ou uso de detached entity. UseCase trabalha com `Output`/DTO.
5. **Escopo grande (~86 arquivos)**: aplicar por lote/agregado, com revisão, para não introduzir regressão silenciosa.

---

## 📎 Referências
- `20260707_cadastro-atualizacao-empresa-company-core.md` (primeiro consumidor: Company create multi-write / RF-05)
- Padrão de referência atual: `AddressTypeServiceBean` (diff em andamento), `ContactTypeServiceBean`
- Skill `scos-conventions` (destino da convenção — T-01)
- Anomalia a separar: `ProfileRepository` com `@Transactional` a nível de repositório

---
