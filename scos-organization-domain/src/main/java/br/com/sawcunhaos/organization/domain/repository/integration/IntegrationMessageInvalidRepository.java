
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

package br.com.sawcunhaos.organization.domain.repository.integration;

import br.com.sawcunhaos.organization.domain.model.integration.IntegrationMessageInvalid;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IntegrationMessageInvalidRepository extends BaseJpaRepository<IntegrationMessageInvalid, UUID>, JpaSpecificationExecutor<IntegrationMessageInvalid>, QuerydslPredicateExecutor<IntegrationMessageInvalid> {
}
