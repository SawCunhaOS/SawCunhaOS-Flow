
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

import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonEnableInput;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonEnableOutput;
import br.com.sawcunhaos.organization.domain.access.status.internal.EntityType;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Casos de uso de domínio do catálogo de referência {@code ReasonEnable}.
 */
public interface ReasonEnableService {

    /** Cria um novo motivo de desbloqueio, validando unicidade de {@code code} por {@code entityType} no catálogo. */
    ReasonEnableOutput create(@NonNull ReasonEnableInput reasonEnableInput);

    /** Atualiza {@code code}/{@code description}/{@code entityType} de um motivo de desbloqueio existente. */
    void update(@NonNull ReasonEnableInput reasonEnableInput);

    /** Busca um motivo de desbloqueio pelo id. */
    ReasonEnableOutput findById(@NonNull Long reasonEnableId);

    /** Lista paginada, filtrando por {@code entityType} quando informado. */
    Page<ReasonEnableOutput> findAll(EntityType entityType, Boolean active, @NonNull Pageable pageable);

    /** Reativa um motivo de desbloqueio inativo. */
    void enable(@NonNull Long reasonEnableId);

    /** Inativa um motivo de desbloqueio ativo — não remove vínculos existentes em histórico. */
    void disable(@NonNull Long reasonEnableId);

}
