
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

package br.com.sawcunhaos.organization.domain.corporate.catalog.service;

import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.foundation.core.specification.ScosUserAuthentication;
import br.com.sawcunhaos.organization.domain.access.status.internal.EntityType;
import br.com.sawcunhaos.organization.domain.corporate.catalog.dto.ContactTypeInput;
import br.com.sawcunhaos.organization.domain.corporate.catalog.dto.ContactTypeOutput;
import br.com.sawcunhaos.organization.domain.corporate.catalog.internal.ContactType;
import br.com.sawcunhaos.organization.domain.corporate.catalog.internal.ContactTypeRepository;
import br.com.sawcunhaos.organization.domain.corporate.catalog.specification.ContactTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_CONTACT_TYPE_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_CONTACT_TYPE_002;

/**
 * Implementação de {@link ContactTypeService} — regras de unicidade de {@code code} por {@code entityType}
 * e idempotência de ativação/inativação ficam no agregado {@link ContactType}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
class ContactTypeServiceBean implements ContactTypeService {

    private final ContactTypeRepository contactTypeRepository;
    private final ContactTypeMapper contactTypeMapper;
    private final ScosUserAuthentication scosUserAuthentication;

    /**
     * @throws ScosException SCOS_CONTACT_TYPE_002 se já existir um {@link ContactType} com o mesmo {@code code}/{@code entityType}.
     */
    @Override
    @Transactional(rollbackFor = ScosException.class)
    public ContactTypeOutput create(@NonNull ContactTypeInput contactTypeInput) {
        log.info("Create ContactType: {}", contactTypeInput.code());

        if (contactTypeRepository.existsByCodeAndEntityType(contactTypeInput.code(), contactTypeInput.entityType())) {
            throw new ScosException(SCOS_CONTACT_TYPE_002);
        }

        ContactType contactType = ContactType.builder()
                .code(contactTypeInput.code())
                .description(contactTypeInput.description())
                .entityType(contactTypeInput.entityType())
                .active(true)
                .build();
        contactType.updateAuditInfo(scosUserAuthentication.findUserAuthentication());

        contactType = contactTypeRepository.merge(contactType);

        return ContactTypeOutput.builder()
                .id(contactType.getId())
                .code(contactType.getCode())
                .description(contactType.getDescription())
                .entityType(contactType.getEntityType())
                .active(contactType.isActive())
                .build();
    }

    /**
     * @throws ScosException SCOS_CONTACT_TYPE_001 se o {@code id} não existir.
     * @throws ScosException SCOS_CONTACT_TYPE_002 se o novo {@code code}/{@code entityType} colidir com outro registro.
     */
    @Override
    @Transactional(rollbackFor = ScosException.class)
    public void update(@NonNull ContactTypeInput contactTypeInput) {
        log.info("Update ContactType: {}", contactTypeInput.id());
        ContactType contactType = findContactTypeById(contactTypeInput.id());

        if (contactTypeRepository.existsByCodeAndEntityAndNotId(contactTypeInput.code(), contactTypeInput.entityType(), contactTypeInput.id())) {
            throw new ScosException(SCOS_CONTACT_TYPE_002);
        }

        contactType.setCode(contactTypeInput.code());
        contactType.setDescription(contactTypeInput.description());
        contactType.setEntityType(contactTypeInput.entityType());
        contactType.updateAuditInfo(scosUserAuthentication.findUserAuthentication());
        contactTypeRepository.update(contactType);
    }

    /**
     * @throws ScosException SCOS_CONTACT_TYPE_001 se o {@code id} não existir.
     */
    @Override
    @Transactional(readOnly = true)
    public ContactTypeOutput findById(@NonNull Long contactTypeId) {
        log.info("Find ContactType by Id: {}", contactTypeId);
        ContactType contactType = findContactTypeById(contactTypeId);
        return contactTypeMapper.toContactTypeOutput(contactType);
    }

    /**
     * Lista paginada, filtrando por {@code entityType} quando informado.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ContactTypeOutput> findAll(EntityType entityType,
                                           Boolean active,
                                           @NonNull Pageable pageable
    ) {
        log.info("Find All ContactTypes, EntityType: {}", entityType);
        return contactTypeRepository.findAllFiltered(entityType, active, pageable)
                .map(contactTypeMapper::toContactTypeOutput);
    }

    /**
     * @throws ScosException SCOS_CONTACT_TYPE_001 se o {@code id} não existir.
     * @throws ScosException SCOS_CONTACT_TYPE_003 se já estiver ativo.
     */
    @Override
    @Transactional(rollbackFor = ScosException.class)
    public void enable(@NonNull Long contactTypeId) {
        log.info("Enable ContactType: {}", contactTypeId);
        ContactType contactType = findContactTypeById(contactTypeId);
        contactType.activate();

        contactTypeRepository.update(contactType);
        log.info("ContactType enabled");
    }

    /**
     * @throws ScosException SCOS_CONTACT_TYPE_001 se o {@code id} não existir.
     * @throws ScosException SCOS_CONTACT_TYPE_004 se já estiver inativo.
     */
    @Override
    @Transactional(rollbackFor = ScosException.class)
    public void disable(@NonNull Long contactTypeId) {
        log.info("Disable ContactType: {}", contactTypeId);
        ContactType contactType = findContactTypeById(contactTypeId);
        contactType.deactivate();

        contactTypeRepository.update(contactType);
        log.info("ContactType disabled");
    }

    @Override
    @Transactional(readOnly = true)
    public ContactType findContactTypeById(@NonNull Long contactTypeId) {
        log.info("Find ContactType by Id: {}", contactTypeId);
        return contactTypeRepository.findById(contactTypeId).orElseThrow(
                () -> new ScosException(SCOS_CONTACT_TYPE_001)
        );
    }
}
