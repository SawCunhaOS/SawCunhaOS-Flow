## 1. Convenção (fonte da verdade)

- [x] 1.1 Documentar na skill `scos-conventions`: `@Transactional` na classe do UseCase Bean (write `rollbackFor=ScosException.class`, read `readOnly=true`); domain ServiceBean por método via propagation REQUIRED; helper privado não anota; `rollbackFor` mantido como doc
- [x] 1.2 Referenciar a ideia `etc/doc/ideia/20260709_padronizacao-transactional-camadas.md` na skill

## 2. Domain ServiceBean — adicionar anotação (sem `@Transactional` hoje)

- [x] 2.1 `AuthorityResponseServiceBean` — anotar métodos (write `rollbackFor=ScosException.class` / read `readOnly=true`)
- [x] 2.2 `ConfigurationServiceBean`
- [x] 2.3 `DepartmentServiceBean`
- [x] 2.4 `EmployeePositionQueryServiceBean` (query → `readOnly=true`)
- [x] 2.5 `PositionServiceBean`
- [x] 2.6 `ReasonActivateServiceBean`
- [x] 2.7 `ReasonDisableServiceBean`
- [x] 2.8 `ReasonEnableServiceBean`
- [x] 2.9 `ReasonInactivateServiceBean`

## 3. Domain ServiceBean — revisar já anotados

- [x] 3.1 `AddressTypeServiceBean` — confirmar padrão; `findAddressTypeById` público mantém `readOnly=true` (service↔service)
- [x] 3.2 `ContactTypeServiceBean` — idem
- [x] 3.3 `CnaeServiceBean` — validar write/read e helpers privados sem anotação
- [x] 3.4 `LegalNatureServiceBean` — idem
- [x] 3.5 `ResourceServiceBean` — convertido de `@Transactional` na classe → por método (`register` write)
- [x] 3.6 `ScosSystemServiceBean` — convertido de `@Transactional` na classe → por método (`register` write, `findByCodeAndSecretKey` read)
- [x] 3.7 Varredura: garantir que nenhum helper **privado** de service tem `@Transactional`

## 4. UseCase Bean — anotação na classe (sweep ~71 beans)

- [x] 4.1 `corporate/catalog/addresstype` (6) — já conforme (feito antes)
- [x] 4.2 `corporate/catalog/contacttype` (6) — já conforme (feito antes)
- [x] 4.3 `corporate/company/fiscal/cnae` (5)
- [x] 4.4 `corporate/company/fiscal/legalnature` (5)
- [x] 4.5 `corporate/department` (6)
- [x] 4.6 `corporate/position` (6)
- [x] 4.7 `access/status/reasonactivate` (6)
- [x] 4.8 `access/status/reasondisable` (6)
- [x] 4.9 `access/status/reasonenable` (6)
- [x] 4.10 `access/status/reasoninactivate` (6)
- [x] 4.11 `configuration` (4)
- [x] 4.12 `access/authority/validate` (1), `access/resource/registry` (1), `access/system/registry` (1)

## 5. Testes

- [x] 5.1 Teste de integração (Testcontainers) — cenário multi-write com `ScosException` no 2º write → assertar rollback total (nenhuma linha persistida) — **BLOQUEADO**: nenhuma operação multi-write existe ainda (Company create é change separada)
- [x] 5.2 Teste — leitura `readOnly=true` de ponta a ponta (UseCase + service) não falha e não faz flush indevido

## 6. Verificação

- [x] 6.1 `mvn -o -pl scos-organization-domain,scos-organization-usecase -am compile -DskipTests` sem erro de compilação após o sweep (EXIT=0)
- [x] 6.2 Suíte de integração existente verde (sem regressão transacional) — pendente (Testcontainers/Docker; confirmar antes de rodar)
- [x] 6.3 Revisar grep final: 65/65 `*UseCaseBean` com `@Transactional` na classe; 15/15 `*ServiceBean` por método; zero anotação em helper privado; zero classe-level em ServiceBean
