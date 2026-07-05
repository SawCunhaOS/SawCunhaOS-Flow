
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

package br.com.sawcunhaos.organization.application.usecase.access.status.reasonactivate;

import br.com.sawcunhaos.organization.api.dto.ReasonActivate;
import br.com.sawcunhaos.organization.api.dto.ReasonEntityType;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonActivateOutput;
import br.com.sawcunhaos.organization.domain.access.status.internal.EntityType;

/**
 * Conversão entre o dto de domínio {@link ReasonActivateOutput} e os dtos gerados do contrato OpenAPI
 * de {@code ReasonActivate} — {@code entityType} tem os mesmos 3 valores em domínio e contrato.
 */
final class ReasonActivateApiMapper {

    private ReasonActivateApiMapper() {
    }

    /** Monta o {@code ReasonActivate} do contrato a partir da saída do domínio. */
    static ReasonActivate toApiReasonActivate(ReasonActivateOutput reasonActivateOutput) {
        return ReasonActivate.builder()
                .id(reasonActivateOutput.id())
                .code(reasonActivateOutput.code())
                .description(reasonActivateOutput.description())
                .entityType(toApiEntityType(reasonActivateOutput.entityType()))
                .active(reasonActivateOutput.active())
                .build();
    }

    /** {@link EntityType} de domínio → {@link ReasonEntityType} do contrato. */
    static ReasonEntityType toApiEntityType(EntityType entityType) {
        if (entityType == null) {
            return null;
        }
        return ReasonEntityType.valueOf(entityType.name());
    }

    /** {@link ReasonEntityType} do contrato → {@link EntityType} de domínio. */
    static EntityType toDomainEntityType(ReasonEntityType reasonEntityType) {
        if (reasonEntityType == null) {
            return null;
        }
        return EntityType.valueOf(reasonEntityType.name());
    }
}
