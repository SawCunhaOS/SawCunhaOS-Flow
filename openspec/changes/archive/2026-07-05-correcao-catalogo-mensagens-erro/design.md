## Context

`ExceptionCodeError` (enum) já contém todos os códigos de negócio necessários — nenhuma alteração de enum é necessária. O problema é só nos arquivos de mensagem (`.properties`/`_en.properties`) e no catálogo de referência (`07-mensagens-erro-pt-en.md`), que já foi corrigido nesta rodada de exploração e serve de fonte de verdade para o patch. Três pontos exigiram decisão técnica antes de codificar (detalhadas abaixo); as demais correções são mecânicas (typo, tradução ausente, PT colado no arquivo EN).

## Goals / Non-Goals

**Goals:**
- Toda entrada `_en.properties` (organization + validation) com tradução real, nenhuma com PT colado
- Texto de mensagem batendo com o verbo/operação real do código (ex.: `disable`, não `delete`, para Department/Position)
- Nenhuma mudança de numeração de código ou de comportamento de negócio

**Non-Goals:**
- Implementar `SCOS_COMPANY_004/005/006` (regras de negócio ainda sem gatilho Java)
- Editar o enum de validação genérica (`SCOS_VALIDATION_*`), que vive em `scos-foundation-utils` (dependência externa)
- Corrigir `ScosOrganizationPermission.java` (nomenclatura de permissões — assunto separado)

## Decisions

1. **`SCOS_CONFIGURATION_002` mantém o texto atual do código** ("configuração não cadastrada"), não a redação alternativa que uma versão anterior do catálogo propunha ("valor incompatível com tipo declarado"). Motivo: `ConfigurationServiceBean.getOrganizationConfiguration` (`ConfigurationServiceBean.java:96`) lança esse código quando não há registro no banco para a key — não existe hoje nenhuma validação de tipo de dado. Alternativa descartada: adotar a redação de tipo incompatível, que exigiria código novo (validação de tipo), fora do escopo deste change.
2. **`SCOS_COMPANY_003`/`006` são reescritos** (PT+EN) trocando "excluir"→"bloquear" e removendo referência a "departamentos" em `003`, mesmo sem nenhuma classe Java lançar esses códigos hoje (confirmado via grep exaustivo). Motivo: o texto antigo é resquício do modelo `DELETED` já removido do domínio; reescrever agora documenta o alvo correto (`block`) para quem for implementar a regra depois. Nota de transparência mantida no catálogo de referência.
3. **`SCOS_VALIDATION_006` (PT) é corrigido assumindo `{1}`=mínimo/`{2}`=máximo**, o mesmo padrão de `SCOS_VALIDATION_008` e do texto EN já existente. Motivo: o binding real de parâmetros fica em `scos-foundation-utils` (dependência externa, fora deste repositório) — não é possível confirmar 100% antes de aplicar. Risco aceito explicitamente pelo usuário.
4. **`SCOS_DEPARTMENT_003`/`SCOS_POSITION_003` (EN) trocam "delete"→"disable"** e ganham o qualificador "active" (ex.: "active positions"). Motivo: "delete" contradiz a regra de domínio de que Department/Position não têm exclusão física/lógica irreversível (só `enable`/`disable`); o qualificador estava presente no PT ("posições ativas"/"empregados ativos") e se perdia na tradução.

## Risks / Trade-offs

- [Risco] `SCOS_VALIDATION_006` — se o binding real de `{1}`/`{2}` no framework externo for diferente do assumido, a mensagem passa a inverter mínimo/máximo → [Mitigação] nenhuma automática disponível neste repo; documentado no catálogo como ponto que exige confirmação futura com o time responsável por `scos-foundation-utils`.
- [Risco] Reescrever `SCOS_COMPANY_003/006` documenta um comportamento (`block`) que ainda não existe no código → [Mitigação] nota de transparência explícita no catálogo (`07-mensagens-erro-pt-en.md`), para não confundir quem for implementar a regra depois.
- [Trade-off] `SCOS_CONFIGURATION_002` do catálogo de referência fica marcado como divergente de uma proposta anterior descartada — aceito porque a proposta anterior descrevia regra de negócio inexistente; manter o texto real evita catálogo prescrevendo comportamento fictício.

## Migration Plan

Não aplicável — mudança é só em arquivos `.properties` (mensagens) e documentação; sem migração de dados, sem endpoint novo, sem deploy especial. Rollback trivial (reverter o commit).

## Open Questions

Nenhuma — todos os pontos de decisão foram resolvidos com o usuário durante a exploração (ver `etc/doc/ideia/20260705_correcao-catalogo-mensagens-erro.md`).
