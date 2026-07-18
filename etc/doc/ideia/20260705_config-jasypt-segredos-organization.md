# Externalização e Cifra de Segredos (Jasypt) em flow-organization-boot/grpc-boot

**Data**: 2026-07-05
**Status**: 🔄 Em Análise
**Tipo**: 🔧 Refatoração

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

> **Regra**: Uma ideia = uma funcionalidade. Features independentes → arquivos separados.

- **Nome da funcionalidade**: `config-jasypt-segredos-organization`
- **Resumo em uma frase**: Segredos hoje fixos em texto plano no `application.yml`/`bootstrap.yml` (senha de banco, senha de cache, chave de acesso do registry, chave de criptografia da privacy) viram env var por instalação, com suporte a valor cifrado (`ENC(...)`, Jasypt) pra quem quiser não deixar nem o valor cifrado — só o texto plano — exposto em lugar nenhum.

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade
- [x] Não mistura features independentes no mesmo arquivo
- [x] O nome é específico (não genérico como "melhorar módulo X")

---

## 1️⃣ Visão

### Problema
`flow-organization-boot` e `flow-organization-grpc-boot` têm 5 segredos hardcoded em texto plano nos arquivos de config versionados: `scos.datasource.password`, `scos.audit.datasource.password`, `scos.cache.password` (todos `scos#2026`), `scos.registry.key-access` (`ABLABLABLA`, valor claramente fake) e `scos.privacy.crypto.secret` (`ASDASDASDA`, idem). Isso foi tolerável enquanto só existia 1 ambiente de dev compartilhado. Com o modelo confirmado de **deploy separado por cliente** (on-premise/whitelabel, ver [[config-build-imagem-e-properties-docker]]), isso vira 2 problemas reais:

1. **Cada cliente precisa da sua própria credencial** — não dá pra todo cliente usar a mesma senha de banco/Redis/registry
2. `scos.privacy.crypto.secret` é a chave que cifra dado sensível (PII) em repouso — se toda instalação usar a mesma chave hardcoded no jar/imagem, um vazamento em qualquer cliente compromete a cifra de **todos os outros clientes** também. Não é só má prática, é risco de segurança concreto num produto multi-cliente

A skill `spring-security-scos` já documenta o padrão SCOS esperado: segredo se resolve com **Jasypt**, não hardcode. `scos-bom` já tem `jasypt-spring-boot-starter` (`com.github.ulisesbocchio`, versão gerenciada em `dependencyManagement`) — só falta ser adicionado e usado nos módulos do `organization`.

### Objetivo
- Segredos viram `${VAR}` (sem default "de produção" — só um default de conveniência pro dev local, igual ao valor de hoje) nos 2 módulos
- `jasypt-spring-boot-starter` adicionado às dependências, habilitando o app a decifrar transparentemente qualquer valor recebido no formato `ENC(...)` — venha de onde vier (env var, arquivo externo de config)
- Documentar o fluxo completo: como um cliente gera seu próprio `ENC(...)`, como fornece a senha mestra (`jasypt.encryptor.password`) separada do valor cifrado, e por que isso não quebra o dev local

### Fora de Escopo
- Definir **quais** hosts/portas variam por instalação — já resolvido em [[config-build-imagem-e-properties-docker]] (username do datasource também fica lá, por não ser segredo)
- Ativar Config Server — incerto, fora de escopo (ver decisão equivalente na ideia de properties)
- Rotação de chave/segredo já em produção, gestão de ciclo de vida de secret (Vault, rotação automática) — essa ideia cobre só "como não commitar segredo em texto plano e como decifrar em runtime", não um sistema de gestão de segredo completo

---

## 2️⃣ Requisitos

