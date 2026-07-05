
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
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonActivateInput;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonActivateOutput;
import br.com.sawcunhaos.organization.domain.access.status.internal.EntityType;
import br.com.sawcunhaos.organization.domain.access.status.internal.ReasonActivate;
import br.com.sawcunhaos.organization.domain.access.status.internal.ReasonActivateRepository;
import br.com.sawcunhaos.organization.domain.access.status.specification.ReasonActivateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_REASON_ACTIVATE_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_REASON_ACTIVATE_002;

/**
 * Implementação de {@link ReasonActivateService} — regras de unicidade de {@code code} por catálogo
 * e idempotência de ativação/inativação ficam no agregado {@link ReasonActivate}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReasonActivateServiceBean implements ReasonActivateService {

    private final ReasonActivateRepository reasonActivateRepository;
    private final ReasonActivateMapper reasonActivateMapper;
    private final ScosUserAuthentication scosUserAuthentication;

    /**
     * @throws ScosException SCOS_REASON_ACTIVATE_002 se já existir um {@link ReasonActivate} com o mesmo {@code code}/{@code entityType}.
     */
    @Override
    public ReasonActivateOutput create(@NonNull ReasonActivateInput reasonActivateInput) {
        log.info("Create ReasonActivate: {}", reasonActivateInput.code());

        if (reasonActivateRepository.existsByCodeAndEntityType(reasonActivateInput.code(), reasonActivateInput.entityType())) {
            throw new ScosException(SCOS_REASON_ACTIVATE_002);
        }

        ReasonActivate reasonActivate = ReasonActivate.builder()
                .code(reasonActivateInput.code())
                .description(reasonActivateInput.description())
                .entityType(reasonActivateInput.entityType())
                .active(true)
                .build();
        reasonActivate.updateAuditInfo(scosUserAuthentication.findUserAuthentication());

        reasonActivate = reasonActivateRepository.merge(reasonActivate);

        return ReasonActivateOutput.builder()
                .id(reasonActivate.getId())
                .code(reasonActivate.getCode())
                .description(reasonActivate.getDescription())
                .entityType(reasonActivate.getEntityType())
                .active(reasonActivate.isActive())
                .build();
    }

    /**
     * @throws ScosException SCOS_REASON_ACTIVATE_001 se o {@code id} não existir.
     * @throws ScosException SCOS_REASON_ACTIVATE_002 se o novo {@code code}/{@code entityType} colidir com outro registro.
     */
    @Override
    public void update(@NonNull ReasonActivateInput reasonActivateInput) {
        log.info("Update ReasonActivate: {}", reasonActivateInput.id());
        ReasonActivate reasonActivate = findReasonActivateById(reasonActivateInput.id());

        if (reasonActivateRepository.existsByCodeAndEntityTypeAndNotId(reasonActivateInput.code(), reasonActivateInput.entityType(), reasonActivateInput.id())) {
            throw new ScosException(SCOS_REASON_ACTIVATE_002);
        }

        reasonActivate.setCode(reasonActivateInput.code());
        reasonActivate.setDescription(reasonActivateInput.description());
        reasonActivate.setEntityType(reasonActivateInput.entityType());
        reasonActivate.updateAuditInfo(scosUserAuthentication.findUserAuthentication());
        reasonActivateRepository.update(reasonActivate);
    }

    /**
     * @throws ScosException SCOS_REASON_ACTIVATE_001 se o {@code id} não existir.
     */
    @Override
    public ReasonActivateOutput findById(@NonNull Long reasonActivateId) {
        log.info("Find ReasonActivate by Id: {}", reasonActivateId);
        ReasonActivate reasonActivate = findReasonActivateById(reasonActivateId);
        return reasonActivateMapper.toReasonActivateOutput(reasonActivate);
    }

    /**
     * Lista paginada, filtrando por {@code entityType} quando informado.
     */
    @Override
    public Page<ReasonActivateOutput> findAll(EntityType entityType,
                                              Boolean active,
                                              @NonNull Pageable pageable
    ) {
        log.info("Find All ReasonActivate, EntityType: {}", entityType);
        return reasonActivateRepository.findAllFiltered(entityType, active, pageable)
                .map(reasonActivateMapper::toReasonActivateOutput);
    }

    /**
     * @throws ScosException SCOS_REASON_ACTIVATE_001 se o {@code id} não existir.
     * @throws ScosException SCOS_REASON_ACTIVATE_003 se já estiver ativo.
     */
    @Override
    public void enable(@NonNull Long reasonActivateId) {
        log.info("Enable ReasonActivate: {}", reasonActivateId);
        ReasonActivate reasonActivate = findReasonActivateById(reasonActivateId);
        reasonActivate.activate();

        reasonActivateRepository.update(reasonActivate);
        log.info("ReasonActivate enabled");
    }

    /**
     * @throws ScosException SCOS_REASON_ACTIVATE_001 se o {@code id} não existir.
     * @throws ScosException SCOS_REASON_ACTIVATE_004 se já estiver inativo.
     */
    @Override
    public void disable(@NonNull Long reasonActivateId) {
        log.info("Disable ReasonActivate: {}", reasonActivateId);
        ReasonActivate reasonActivate = findReasonActivateById(reasonActivateId);
        reasonActivate.deactivate();

        reasonActivateRepository.update(reasonActivate);
        log.info("ReasonActivate disabled");
    }

    private ReasonActivate findReasonActivateById(@NonNull Long reasonActivateId) {
        log.info("Find ReasonActivate by Id: {}", reasonActivateId);
        return reasonActivateRepository.findById(reasonActivateId).orElseThrow(
                () -> new ScosException(SCOS_REASON_ACTIVATE_001)
        );
    }
}
