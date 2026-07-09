## Context

O módulo `scos-organization` segue arquitetura em camadas com sabor DDD: `Delegate` (API) → `UseCaseBean` (orquestração) → `DomainServiceBean` (regras) → `Repository`/`Aggregate`. Hoje o `@Transactional` está espalhado sem convenção: 6 de 15 domain `*ServiceBean` anotados, 0 de ~71 `*UseCaseBean`. A ideia base é `etc/doc/ideia/20260709_padronizacao-transactional-camadas.md`.

Consequência prática: operações que fazem mais de um write (o primeiro consumidor será Company create — `SCOS_COMPANY` + `SCOS_COMPANY_STATUS_HISTORY`) não têm garantia de atomicidade se cada write viver em método de service separado com sua própria transação.

Restrição aceita explicitamente: a estrutura escolhida prioriza a **estrutura DDD** (application service como fronteira de transação), mas nem toda regra DDD ortodoxa será seguida por esta base de código.

## Goals / Non-Goals

**Goals:**
- Uma convenção única, documentada e aplicada, de posicionamento e configuração de `@Transactional`.
- Fronteira da transação no UseCase Bean (opção B), anotação **na classe**.
- Domain services participam da mesma transação via propagation `REQUIRED`.
- Atomicidade comprovada para operações multi-write.
- Regras explícitas de `readOnly`, `rollbackFor`, propagation e self-invocation.

**Non-Goals:**
- Implementar qualquer CRUD novo (Company etc. são changes próprias, apenas consomem esta convenção).
- Tunar isolation level / timeout por operação (mantém default).
- Corrigir a anomalia `ProfileRepository` (`@Transactional` a nível de repositório).

## Decisions

**D1 — Fronteira da transação no UseCase Bean (opção B), `@Transactional` na classe.**
Cada `*UseCaseBean` expõe uma única operação (`execute`), então anotar a classe é equivalente a anotar o método e reduz ruído. Write → `@Transactional(rollbackFor = ScosException.class)`; read → `@Transactional(readOnly = true)`.
- *Alternativa descartada*: fronteira apenas no domain service. Rejeitada porque exigiria o service compor todos os writes num único método para garantir atomicidade multi-service; a opção B mantém o UseCase como boundary natural e o service focado em regra.

**D2 — Domain Service participa via propagation `REQUIRED` (default), anotação por método.**
O `DomainServiceBean` é multi-método com read e write misturados, então a anotação fica por método (não na classe). Com `REQUIRED`, ao ser chamado dentro da transação já aberta pelo UseCase, o service **junta** (não abre nova) — rollback do boundary desfaz tudo.
- *Alternativa descartada*: `REQUIRES_NEW`. Rejeitada porque criaria transação independente, quebrando a atomicidade que a opção B garante.

**D3 — `rollbackFor = ScosException.class` mantido como documentação.**
`ScosException extends RuntimeException`, então o Spring já faz rollback por default — o atributo é redundante em runtime. Mantido porque documenta, no ponto de uso, qual exception dispara o rollback. Legibilidade > redundância.

**D4 — `readOnly = true` nas leituras (UseCase na classe + domain no método).**
Habilita `FlushMode.MANUAL` do Hibernate (sem dirty-check) e sinaliza intenção. O boundary de leitura precisa ser `readOnly` no UseCase porque `readOnly` **não rebaixa** transação já aberta: se o boundary for read-write, o `readOnly` do método interno é ignorado.

**D5 — Helper privado NÃO anota; método público promovido mantém anotação.**
`@Transactional` em método chamado via `this.` dentro do mesmo bean é no-op (bypassa o proxy CGLIB/JDK). Portanto helper privado não recebe anotação. Já `findAddressTypeById`, promovido a `public`/interface para uso service↔service, **mantém** a anotação — vale quando chamado por outro bean (passa pelo proxy).

**D6 — Convenção documentada na skill `scos-conventions`.**
Fonte única da verdade, aplicada por Claude em geração/review de novo código; a ideia serve como registro.

## Risks / Trade-offs

- **readOnly não rebaixa tx já aberta** → garantir que reads tenham o boundary do UseCase também `readOnly = true`, não só o domain service.
- **Rollback marking propaga** (inner lança `ScosException` → tx marcada rollback-only; UseCase não commita mesmo capturando) → comportamento correto; documentar para não surpreender quem tenta "engolir" a exception.
- **Self-invocation no-op** → nunca anotar helper privado esperando efeito interno; regra explícita na convenção.
- **Entidade JPA fora da tx** (retornar entidade gerenciada do service ao UseCase) → risco de detached/lazy; UseCase trabalha com `Output`/DTO.
- **Escopo grande (~86 arquivos)** → aplicar por lote/agregado com revisão, para evitar regressão silenciosa; teste de integração multi-write como rede de segurança.
- **Redundância do `rollbackFor`** → aceita conscientemente (D3); pode confundir quem não conhece a decisão → registrar na skill.

## Migration Plan

1. Documentar a convenção na skill `scos-conventions` (fonte da verdade).
2. Sweep domain `*ServiceBean` sem anotação (~9), aplicando o padrão por método.
3. Revisar os 6 domain `*ServiceBean` já anotados: remover anotação de helper privado; validar que promoções a público são service↔service.
4. Sweep ~71 `*UseCaseBean`: `@Transactional` na classe (write/read).
5. Teste de integração (Testcontainers) do cenário multi-write com rollback total.
6. Rollback da migração: reverter por commit/lote; mudança é puramente de camada de transação, sem impacto de schema.

## Open Questions

- Nenhuma bloqueante. (Anomalia `ProfileRepository` fica para change separada, conforme escopo.)
