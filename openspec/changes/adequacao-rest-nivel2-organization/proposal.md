## Why

As specs OpenAPI do módulo Organization violam convenções REST estabelecidas e o RFC 9110 (HTTP Semantics), criando inconsistências entre recursos, endpoints inacessíveis via proxies HTTP (DELETE com body) e URIs com singular/plural misturados que dificultam o consumo da API por clientes. A correção é necessária antes que novos consumidores sejam adicionados, para evitar breaking changes em cadeia no futuro.

## What Changes

- **BREAKING** URIs de coleções renomeadas para plural: `company→companies`, `employee→employees`, `profile→profiles`, sub-recursos `contact→contacts`, `address→addresses`, `login→logins`
- **BREAKING** `GET /v1/employee/login/info` movido para `GET /v1/logins/me`
- **BREAKING** `DELETE /v1/profile/{id}/features` removido — coberto pelo `PUT /v1/profiles/{id}/features` existente (substituição completa da lista)
- **BREAKING** `LoginStatus.ENABLE` renomeado para `ENABLED` — valor incorreto (verbo vs. estado)
- Endpoints `PUT /{id}/enable` e `PUT /{id}/disable` adicionados a Department e Position (consistência com Company e Employee)
- Schemas de Profile e Login migrados de sufixo `*DTO` para `*Request`/`*Response`
- RFC 9205 removido das referências — não aplicável a APIs REST convencionais; somente RFC 9110 é referenciado

## Capabilities

### New Capabilities

- `api-rest-nomenclature`: Requisitos de nomenclatura das URIs (plural), sufixo de schemas (*Request/*Response), valores de enum como estados (não verbos), e ausência de colisão de paths
- `api-rest-level2-compliance`: Requisitos de conformidade REST Level 2 e RFC 9110 — transição de estado via PUT sub-resource sem body, DELETE sem requestBody, cobertura uniforme de toggle de status entre todos os recursos

### Modified Capabilities

## Impact

- `etc/api/organization/ScosOrganization_Company.yml` — renomeação de URIs
- `etc/api/organization/ScosOrganization_Employee.yml` — renomeação de URIs + schemas
- `etc/api/organization/ScosOrganization_Login.yml` — renomeação de URIs, remoção de DELETE /features, migração *DTO→*Request/Response, correção LoginStatus, novo `/v1/logins/me`
- `etc/api/organization/ScosOrganization_Department-Position.yml` — adição de endpoints enable/disable
- Implementação Java: todos os Controllers e mapeamentos de URI dos recursos afetados
- Clientes da API: qualquer consumidor das URIs atuais — mudanças são breaking
