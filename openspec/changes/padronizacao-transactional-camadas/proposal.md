## Why

`@Transactional` está sendo adicionado ao módulo de forma inconsistente: apenas 6 dos 15 domain `*ServiceBean` têm a anotação e **nenhum** dos ~71 `*UseCaseBean` a possui. Sem uma convenção única, operações com mais de um write (ex.: Company create = `SCOS_COMPANY` + `SCOS_COMPANY_STATUS_HISTORY`) podem commitar parcialmente e deixar dados órfãos, além de haver risco de `@Transactional` no-op por self-invocation e de `LazyInitializationException`.

## What Changes

- Definir a fronteira da transação no **UseCase Bean** (application boundary — opção B), com `@Transactional` **na classe** (nível de tipo, pois cada UseCase Bean expõe uma única operação):
  - Escrita: `@Transactional(rollbackFor = ScosException.class)`.
  - Leitura: `@Transactional(readOnly = true)`.
- Domain `*ServiceBean` **participa** da transação do UseCase via propagation `REQUIRED` (default), com anotação **por método** (bean multi-método, read+write misto): write → `rollbackFor = ScosException.class`; read → `readOnly = true`.
- Helper **privado** de service **não** recebe `@Transactional` (self-invocation via `this.` bypassa o proxy → no-op).
- Métodos de service promovidos a `public`/interface (ex.: `findAddressTypeById`) **mantêm** a anotação, justificados apenas por uso service↔service.
- `rollbackFor = ScosException.class` é **mantido** como documentação de qual exception dispara rollback (redundante em runtime, pois `ScosException extends RuntimeException`).
- Documentar a convenção na skill `scos-conventions` como fonte única da verdade.
- Aplicar o sweep: ~71 `*UseCaseBean` + 15 domain `*ServiceBean`.

## Capabilities

### New Capabilities
- `transactional-boundary`: convenção de posicionamento e configuração de `@Transactional` por camada (UseCase boundary + domain participante), regras de `readOnly`/`rollbackFor`/propagation/self-invocation, e o sweep de aplicação no módulo.

### Modified Capabilities
<!-- Nenhuma — mudança cross-cutting de camada de transação; não altera requisitos de specs de domínio existentes. -->

## Impact

- **Código**:
  - `scos-organization-usecase/.../**/*UseCaseBean.java` — adicionar `@Transactional` na classe (~71 beans).
  - `scos-organization-domain/.../**/*ServiceBean.java` — padronizar `@Transactional` por método; garantir helper privado sem anotação (15 beans; 6 já anotados a revisar, ~9 a adicionar).
  - Skill `scos-conventions` — registrar a convenção.
- **Banco de dados**: nenhum (mudança puramente de camada de transação; sem tabelas/migrations).
- **Fora de escopo**: anomalia `ProfileRepository` com `@Transactional` a nível de repositório (ideia/change separada).
- **Testes**: teste de integração (Testcontainers) do cenário multi-write com rollback total.
