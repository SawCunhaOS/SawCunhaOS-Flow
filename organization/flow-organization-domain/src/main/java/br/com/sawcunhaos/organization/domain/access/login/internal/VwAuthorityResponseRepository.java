
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

package br.com.sawcunhaos.organization.domain.access.login.internal;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;

import java.util.Optional;

public interface VwAuthorityResponseRepository extends BaseJpaRepository<VwAuthorityResponse, Long> {
    Optional<VwAuthorityResponse> findByLogin(String login);
}
