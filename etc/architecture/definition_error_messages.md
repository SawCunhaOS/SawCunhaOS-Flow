# Padrão de códigos de erro (mensagens de erro do domínio)

Este documento define o padrão oficial para a criação e manutenção dos códigos de erro utilizados pelo módulo `scos-organization`.

## Padrão
- Formato: `SCOS_<MÓDULO>_<NNN>`
  - `SCOS_` — prefixo fixo do projeto
  - `<MÓDULO>` — nome da entidade ou funcionalidade em letras maiúsculas (ex.: DEPARTMENT, POSITION, COMPANY, USER)
  - `<NNN>` — sufixo numérico de 3 dígitos, incrementado sequencialmente por módulo (ex.: 001, 002)

Exemplos válidos:
- `SCOS_DEPARTMENT_001`
- `SCOS_POSITION_002`
- `SCOS_COMPANY_004`

Regex de validação sugerida: `^SCOS_[A-Z0-9]+_\d{3}$`

## Diretrizes de uso
- Sempre utilize o `ExceptionCodeError` (enum) ao lançar `ScosException` no domínio.
- Ao adicionar um novo erro para um módulo, inclua o próximo código incremental de 3 dígitos.
- Inclua mensagem legível no `messages.properties` ou equivalente e referencie o código no `ExceptionCodeError`.
- Não utilizar hífens (`-`) — usar underscore (`_`) conforme padrão.

## Compatibilidade / migração
- Códigos antigos baseados em prefixos diferentes (ex.: `SCOS_...`) podem existir — manter *aliases* apenas enquanto necessário para compatibilidade.
- Objetivo: padronizar todos os códigos para `SCOS_` e migrar referências gradualmente.

## Como adicionar um novo código (passo a passo)
1. Escolher o módulo/área (ex.: `DEPARTMENT`).
2. Incrementar o sufixo (ex.: se já existir `_001` e `_002`, use `_003`).
3. Adicionar a constante em `ExceptionCodeError` com o novo código (ex.: `SCOS_DEPARTMENT_003("SCOS_DEPARTMENT_003")`).
4. Adicionar teste cobrindo o lançamento do `ScosException` com o novo `ExceptionCodeError`.
5. Atualizar `messages.properties` com a mensagem human-readable vinculada ao código.

## Exemplo de referência no código
```java
if (!departmentRepository.existsById(id)) {
    throw new ScosException(ExceptionCodeError.SCOS_DEPARTMENT_001);
}
```