### Funcionais
- [ ] **RF-01**: Adicionar dependência `com.github.ulisesbocchio:jasypt-spring-boot-starter` (sem version — já gerenciada em `scos-bom`) no `pom.xml` de `flow-organization-boot` e `flow-organization-grpc-boot`
- [ ] **RF-02**: `scos.datasource.password` e `scos.audit.datasource.password` viram `${SCOS_DB_PASSWORD:scos#2026}` (mesma env var reaproveitada entre principal/auditoria, mesma instância física — mesmo padrão de reaproveitamento já usado pra `SCOS_DB_HOST` na outra ideia)
- [ ] **RF-03**: `scos.cache.password` vira `${SCOS_CACHE_PASSWORD:scos#2026}`
- [ ] **RF-04**: `scos.registry.key-access` (`bootstrap.yml`, só `boot`) vira `${SCOS_REGISTRY_KEY_ACCESS:ABLABLABLA}` — default mantém o valor fake de hoje só pra não quebrar dev local; qualquer deploy real **precisa** sobrescrever
- [ ] **RF-05**: `scos.privacy.crypto.secret` vira `${SCOS_PRIVACY_CRYPTO_SECRET:ASDASDASDA}` nos 2 módulos — mesmo raciocínio do RF-04
- [ ] **RF-06**: Documentar (README) o fluxo de geração de `ENC(...)`: cliente gera o valor cifrado com sua própria senha mestra (via utilitário Jasypt — CLI ou `jasypt-spring-boot-maven-plugin`), coloca `ENC(...)` na env var do secret (ex: `SCOS_DB_PASSWORD=ENC(base64...)`), e fornece a senha mestra via `JASYPT_ENCRYPTOR_PASSWORD` (relaxed binding pra `jasypt.encryptor.password`) — canal separado da env var do segredo em si
- [ ] **RF-07**: Documentar explicitamente que **local dev não precisa de nada disso** — sem `ENC(...)` em nenhuma env var, o Jasypt starter não exige `jasypt.encryptor.password` setado (só decifra o que casar com o padrão `ENC(...)`; texto plano passa direto)

### Não-Funcionais
- [ ] **RNF-01**: Rodar local sem setar nenhuma env var reproduz exatamente o comportamento de hoje (mesmos valores de dev), sem exigir senha mestra Jasypt
- [ ] **RNF-02**: Nenhum valor cifrado real (`ENC(...)` de produção) é commitado no repositório — só os defaults de dev (texto plano, já públicos hoje) ficam no `application.yml`/`bootstrap.yml`
- [ ] **RNF-03**: A senha mestra do Jasypt (`jasypt.encryptor.password`) nunca é a mesma coisa que o segredo que ela protege — documentar que são 2 valores/canais distintos, pra não virar "cifrar com a própria senha"

---

## 3️⃣ Arquitetura

### Componentes Afetados
```
flow-organization-boot/pom.xml
└── adiciona dependency com.github.ulisesbocchio:jasypt-spring-boot-starter

flow-organization-boot/src/main/resources/application.yml
└── datasource.password, audit.datasource.password, cache.password,
    privacy.crypto.secret: texto plano → ${VAR:valor-de-dev-atual}

flow-organization-boot/src/main/resources/bootstrap.yml
└── registry.key-access: texto plano → ${VAR:valor-de-dev-atual}

grpc/flow-organization-grpc-boot/pom.xml
└── adiciona dependency com.github.ulisesbocchio:jasypt-spring-boot-starter

grpc/flow-organization-grpc-boot/src/main/resources/application.yml
└── mesmos placeholders que o boot (sem registry, que só existe no boot)
```

### Fluxo Principal
```
Dev local (nada setado)
  → placeholders caem no default de dev (texto plano, igual hoje)
  → Jasypt starter presente mas ocioso (nenhum ENC() pra decifrar)

Deploy real (cliente X)
  1. Operador gera ENC(<segredo real>) com uma senha mestra escolhida
     (utilitário Jasypt / jasypt-spring-boot-maven-plugin)
  2. Env var do segredo recebe o valor cifrado: SCOS_DB_PASSWORD=ENC(...)
  3. Env var separada carrega a senha mestra: JASYPT_ENCRYPTOR_PASSWORD=<senha>
  4. App decifra em runtime — texto plano do segredo nunca fica em disco
     nem em histórico de shell, só em memória do processo
```

### Decisões Técnicas
| Decisão | Escolha | Alternativa Descartada | Motivo |
|---------|---------|------------------------|--------|
| Biblioteca | `jasypt-spring-boot-starter` (já em `scos-bom`) | Vault, AWS/GCP Secrets Manager, k8s Secrets nativo | Já gerenciada na BOM, zero infra nova pra adicionar; funciona igual em qualquer plataforma de deploy (não amarra a um cloud provider especifico) — importante pro modelo on-premise/whitelabel |
| Onde mora o valor cifrado | Env var (`SCOS_DB_PASSWORD=ENC(...)`), não hardcoded no yaml | `ENC(...)` fixo direto no `application.yml` versionado | Ciphertext fixo no yaml serviria só pra 1 segredo específico (não varia por cliente) — não funciona pro modelo de deploy separado por cliente, onde cada um tem sua própria credencial real |
| Senha mestra | Env var separada (`JASYPT_ENCRYPTOR_PASSWORD`), sem default de produção | Senha mestra fixa no código/imagem | Se a senha mestra for fixa e conhecida (ex: hardcoded), o `ENC(...)` vira decorativo — qualquer um com a imagem decifra |
| Dev local | Nenhuma mudança de fluxo — default texto plano, Jasypt ocioso | Forçar `ENC()` até em dev | Sem necessidade — segredo de dev já é público/conhecido; forçar Jasypt em dev só adicionaria fricção sem ganho de segurança real |
| Escopo desta ideia | Só mecanismo de cifra + placeholder dos 5 segredos já identificados | Também username do datasource, rotação de chave, integração com Vault | Username não é segredo (fica na outra ideia); rotação/Vault é gestão de ciclo de vida, categoria de problema diferente |

