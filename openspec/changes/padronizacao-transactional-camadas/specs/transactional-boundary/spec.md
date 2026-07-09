## ADDED Requirements

### Requirement: Fronteira de transação no UseCase Bean
Todo `*UseCaseBean` SHALL declarar a fronteira de transação anotando a **classe** com `@Transactional`. Um UseCase Bean de escrita MUST usar `@Transactional(rollbackFor = ScosException.class)`; um de leitura MUST usar `@Transactional(readOnly = true)`.

#### Scenario: UseCase de escrita abre transação
- **WHEN** um `CreateXUseCaseBean`/`UpdateXUseCaseBean` é invocado pelo Delegate
- **THEN** a classe está anotada com `@Transactional(rollbackFor = ScosException.class)`
- **AND** uma única transação read-write é aberta como boundary da operação

#### Scenario: UseCase de leitura abre transação readOnly
- **WHEN** um `FindXUseCaseBean`/`FindAllXUseCaseBean` é invocado
- **THEN** a classe está anotada com `@Transactional(readOnly = true)`

### Requirement: Domain Service participa da transação via propagation REQUIRED
Todo `*ServiceBean` de domínio SHALL anotar `@Transactional` **por método público** (não na classe), usando propagation `REQUIRED` (default). Métodos de escrita MUST usar `rollbackFor = ScosException.class`; métodos de leitura MUST usar `readOnly = true`. Ao ser chamado dentro da transação aberta pelo UseCase, o service MUST juntar-se a ela e NOT abrir uma nova transação.

#### Scenario: Service junta transação do UseCase
- **WHEN** um `*UseCaseBean` anotado invoca um método de `*ServiceBean` anotado com `@Transactional` default
- **THEN** o método do service participa da mesma transação (propagation REQUIRED)
- **AND** nenhuma transação adicional é criada

#### Scenario: Operação multi-write é atômica
- **WHEN** uma operação de UseCase executa mais de um write através de um ou mais services e o segundo write lança `ScosException`
- **THEN** a transação é revertida por completo
- **AND** nenhuma linha do primeiro write permanece persistida

### Requirement: readOnly não rebaixa transação já aberta
Operações de leitura SHALL ter o boundary do UseCase anotado com `readOnly = true`, pois `readOnly` MUST NOT rebaixar uma transação read-write já aberta. Se o boundary for read-write, o `readOnly` de um método interno é ignorado.

#### Scenario: Leitura mantém readOnly de ponta a ponta
- **WHEN** um UseCase de leitura `readOnly = true` chama um método de service `readOnly = true`
- **THEN** a transação inteira é read-only e habilita FlushMode.MANUAL do Hibernate

### Requirement: rollbackFor mantido como documentação
Métodos de escrita SHALL declarar `rollbackFor = ScosException.class` mesmo sendo redundante em runtime (`ScosException extends RuntimeException` já dispara rollback por default). O atributo MUST ser mantido para documentar, no ponto de uso, qual exception dispara o rollback.

#### Scenario: rollback em ScosException
- **WHEN** um método de escrita anotado lança `ScosException`
- **THEN** a transação é revertida
- **AND** a anotação `rollbackFor = ScosException.class` está presente explicitamente

### Requirement: Self-invocation não recebe anotação
Métodos helper **privados** de um bean SHALL NOT ser anotados com `@Transactional`, pois a chamada via `this.` bypassa o proxy Spring e a anotação seria no-op. Um método promovido a `public`/interface para uso service↔service MAY manter a anotação, pois passa pelo proxy quando chamado por outro bean.

#### Scenario: Helper privado sem anotação
- **WHEN** um método privado é chamado internamente via `this.` dentro do mesmo bean
- **THEN** o método não está anotado com `@Transactional`

#### Scenario: Método público service↔service mantém anotação
- **WHEN** um método público como `findAddressTypeById` é chamado por outro bean de service
- **THEN** o método está anotado com `@Transactional(readOnly = true)` e a anotação é efetiva (passa pelo proxy)

### Requirement: Convenção documentada como fonte da verdade
A convenção transacional SHALL ser registrada na skill `scos-conventions`, servindo de fonte única aplicada na geração e revisão de código.

#### Scenario: Convenção aplicável a novo código
- **WHEN** um novo `*UseCaseBean` ou `*ServiceBean` é criado
- **THEN** a skill `scos-conventions` descreve o posicionamento e a configuração de `@Transactional` a seguir
