
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

package br.com.sawcunhaos.organization.domain.repository.configuration;

import br.com.sawcunhaos.organization.domain.model.configuration.PartnersConfiguration;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PartnersConfigurationRepository extends BaseJpaRepository<PartnersConfiguration, String> {

    Page<PartnersConfiguration> findAll(Pageable pageable);

    Optional<PartnersConfiguration> findById(String id);
}
