
/*
 *
 *  * Copyright 2026 SawCunha Open System - SawCunhaOS-Organization
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *
 */

package br.com.sawcunhaos.organization.domain.access.system.internal;

import br.com.sawcunhaos.organization.shared.converter.SecretKeyConverter;
import br.com.sawcunhaos.organization.shared.utils.SystemSecretCryptoService;
import io.hypersistence.utils.spring.repository.BaseJpaRepositoryImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Primeiro {@code @DataJpaTest} do projeto — prova o wiring de {@link SecretKeyConverter}: valor
 * fica cifrado em repouso (coluna crua) e volta em claro ao recarregar via repositório (nova sessão).
 * {@code @EntityScan}/{@code @EnableJpaRepositories} restritos a este pacote: o slice padrão
 * escanearia TODAS as entidades e repositórios de {@code br.com.sawcunhaos.organization.domain}
 * (pacote do {@code @SpringBootConfiguration} de teste) — incluindo entidades com tipo JSON do
 * hypersistence-utils que exigem Jackson (ausente do classpath de teste deste módulo biblioteca) e
 * repositórios cujas entidades ficariam fora do persistence-unit restrito. Fora do escopo desta story.
 * {@code repositoryBaseClass = BaseJpaRepositoryImpl.class}: achado desta story, fora das ACs —
 * nenhum módulo do projeto configura {@code @EnableJpaRepositories(repositoryBaseClass=...)}, então
 * os métodos extras de {@code BaseJpaRepository} (persist/merge/update/lockById) não resolvem via
 * {@code SimpleJpaRepository} e falham na criação do bean por tentativa de derivação de query
 * (`PropertyReferenceException: No property 'update' found`). Sem esse fix aqui, nem o {@code findById}
 * usado por este teste chega a rodar — o bean de repositório falha antes disso. Setado só neste
 * teste (não é escopo desta story alterar a configuração de produção); registrar como débito.
 */
@DataJpaTest
@EntityScan(basePackageClasses = ScosSystem.class)
@EnableJpaRepositories(basePackageClasses = ScosSystemRepository.class, repositoryBaseClass = BaseJpaRepositoryImpl.class)
@Import({SecretKeyConverter.class, SystemSecretCryptoService.class})
@TestPropertySource(properties = "scos.security.master-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
class ScosSystemRepositoryPersistenceTest {

    @Autowired
    private ScosSystemRepository scosSystemRepository;
    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void secretKeyShouldBeEncryptedAtRestAndDecryptedOnReload() {
        ScosSystem system = ScosSystem.builder()
                .code("TEST_SYS").name("Test").description("Test")
                .secretKey("plain-secret-value").status("ACTIVE").version("1.0.0")
                .build();
        system.updateAuditInfo("test");

        ScosSystem saved = entityManager.persistFlushFind(system);
        entityManager.clear();

        String rawColumn = jdbcTemplate.queryForObject(
                "SELECT SECRET_KEY FROM SCOS_SYSTEM WHERE SYSTEM_ID = ?",
                String.class, saved.getId());
        assertThat(rawColumn).isNotEqualTo("plain-secret-value");

        ScosSystem reloaded = scosSystemRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getSecretKey()).isEqualTo("plain-secret-value");
    }
}
