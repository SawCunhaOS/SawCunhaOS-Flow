
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
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonInactivateInput;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonInactivateOutput;
import br.com.sawcunhaos.organization.domain.access.status.internal.EntityType;
import br.com.sawcunhaos.organization.domain.access.status.internal.ReasonInactivate;
import br.com.sawcunhaos.organization.domain.access.status.internal.ReasonInactivateRepository;
import br.com.sawcunhaos.organization.domain.access.status.specification.ReasonInactivateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_REASON_INACTIVATE_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_REASON_INACTIVATE_002;

/**
 * Implementação de {@link ReasonInactivateService} — regras de unicidade de {@code code} por catálogo
 * e idempotência de ativação/inativação ficam no agregado {@link ReasonInactivate}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReasonInactivateServiceBean implements ReasonInactivateService {

    private final ReasonInactivateRepository reasonInactivateRepository;
    private final ReasonInactivateMapper reasonInactivateMapper;
    private final ScosUserAuthentication scosUserAuthentication;

    /**
     * @throws ScosException SCOS_REASON_INACTIVATE_002 se já existir um {@link ReasonInactivate} com o mesmo {@code code}/{@code entityType}.
     */
    @Override
    public ReasonInactivateOutput create(@NonNull ReasonInactivateInput reasonInactivateInput) {
        log.info("Create ReasonInactivate: {}", reasonInactivateInput.code());

        if (reasonInactivateRepository.existsByCodeAndEntityType(reasonInactivateInput.code(), reasonInactivateInput.entityType())) {
            throw new ScosException(SCOS_REASON_INACTIVATE_002);
        }

        ReasonInactivate reasonInactivate = ReasonInactivate.builder()
                .code(reasonInactivateInput.code())
                .description(reasonInactivateInput.description())
                .entityType(reasonInactivateInput.entityType())
                .active(true)
                .build();
        reasonInactivate.updateAuditInfo(scosUserAuthentication.findUserAuthentication());

        reasonInactivate = reasonInactivateRepository.merge(reasonInactivate);

        return ReasonInactivateOutput.builder()
                .id(reasonInactivate.getId())
                .code(reasonInactivate.getCode())
                .description(reasonInactivate.getDescription())
                .entityType(reasonInactivate.getEntityType())
                .active(reasonInactivate.isActive())
                .build();
    }

    /**
     * @throws ScosException SCOS_REASON_INACTIVATE_001 se o {@code id} não existir.
     * @throws ScosException SCOS_REASON_INACTIVATE_002 se o novo {@code code}/{@code entityType} colidir com outro registro.
     */
    @Override
    public void update(@NonNull ReasonInactivateInput reasonInactivateInput) {
        log.info("Update ReasonInactivate: {}", reasonInactivateInput.id());
        ReasonInactivate reasonInactivate = findReasonInactivateById(reasonInactivateInput.id());

        if (reasonInactivateRepository.existsByCodeAndEntityTypeAndNotId(reasonInactivateInput.code(), reasonInactivateInput.entityType(), reasonInactivateInput.id())) {
            throw new ScosException(SCOS_REASON_INACTIVATE_002);
        }

        reasonInactivate.setCode(reasonInactivateInput.code());
        reasonInactivate.setDescription(reasonInactivateInput.description());
        reasonInactivate.setEntityType(reasonInactivateInput.entityType());
        reasonInactivate.updateAuditInfo(scosUserAuthentication.findUserAuthentication());
        reasonInactivateRepository.update(reasonInactivate);
    }

    /**
     * @throws ScosException SCOS_REASON_INACTIVATE_001 se o {@code id} não existir.
     */
    @Override
    public ReasonInactivateOutput findById(@NonNull Long reasonInactivateId) {
        log.info("Find ReasonInactivate by Id: {}", reasonInactivateId);
        ReasonInactivate reasonInactivate = findReasonInactivateById(reasonInactivateId);
        return reasonInactivateMapper.toReasonInactivateOutput(reasonInactivate);
    }

    /**
     * Lista paginada, filtrando por {@code entityType} quando informado.
     */
    @Override
    public Page<ReasonInactivateOutput> findAll(EntityType entityType,
                                                Boolean active,
                                                @NonNull Pageable pageable
    ) {
        log.info("Find All ReasonInactivate, EntityType: {}", entityType);
        return reasonInactivateRepository.findAllFiltered(entityType, active, pageable)
                .map(reasonInactivateMapper::toReasonInactivateOutput);
    }

    /**
     * @throws ScosException SCOS_REASON_INACTIVATE_001 se o {@code id} não existir.
     * @throws ScosException SCOS_REASON_INACTIVATE_003 se já estiver ativo.
     */
    @Override
    public void enable(@NonNull Long reasonInactivateId) {
        log.info("Enable ReasonInactivate: {}", reasonInactivateId);
        ReasonInactivate reasonInactivate = findReasonInactivateById(reasonInactivateId);
        reasonInactivate.activate();

        reasonInactivateRepository.update(reasonInactivate);
        log.info("ReasonInactivate enabled");
    }

    /**
     * @throws ScosException SCOS_REASON_INACTIVATE_001 se o {@code id} não existir.
     * @throws ScosException SCOS_REASON_INACTIVATE_004 se já estiver inativo.
     */
    @Override
    public void disable(@NonNull Long reasonInactivateId) {
        log.info("Disable ReasonInactivate: {}", reasonInactivateId);
        ReasonInactivate reasonInactivate = findReasonInactivateById(reasonInactivateId);
        reasonInactivate.deactivate();

        reasonInactivateRepository.update(reasonInactivate);
        log.info("ReasonInactivate disabled");
    }

    private ReasonInactivate findReasonInactivateById(@NonNull Long reasonInactivateId) {
        log.info("Find ReasonInactivate by Id: {}", reasonInactivateId);
        return reasonInactivateRepository.findById(reasonInactivateId).orElseThrow(
                () -> new ScosException(SCOS_REASON_INACTIVATE_001)
        );
    }
}
