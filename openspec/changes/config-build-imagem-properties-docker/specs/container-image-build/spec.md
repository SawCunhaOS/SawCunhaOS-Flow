## ADDED Requirements

### Requirement: Bloco de imagem configurado por módulo deployável

Cada módulo deployável (`scos-organization-boot`, `scos-organization-grpc-boot`) SHALL declarar o bloco `<image>` no `spring-boot-maven-plugin` com nome fixo, builder pinado em tag exata e `pullPolicy` explícito. O builder MUST NOT usar a tag `latest`.

#### Scenario: Nome de imagem previsível

- **WHEN** `mvn -pl scos-organization-boot spring-boot:build-image` roda
- **THEN** a imagem gerada é nomeada `scos-organization-boot:local`
- **AND** o `scos-organization-grpc-boot` gera `scos-organization-grpc-boot:local`

#### Scenario: Builder pinado, não latest

- **WHEN** o build de imagem é executado
- **THEN** o builder usado é `paketobuildpacks/builder-jammy-tiny` numa tag exata
- **AND** `pullPolicy` é `IF_NOT_PRESENT`

#### Scenario: Versão de JDK explícita no build

- **WHEN** a imagem é construída
- **THEN** o env de build inclui `BP_JVM_VERSION=25`

### Requirement: Build de imagem parametrizável sem editar o pom

O build SHALL permitir alternar CDS e o thread-count da JVM por propriedade Maven (`-D`), sem editar o pom. O default de CDS MUST reproduzir o comportamento pretendido (`true`) e MUST ser desativável para o fallback do bug conhecido (paketo-buildpacks/spring-boot#581).

#### Scenario: Desabilitar CDS por linha de comando

- **WHEN** `mvn spring-boot:build-image -Dbp.cds.enabled=false` roda
- **THEN** o build usa `BP_JVM_CDS_ENABLED=false` sem que o pom seja alterado

#### Scenario: Thread-count parametrizável

- **WHEN** o build roda sem override
- **THEN** `BPL_JVM_THREAD_COUNT` assume um default sensato para 1 CPU
- **AND** pode ser sobrescrito por propriedade Maven

### Requirement: Build isolado por módulo

Construir a imagem de um módulo SHALL funcionar sem buildar o reactor inteiro.

#### Scenario: Build de um único módulo

- **WHEN** `mvn -pl scos-organization-boot spring-boot:build-image` roda
- **THEN** a imagem do `boot` é gerada sem exigir build dos demais módulos do reactor
