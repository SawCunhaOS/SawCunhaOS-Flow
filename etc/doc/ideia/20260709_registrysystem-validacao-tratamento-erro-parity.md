# registrySystem — Paridade de Validação e Tratamento de Erro

**Data**: 2026-07-09  
**Status**: 🔄 Em Análise  
**Tipo**: 🔧 Refatoração

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

> **Regra**: Uma ideia = uma funcionalidade. Features independentes → arquivos separados.

- **Nome da funcionalidade**: `registrysystem-validation-error-parity`
- **Resumo em uma frase**: Aplica ao fluxo de `registrySystem` o mesmo tratamento já feito no `Resource` — persistência via upsert nativo **condicional** (só atualiza quando `version` muda) e propagação do motivo de erro entre módulos (`ScosException` → `Status` gRPC), dando consistência e visibilidade ao registro do sistema.

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade (paridade do registrySystem)
- [x] Não mistura features independentes (isolamento multi-tenant está em arquivo próprio)
- [x] O nome é específico

**Fora deste arquivo (SRP):**
- Isolamento de permissões por sistema → `20260709_isolamento-permissoes-por-sistema-multi-tenant.md`.

---

## 1️⃣ Visão

### Problema
O `Resource` recebeu upsert nativo condicional + `ScosException`/status gRPC, mas o `registrySystem` continua no padrão antigo:
- **`ScosSystemServiceBean.register`** usa `findByCode().orElse(null)` + `merge`/`update` (JPA), com update disparado por comparação de `version` em Java. Não é atômico numa query, e re-registro sem mudança ainda percorre o caminho de JPA.
- **`findByCodeAndSecretKey(...).orElseThrow()`** lança `NoSuchElementException` (não `ScosException`). Hoje o `TokenAuthorizationInterceptor` captura e devolve `UNAUTHENTICATED "Sistema informado nao existe"` — funciona, mas o tratamento **não é consistente** com o padrão `ScosException` + código de erro + mensagem i18n usado no resto do domínio.
- Não há códigos `SCOS_SYSTEM_*` para credenciais inválidas / conflito de registro — o motivo fica em string solta no interceptor.

### Objetivo
- `registrySystem` persiste via **upsert nativo condicional** (só `DO UPDATE` quando `version` difere), colunas de auditoria no SQL, mantendo a semântica de "criado vs atualizado" e a geração de `secretKey` só no insert.
- Erros do fluxo de sistema viram `ScosException` com código próprio (`SCOS_SYSTEM_00x`), resolvidos em `Status` gRPC de negócio com mensagem i18n visível.
- Sucesso = re-registrar o mesmo sistema com a mesma `version` não gera update; credencial inválida retorna erro de negócio visível e consistente.

### Fora de Escopo
- Isolamento de permissões / mudança de UK do resource (arquivo próprio).
- Rotação/expiração de `secretKey`.

---

## 2️⃣ Requisitos

### Funcionais
- [ ] **RF-01**: `ScosSystemServiceBean.register` usa upsert nativo condicional em `SCOS_SYSTEM` (`ON CONFLICT (CODE) DO UPDATE ... WHERE VERSION IS DISTINCT FROM EXCLUDED.VERSION`), auditoria no SQL.
- [ ] **RF-02**: A semântica de retorno "criado/atualizado" (`updateRegistration`) e a geração de `secretKey` **apenas no insert** são preservadas (upsert com `RETURNING`, ou upsert + releitura).
- [ ] **RF-03**: `findByCodeAndSecretKey` deixa de usar `.orElseThrow()` genérico e lança `ScosException(SCOS_SYSTEM_00x)` (credencial inválida) — motivo visível.
- [ ] **RF-04**: Novos códigos `SCOS_SYSTEM_*` no `ExceptionCodeError` (+ mensagens pt/en) para credencial inválida (401) e, se aplicável, conflito de registro.
- [ ] **RF-05**: `GrpcGlobalExceptionHandler` já mapeia `ScosException` → `Status` (feito na change do resource) — reusar para o fluxo de sistema.

### Não-Funcionais
- [ ] **RNF-01**: Consistência com o padrão do resource (mesmo estilo de upsert e de erro).
- [ ] **RNF-02**: Trilha `@Auditable` não dispara no upsert nativo — aceito (metadado de sistema).

---

## 3️⃣ Arquitetura

### Componentes Afetados
```
flow-organization-domain
├── access/system/service/ScosSystemServiceBean : register → upsert; findByCodeAndSecretKey → ScosException
└── access/system/internal/ScosSystemRepository : +upsert nativo condicional

flow-organization-shared
├── exception/ExceptionCodeError                : +SCOS_SYSTEM_00x (credencial inválida etc.)
└── resources/scos_message_organization*.properties : +mensagens

grpc/flow-organization-grpc-boot
└── interceptor/TokenAuthorizationInterceptor   : deixar o ScosException fluir p/ o handler (ou mapear)
```

