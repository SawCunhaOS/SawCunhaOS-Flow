
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

package br.com.sawcunhaos.organization.domain.access.status.specification;

import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonInactivateInput;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonInactivateOutput;
import br.com.sawcunhaos.organization.domain.access.status.internal.EntityType;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Casos de uso de domínio do catálogo de referência {@code ReasonInactivate}.
 */
public interface ReasonInactivateService {

    /** Cria um novo motivo de inativação, validando unicidade de {@code code} por {@code entityType} no catálogo. */
    ReasonInactivateOutput create(@NonNull ReasonInactivateInput reasonInactivateInput);

    /** Atualiza {@code code}/{@code description}/{@code entityType} de um motivo de inativação existente. */
    void update(@NonNull ReasonInactivateInput reasonInactivateInput);

    /** Busca um motivo de inativação pelo id. */
    ReasonInactivateOutput findById(@NonNull Long reasonInactivateId);

    /** Lista paginada, filtrando por {@code entityType} quando informado. */
    Page<ReasonInactivateOutput> findAll(EntityType entityType, Boolean active, @NonNull Pageable pageable);

    /** Reativa um motivo de inativação inativo. */
    void enable(@NonNull Long reasonInactivateId);

    /** Inativa um motivo de inativação ativo — não remove vínculos existentes em histórico. */
    void disable(@NonNull Long reasonInactivateId);

}
