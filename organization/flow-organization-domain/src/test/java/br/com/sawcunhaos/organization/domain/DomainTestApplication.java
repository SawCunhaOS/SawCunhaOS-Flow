
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

package br.com.sawcunhaos.organization.domain;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;

/**
 * {@code flow-organization-domain} é módulo biblioteca, sem {@code @SpringBootApplication} próprio.
 * {@code @DataJpaTest} exige um {@code @SpringBootConfiguration} localizável subindo os pacotes a
 * partir da classe de teste — esta classe supre esse requisito para toda a árvore
 * {@code br.com.sawcunhaos.organization.domain.*}. {@code @AutoConfigurationPackage} (não
 * {@code @EnableAutoConfiguration}, que traria auto-config não relacionada a JPA — ex. Jackson —
 * ausente do classpath de teste deste módulo biblioteca) registra {@code AutoConfigurationPackages},
 * usado por {@code DataJpaRepositoriesAutoConfiguration} para descobrir os repositórios do slice.
 */
@SpringBootConfiguration
@AutoConfigurationPackage
public class DomainTestApplication {
}
