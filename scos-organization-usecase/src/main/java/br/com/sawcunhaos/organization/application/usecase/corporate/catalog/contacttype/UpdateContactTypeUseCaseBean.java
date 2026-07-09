
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

package br.com.sawcunhaos.organization.application.usecase.corporate.catalog.contacttype;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.UpdateContactTypeRequest;
import br.com.sawcunhaos.organization.domain.corporate.catalog.dto.ContactTypeInput;
import br.com.sawcunhaos.organization.domain.corporate.catalog.specification.ContactTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação de {@link UpdateContactTypeUseCase}. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class)
class UpdateContactTypeUseCaseBean implements UpdateContactTypeUseCase {
    private final ContactTypeService contactTypeService;

    @Override
    public void execute(@NonNull Long id, @NonNull UpdateContactTypeRequest updateContactTypeRequest) {
        contactTypeService.update(
                ContactTypeInput.builder()
                        .id(id)
                        .code(updateContactTypeRequest.code())
                        .description(updateContactTypeRequest.description())
                        .entityType(ContactTypeApiMapper.toDomainEntityType(updateContactTypeRequest.entityType()))
                        .build()
        );
    }
}