### Banco de Dados
- **Impacto**: ❌ Não

---

## 4️⃣ Implementação

### Arquivos

**Modificados**:
- `flow-organization-boot/pom.xml` — adiciona `jasypt-spring-boot-starter`
- `flow-organization-boot/src/main/resources/application.yml` — segredos viram placeholder
- `flow-organization-boot/src/main/resources/bootstrap.yml` — `registry.key-access` vira placeholder
- `grpc/flow-organization-grpc-boot/pom.xml` — adiciona `jasypt-spring-boot-starter`
- `grpc/flow-organization-grpc-boot/src/main/resources/application.yml` — segredos viram placeholder

### Tarefas
- [ ] **T-01**: Adicionar `jasypt-spring-boot-starter` no pom do `boot`
- [ ] **T-02**: Adicionar `jasypt-spring-boot-starter` no pom do `grpc-boot`
- [ ] **T-03**: Placeholder dos segredos em `application.yml` do `boot` (datasource, audit.datasource, cache, privacy.crypto)
- [ ] **T-04**: Placeholder de `registry.key-access` em `bootstrap.yml` do `boot`
- [ ] **T-05**: Placeholder dos segredos em `application.yml` do `grpc-boot`
- [ ] **T-06**: Documentar (README) o fluxo de geração de `ENC(...)` e a senha mestra, com exemplo de comando
- [ ] **T-07**: Validar que dev local sem nenhuma env var setada continua funcionando exatamente como hoje (regressão zero, sem exigir Jasypt configurado)
- [ ] **T-08**: Validar (manualmente, com um valor de teste) que um `ENC(...)` real + `JASYPT_ENCRYPTOR_PASSWORD` decifra corretamente em runtime

### Riscos e Edge Cases
1. Se `JASYPT_ENCRYPTOR_PASSWORD` não for setado mas alguma env var contiver `ENC(...)`, a app falha ao subir (Jasypt não consegue decifrar) — comportamento correto (falha explícita), mas precisa de mensagem de erro clara documentada, senão vira confuso pro time que for instalar num cliente
2. `scos.registry.key-access` e `scos.privacy.crypto.secret` mantêm um default de dev óbvio (`ABLABLABLA`, `ASDASDASDA`) — se alguém subir uma instalação real sem perceber que precisa sobrescrever, o sistema roda com chave fake e insegura silenciosamente (não há validação hoje que bloqueie subir em "modo produção" com secret default). Vale considerar um startup check separado (fora de escopo aqui) que recuse subir com esses valores conhecidos se um profile `prod` estiver ativo
3. `SCOS_DB_PASSWORD` reaproveitada entre datasource principal e de auditoria assume que são sempre a mesma credencial — se algum cliente um dia precisar de usuário/senha diferente pra auditoria, a variável precisa duplicar (mesma ressalva já registrada pra `SCOS_DB_HOST` na outra ideia)
4. Gerar o `ENC(...)` exige rodar o utilitário Jasypt (CLI ou plugin Maven) fora do fluxo normal da aplicação — documentar o comando exato evita que cada instalação improvise um jeito diferente de gerar o valor
5. Essa ideia não resolve *como* a senha mestra chega até o container do cliente com segurança (isso é responsabilidade do orquestrador/plataforma de deploy de cada instalação — k8s Secret, vault-agent, etc.) — só garante que a aplicação sabe consumir `ENC(...)` quando presente

---

## 📎 Referências
- `.claude/skills/spring-security-scos/references/resource-server.md` — convenção SCOS de Jasypt pra segredos
- `~/.m2/repository/br/com/sawcunhaos/scos-bom/1.2.0/scos-bom-1.2.0.pom` — `jasypt-spring-boot-starter` (`com.github.ulisesbocchio`, versão `4.0.4`) já gerenciado em `dependencyManagement`
- [[config-build-imagem-e-properties-docker]] — ideia irmã: topologia/tuning via placeholder; username do datasource fica lá por não ser segredo

---
