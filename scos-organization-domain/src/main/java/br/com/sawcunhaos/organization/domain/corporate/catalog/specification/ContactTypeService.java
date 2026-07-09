
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

package br.com.sawcunhaos.organization.domain.corporate.catalog.specification;

import br.com.sawcunhaos.organization.domain.access.status.internal.EntityType;
import br.com.sawcunhaos.organization.domain.corporate.catalog.dto.ContactTypeInput;
import br.com.sawcunhaos.organization.domain.corporate.catalog.dto.ContactTypeOutput;
import br.com.sawcunhaos.organization.domain.corporate.catalog.internal.ContactType;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Casos de uso de domínio do catálogo de referência {@code ContactType}.
 */
public interface ContactTypeService {

    /** Cria um novo tipo de contato, validando unicidade de {@code code} por {@code entityType}. */
    ContactTypeOutput create(@NonNull ContactTypeInput contactTypeInput);

    /** Atualiza {@code code}/{@code description}/{@code entityType} de um tipo de contato existente. */
    void update(@NonNull ContactTypeInput contactTypeInput);

    /** Busca um tipo de contato pelo id. */
    ContactTypeOutput findById(@NonNull Long contactTypeId);

    /** Lista paginada, filtrando por {@code entityType} quando informado. */
    Page<ContactTypeOutput> findAll(EntityType entityType, Boolean active, @NonNull Pageable pageable);

    /** Reativa um tipo de contato inativo. */
    void enable(@NonNull Long contactTypeId);

    /** Inativa um tipo de contato ativo — não remove vínculos existentes. */
    void disable(@NonNull Long contactTypeId);

    ContactType findContactTypeById(@NonNull Long contactTypeId);

}
