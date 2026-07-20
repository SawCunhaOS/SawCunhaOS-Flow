
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

import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonActivateInput;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonActivateOutput;
import br.com.sawcunhaos.organization.domain.access.status.internal.EntityType;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Casos de uso de domínio do catálogo de referência {@code ReasonActivate}.
 */
public interface ReasonActivateService {

    /** Cria um novo motivo de ativação, validando unicidade de {@code code} por {@code entityType} no catálogo. */
    ReasonActivateOutput create(@NonNull ReasonActivateInput reasonActivateInput);

    /** Atualiza {@code code}/{@code description}/{@code entityType} de um motivo de ativação existente. */
    void update(@NonNull ReasonActivateInput reasonActivateInput);

    /** Busca um motivo de ativação pelo id. */
    ReasonActivateOutput findById(@NonNull Long reasonActivateId);

    /** Lista paginada, filtrando por {@code entityType} quando informado. */
    Page<ReasonActivateOutput> findAll(EntityType entityType, Boolean active, @NonNull Pageable pageable);

    /** Reativa um motivo de ativação inativo. */
    void enable(@NonNull Long reasonActivateId);

    /** Inativa um motivo de ativação ativo — não remove vínculos existentes em histórico. */
    void disable(@NonNull Long reasonActivateId);

}
