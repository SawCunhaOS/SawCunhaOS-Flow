
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

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.foundation.utils.specification.ScosUserAuthentication;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonEnableInput;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonEnableOutput;
import br.com.sawcunhaos.organization.domain.access.status.internal.EntityType;
import br.com.sawcunhaos.organization.domain.access.status.internal.ReasonEnable;
import br.com.sawcunhaos.organization.domain.access.status.internal.ReasonEnableRepository;
import br.com.sawcunhaos.organization.domain.access.status.specification.ReasonEnableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_REASON_ENABLE_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_REASON_ENABLE_002;

/**
 * Implementação de {@link ReasonEnableService} — regras de unicidade de {@code code} por catálogo
 * e idempotência de ativação/inativação ficam no agregado {@link ReasonEnable}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReasonEnableServiceBean implements ReasonEnableService {

    private final ReasonEnableRepository reasonEnableRepository;
    private final ReasonEnableMapper reasonEnableMapper;
    private final ScosUserAuthentication scosUserAuthentication;

    /**
     * @throws ScosException SCOS_REASON_ENABLE_002 se já existir um {@link ReasonEnable} com o mesmo {@code code}/{@code entityType}.
     */
    @Override
    @Transactional(rollbackFor = ScosException.class)
    public ReasonEnableOutput create(@NonNull ReasonEnableInput reasonEnableInput) {
        log.info("Create ReasonEnable: {}", reasonEnableInput.code());

        if (reasonEnableRepository.existsByCodeAndEntityType(reasonEnableInput.code(), reasonEnableInput.entityType())) {
            throw new ScosException(SCOS_REASON_ENABLE_002);
        }

        ReasonEnable reasonEnable = ReasonEnable.builder()
                .code(reasonEnableInput.code())
                .description(reasonEnableInput.description())
                .entityType(reasonEnableInput.entityType())
                .active(true)
                .build();
        reasonEnable.updateAuditInfo(scosUserAuthentication.findUserAuthentication());

        reasonEnable = reasonEnableRepository.merge(reasonEnable);

        return ReasonEnableOutput.builder()
                .id(reasonEnable.getId())
                .code(reasonEnable.getCode())
                .description(reasonEnable.getDescription())
                .entityType(reasonEnable.getEntityType())
                .active(reasonEnable.isActive())
                .build();
    }

    /**
     * @throws ScosException SCOS_REASON_ENABLE_001 se o {@code id} não existir.
     * @throws ScosException SCOS_REASON_ENABLE_002 se o novo {@code code}/{@code entityType} colidir com outro registro.
     */
    @Override
    @Transactional(rollbackFor = ScosException.class)
    public void update(@NonNull ReasonEnableInput reasonEnableInput) {
        log.info("Update ReasonEnable: {}", reasonEnableInput.id());
        ReasonEnable reasonEnable = findReasonEnableById(reasonEnableInput.id());

        if (reasonEnableRepository.existsByCodeAndEntityTypeAndNotId(reasonEnableInput.code(), reasonEnableInput.entityType(), reasonEnableInput.id())) {
            throw new ScosException(SCOS_REASON_ENABLE_002);
        }

        reasonEnable.setCode(reasonEnableInput.code());
        reasonEnable.setDescription(reasonEnableInput.description());
        reasonEnable.setEntityType(reasonEnableInput.entityType());
        reasonEnable.updateAuditInfo(scosUserAuthentication.findUserAuthentication());
        reasonEnableRepository.update(reasonEnable);
    }

    /**
     * @throws ScosException SCOS_REASON_ENABLE_001 se o {@code id} não existir.
     */
    @Override
    @Transactional(readOnly = true)
    public ReasonEnableOutput findById(@NonNull Long reasonEnableId) {
        log.info("Find ReasonEnable by Id: {}", reasonEnableId);
        ReasonEnable reasonEnable = findReasonEnableById(reasonEnableId);
        return reasonEnableMapper.toReasonEnableOutput(reasonEnable);
    }

    /**
     * Lista paginada, filtrando por {@code entityType} quando informado.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ReasonEnableOutput> findAll(EntityType entityType,
                                            Boolean active,
                                            @NonNull Pageable pageable
    ) {
        log.info("Find All ReasonEnable, EntityType: {}", entityType);
        return reasonEnableRepository.findAllFiltered(entityType, active, pageable)
                .map(reasonEnableMapper::toReasonEnableOutput);
    }

    /**
     * @throws ScosException SCOS_REASON_ENABLE_001 se o {@code id} não existir.
     * @throws ScosException SCOS_REASON_ENABLE_003 se já estiver ativo.
     */
    @Override
    @Transactional(rollbackFor = ScosException.class)
    public void enable(@NonNull Long reasonEnableId) {
        log.info("Enable ReasonEnable: {}", reasonEnableId);
        ReasonEnable reasonEnable = findReasonEnableById(reasonEnableId);
        reasonEnable.activate();

        reasonEnableRepository.update(reasonEnable);
        log.info("ReasonEnable enabled");
    }

    /**
     * @throws ScosException SCOS_REASON_ENABLE_001 se o {@code id} não existir.
     * @throws ScosException SCOS_REASON_ENABLE_004 se já estiver inativo.
     */
    @Override
    @Transactional(rollbackFor = ScosException.class)
    public void disable(@NonNull Long reasonEnableId) {
        log.info("Disable ReasonEnable: {}", reasonEnableId);
        ReasonEnable reasonEnable = findReasonEnableById(reasonEnableId);
        reasonEnable.deactivate();

        reasonEnableRepository.update(reasonEnable);
        log.info("ReasonEnable disabled");
    }

    private ReasonEnable findReasonEnableById(@NonNull Long reasonEnableId) {
        log.info("Find ReasonEnable by Id: {}", reasonEnableId);
        return reasonEnableRepository.findById(reasonEnableId).orElseThrow(
                () -> new ScosException(SCOS_REASON_ENABLE_001)
        );
    }
}
