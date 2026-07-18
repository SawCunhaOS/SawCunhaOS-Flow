
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

import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonDisableInput;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonDisableOutput;
import br.com.sawcunhaos.organization.domain.access.status.internal.EntityType;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Casos de uso de domínio do catálogo de referência {@code ReasonDisable}.
 */
public interface ReasonDisableService {

    /** Cria um novo motivo de bloqueio, validando unicidade de {@code code} por {@code entityType} no catálogo. */
    ReasonDisableOutput create(@NonNull ReasonDisableInput reasonDisableInput);

    /** Atualiza {@code code}/{@code description}/{@code entityType} de um motivo de bloqueio existente. */
    void update(@NonNull ReasonDisableInput reasonDisableInput);

    /** Busca um motivo de bloqueio pelo id. */
    ReasonDisableOutput findById(@NonNull Long reasonDisableId);

    /** Lista paginada, filtrando por {@code entityType} quando informado. */
    Page<ReasonDisableOutput> findAll(EntityType entityType, Boolean active, @NonNull Pageable pageable);

    /** Reativa um motivo de bloqueio inativo. */
    void enable(@NonNull Long reasonDisableId);

    /** Inativa um motivo de bloqueio ativo — não remove vínculos existentes em histórico. */
    void disable(@NonNull Long reasonDisableId);

}
