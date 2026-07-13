
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

package br.com.sawcunhaos.organization.boot.infrastructure;

import br.com.sawcunhaos.organization.boot.ScosOrganizationApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.ComposeContainer;

import java.io.File;
import java.util.Objects;

/**
 * Base de TODOS os testes de integração do módulo. Centraliza a configuração comum para que
 * N classes de teste rodem sobre a MESMA infra, sem reiniciar containers nem recriar contexto:
 *
 * <ul>
 *   <li><b>Containers SINGLETON</b> — os dois {@link ComposeContainer} (Postgres/Redis) sobem
 *       UMA vez por JVM no bloco {@code static} e permanecem ativos para todas as classes. Como
 *       o compose publica portas fixas (5432/6379), NÃO pode haver dois Postgres simultâneos:
 *       {@code @Container}/{@code @Testcontainers} (que reinicia por classe) causaria conflito de
 *       porta e "relation does not exist" (o contexto Spring é cacheado e o Liquibase roda só no
 *       1º). O singleton resolve. O Ryuk do Testcontainers remove os containers ao encerrar o JVM.</li>
 *   <li><b>{@code @SpringBootTest} + {@code @AutoConfigureMockMvc} aqui na base</b> — todas as
 *       subclasses herdam a MESMA configuração, então o Spring cacheia UM contexto e o reaproveita
 *       entre as classes (Liquibase roda uma vez). A subclasse só adiciona os {@code @Test}.</li>
 *   <li><b>Isolamento por método</b> — {@code @Sql} recria o seed (BEFORE) e faz
 *       TRUNCATE ... RESTART IDENTITY (AFTER) a cada método, então dados criados por um teste não
 *       vazam para o próximo, mesmo com o banco compartilhado entre classes.</li>
 * </ul>
 *
 * <p>Requisito de execução: Surefire em um único JVM (padrão {@code forkCount=1}/{@code reuseForks=true})
 * — o {@code static} inicializa na 1ª classe carregada e vale para toda a suíte.
 */
@Sql(scripts = "classpath:postgresql/setsup_database.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@SpringBootTest(classes = ScosOrganizationApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ScosOrganizationTestUtil extends ScosOrganizationWiremockUtil {

    protected static final ComposeContainer COMPOSE_CONTAINER_REDIS = new ComposeContainer(
            new File("../etc/infra/docker-compose-redis.yml")
    ).withBuild(true);

    protected static final ComposeContainer COMPOSE_CONTAINER_POSTGRESQL = new ComposeContainer(
            new File("../etc/infra/docker-compose-database.yml")
    ).withBuild(true);

    static {
        COMPOSE_CONTAINER_REDIS.start();
        COMPOSE_CONTAINER_POSTGRESQL.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    // Tokens emitidos em runtime e validados contra o JWKS mockado (ScosJwtTestSupport).
    // BEAR_TOKEN_VALID: exp ~100 anos, login "inside.admin". BEAR_TOKEN_INVALID: chave fora do JWKS → 401.
    protected String BEAR_TOKEN_VALID = ScosJwtTestSupport.bearer("inside.admin");
    protected String BEAR_TOKEN_INVALID = ScosJwtTestSupport.invalidBearer();
    protected String LANGUAGE_EN = "en-US";
    protected String LANGUAGE_PT = "pt-BR";

    protected HttpHeaders httpHeaders(String acceptLanguage, String bearerToken, String contentType) {
        HttpHeaders headers = new HttpHeaders();
        if (Objects.nonNull(bearerToken)) {
            headers.add("Authorization", bearerToken);
        }
        headers.add("Accept-Language", acceptLanguage);
        headers.add("Content-Type", contentType);
        return headers;
    }

}
