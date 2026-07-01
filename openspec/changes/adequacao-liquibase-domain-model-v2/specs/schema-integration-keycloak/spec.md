## REMOVED Requirements

### Requirement: SCOS_INTEGRATION_KEYCLOAK com retry
**Reason**: Mecanismo específico de Keycloak substituído pelo padrão outbox genérico (`schema-outbox`), que cobre qualquer processo de entrega assíncrona ou chamada a API externa, não só Keycloak. Campos específicos (`REALM`, `EMAIL`, `USERNAME`) migram para dentro de `SCOS_OUTBOX_EVENT.PAYLOAD`.
**Migration**: Consumidores desta tabela devem passar a gravar em `SCOS_OUTBOX_EVENT` com `TOPIC` roteado para `BACKEND = 'DIRECT_API'` e `TARGET_SYSTEM = 'KEYCLOAK'`. Não há migração de dados — sistema sem produção, banco recriado do zero.

### Requirement: SCOS_INTEGRATION_KEYCLOAK_LOG imutável
**Reason**: Substituída por `SCOS_OUTBOX_EVENT_LOG`, genérica para qualquer tópico/consumidor, não só Keycloak.
**Migration**: Consumidores devem gravar tentativas em `SCOS_OUTBOX_EVENT_LOG` com `CONSUMER = 'KEYCLOAK'`. Sem migração de dados.

### Requirement: SCOS_INTEGRATION_MESSAGE_INVALID imutável
**Reason**: Substituída por `SCOS_OUTBOX_EVENT_DEAD_LETTER`, que guarda contexto suficiente (tópico, origem, tipo de erro, contagem de tentativas) para reprocessamento manual — a versão antiga só guardava o texto cru.
**Migration**: Consumidores devem gravar payloads não processáveis em `SCOS_OUTBOX_EVENT_DEAD_LETTER`. Sem migração de dados.
