
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

package br.com.sawcunhaos.organization.domain.access.status.service;

import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.foundation.core.specification.ScosUserAuthentication;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonDisableInput;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonDisableOutput;
import br.com.sawcunhaos.organization.domain.access.status.internal.EntityType;
import br.com.sawcunhaos.organization.domain.access.status.internal.ReasonDisable;
import br.com.sawcunhaos.organization.domain.access.status.internal.ReasonDisableRepository;
import br.com.sawcunhaos.organization.domain.access.status.specification.ReasonDisableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_REASON_DISABLE_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_REASON_DISABLE_002;

/**
 * Implementação de {@link ReasonDisableService} — regras de unicidade de {@code code} por catálogo
 * e idempotência de ativação/inativação ficam no agregado {@link ReasonDisable}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReasonDisableServiceBean implements ReasonDisableService {

    private final ReasonDisableRepository reasonDisableRepository;
    private final ReasonDisableMapper reasonDisableMapper;
    private final ScosUserAuthentication scosUserAuthentication;

    /**
     * @throws ScosException SCOS_REASON_DISABLE_002 se já existir um {@link ReasonDisable} com o mesmo {@code code}/{@code entityType}.
     */
    @Override
    @Transactional(rollbackFor = ScosException.class)
    public ReasonDisableOutput create(@NonNull ReasonDisableInput reasonDisableInput) {
        log.info("Create ReasonDisable: {}", reasonDisableInput.code());

        if (reasonDisableRepository.existsByCodeAndEntityType(reasonDisableInput.code(), reasonDisableInput.entityType())) {
            throw new ScosException(SCOS_REASON_DISABLE_002);
        }

        ReasonDisable reasonDisable = ReasonDisable.builder()
                .code(reasonDisableInput.code())
                .description(reasonDisableInput.description())
                .entityType(reasonDisableInput.entityType())
                .active(true)
                .build();
        reasonDisable.updateAuditInfo(scosUserAuthentication.findUserAuthentication());

        reasonDisable = reasonDisableRepository.merge(reasonDisable);

        return ReasonDisableOutput.builder()
                .id(reasonDisable.getId())
                .code(reasonDisable.getCode())
                .description(reasonDisable.getDescription())
                .entityType(reasonDisable.getEntityType())
                .active(reasonDisable.isActive())
                .build();
    }

    /**
     * @throws ScosException SCOS_REASON_DISABLE_001 se o {@code id} não existir.
     * @throws ScosException SCOS_REASON_DISABLE_002 se o novo {@code code}/{@code entityType} colidir com outro registro.
     */
    @Override
    @Transactional(rollbackFor = ScosException.class)
    public void update(@NonNull ReasonDisableInput reasonDisableInput) {
        log.info("Update ReasonDisable: {}", reasonDisableInput.id());
        ReasonDisable reasonDisable = findReasonDisableById(reasonDisableInput.id());

        if (reasonDisableRepository.existsByCodeAndEntityTypeAndNotId(reasonDisableInput.code(), reasonDisableInput.entityType(), reasonDisableInput.id())) {
            throw new ScosException(SCOS_REASON_DISABLE_002);
        }

        reasonDisable.setCode(reasonDisableInput.code());
        reasonDisable.setDescription(reasonDisableInput.description());
        reasonDisable.setEntityType(reasonDisableInput.entityType());
        reasonDisable.updateAuditInfo(scosUserAuthentication.findUserAuthentication());
        reasonDisableRepository.update(reasonDisable);
    }

    /**
     * @throws ScosException SCOS_REASON_DISABLE_001 se o {@code id} não existir.
     */
    @Override
    @Transactional(readOnly = true)
    public ReasonDisableOutput findById(@NonNull Long reasonDisableId) {
        log.info("Find ReasonDisable by Id: {}", reasonDisableId);
        ReasonDisable reasonDisable = findReasonDisableById(reasonDisableId);
        return reasonDisableMapper.toReasonDisableOutput(reasonDisable);
    }

    /**
     * Lista paginada, filtrando por {@code entityType} quando informado.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ReasonDisableOutput> findAll(EntityType entityType,
                                             Boolean active,
                                             @NonNull Pageable pageable
    ) {
        log.info("Find All ReasonDisable, EntityType: {}", entityType);
        return reasonDisableRepository.findAllFiltered(entityType, active, pageable)
                .map(reasonDisableMapper::toReasonDisableOutput);
    }

    /**
     * @throws ScosException SCOS_REASON_DISABLE_001 se o {@code id} não existir.
     * @throws ScosException SCOS_REASON_DISABLE_003 se já estiver ativo.
     */
    @Override
    @Transactional(rollbackFor = ScosException.class)
    public void enable(@NonNull Long reasonDisableId) {
        log.info("Enable ReasonDisable: {}", reasonDisableId);
        ReasonDisable reasonDisable = findReasonDisableById(reasonDisableId);
        reasonDisable.activate();

        reasonDisableRepository.update(reasonDisable);
        log.info("ReasonDisable enabled");
    }

    /**
     * @throws ScosException SCOS_REASON_DISABLE_001 se o {@code id} não existir.
     * @throws ScosException SCOS_REASON_DISABLE_004 se já estiver inativo.
     */
    @Override
    @Transactional(rollbackFor = ScosException.class)
    public void disable(@NonNull Long reasonDisableId) {
        log.info("Disable ReasonDisable: {}", reasonDisableId);
        ReasonDisable reasonDisable = findReasonDisableById(reasonDisableId);
        reasonDisable.deactivate();

        reasonDisableRepository.update(reasonDisable);
        log.info("ReasonDisable disabled");
    }

    private ReasonDisable findReasonDisableById(@NonNull Long reasonDisableId) {
        log.info("Find ReasonDisable by Id: {}", reasonDisableId);
        return reasonDisableRepository.findById(reasonDisableId).orElseThrow(
                () -> new ScosException(SCOS_REASON_DISABLE_001)
        );
    }
}
