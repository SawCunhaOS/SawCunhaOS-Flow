
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

package br.com.sawcunhaos.organization.application.usecase.access.status.reasoninactivate;

import br.com.sawcunhaos.organization.api.dto.ReasonEntityType;
import br.com.sawcunhaos.organization.api.dto.ReasonInactivate;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonInactivateOutput;
import br.com.sawcunhaos.organization.domain.access.status.internal.EntityType;

/**
 * Conversão entre o dto de domínio {@link ReasonInactivateOutput} e os dtos gerados do contrato OpenAPI
 * de {@code ReasonInactivate} — {@code entityType} tem os mesmos 3 valores em domínio e contrato.
 */
final class ReasonInactivateApiMapper {

    private ReasonInactivateApiMapper() {
    }

    /** Monta o {@code ReasonInactivate} do contrato a partir da saída do domínio. */
    static ReasonInactivate toApiReasonInactivate(ReasonInactivateOutput reasonInactivateOutput) {
        return ReasonInactivate.builder()
                .id(reasonInactivateOutput.id())
                .code(reasonInactivateOutput.code())
                .description(reasonInactivateOutput.description())
                .entityType(toApiEntityType(reasonInactivateOutput.entityType()))
                .active(reasonInactivateOutput.active())
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