### Fluxo Principal
```
registrySystem(code, version, ...)
        ▼
ScosSystemService.register → upsert ON CONFLICT (CODE) DO UPDATE WHERE version distinct
        │  insert → gera secretKey + updateRegistration=true (criado)
        │  update → só quando version muda (atualizado)
        ▼
RETURNING id, secret_key, (xmax=0 ? inserted) → RegistrySystemOutput

autenticação (interceptor): findByCodeAndSecretKey
        ▼ credencial inválida → ScosException(SCOS_SYSTEM_00x) → Status.UNAUTHENTICATED + code no trailer
```

### Decisões Técnicas
| Decisão | Escolha (proposta) | Alternativa Descartada | Motivo |
|---------|--------------------|------------------------|--------|
| Persistência do registro | Upsert nativo condicional por `version` | `find` + `merge`/`update` | Consistência com o resource; evita update/dead tuple quando `version` não muda |
| "criado vs atualizado" no upsert | `RETURNING ... , (xmax = 0) AS inserted` | Upsert + releitura + flag | 1 query; distingue insert de update em Postgres via `xmax` |
| `secretKey` só no insert | `INSERT ... secretKey` no VALUES; `DO UPDATE` não toca `SECRET_KEY` | Regenerar sempre | Não invalidar credencial de sistema já registrado |
| Erro de credencial | `ScosException(SCOS_SYSTEM_00x)` (401) | `NoSuchElementException` + string no interceptor | Motivo visível e consistente (i18n + status) |

### Banco de Dados
- **Impacto**: ⚠️ Parcial — `SCOS_SYSTEM` já tem UK em `CODE`; sem novas colunas. Só muda a forma de gravar (upsert nativo).

---

## 4️⃣ Implementação

### Arquivos

**Modificados**:
- `flow-organization-domain/.../system/internal/ScosSystemRepository.java` — +`upsert` nativo condicional (com `RETURNING`).
- `flow-organization-domain/.../system/service/ScosSystemServiceBean.java` — `register` via upsert; `findByCodeAndSecretKey` → `ScosException`.
- `flow-organization-shared/.../exception/ExceptionCodeError.java` — +`SCOS_SYSTEM_00x`.
- `flow-organization-shared/.../resources/scos_message_organization{,_en}.properties` — +mensagens.
- `grpc/.../interceptor/TokenAuthorizationInterceptor.java` — tratar `ScosException` (deixar fluir ao handler, ou mapear para `UNAUTHENTICATED` com `code`).

### Tarefas
- [ ] **T-01**: `ScosSystemRepository.upsert` nativo condicional (`ON CONFLICT (CODE) DO UPDATE ... WHERE version distinct`, `RETURNING id, secret_key, xmax=0`).
- [ ] **T-02**: `ScosSystemServiceBean.register` usa o upsert; preserva `secretKey`-só-no-insert e `updateRegistration`.
- [ ] **T-03**: `findByCodeAndSecretKey` → `ScosException(SCOS_SYSTEM_00x)`; ajustar interceptor.
- [ ] **T-04**: Códigos `SCOS_SYSTEM_*` + mensagens pt/en.
- [ ] **T-05**: Testes — re-registro sem mudança de `version` não atualiza; credencial inválida → erro visível.

### Riscos e Edge Cases
1. **`RETURNING` com Spring Data `@Modifying`**: `@Modifying` retorna `int`. Para obter `id/secret_key/inserted` pode ser preciso `@Query` sem `@Modifying` (SELECT-like) com upsert `RETURNING`, ou nativo via `EntityManager`. Validar a abordagem.
2. **Distinguir insert de update**: `xmax = 0` no `RETURNING` indica linha recém-inserida (heurística Postgres) — validar em teste.
3. **`secretKey` no update**: garantir que o `DO UPDATE` **não** sobrescreve `SECRET_KEY` (senão invalida o sistema já registrado).
4. **Depende do handler gRPC** já entregue na change do resource (mapeamento `ScosException`→`Status`) — reuso direto.
5. **Trilha `@Auditable`** não dispara no upsert nativo (aceito).

---

## 📎 Referências
- `flow-organization-domain/.../system/service/ScosSystemServiceBean.java` (register + findByCodeAndSecretKey)
- `grpc/.../interceptor/TokenAuthorizationInterceptor.java` (autenticação de sistema)
- Padrão de referência (já implementado): `openspec/changes/permission-metadata-registry-enrichment/` (upsert condicional + handler)

---
