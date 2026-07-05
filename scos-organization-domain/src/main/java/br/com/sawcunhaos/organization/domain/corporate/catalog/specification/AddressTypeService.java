
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
import br.com.sawcunhaos.organization.domain.corporate.catalog.dto.AddressTypeInput;
import br.com.sawcunhaos.organization.domain.corporate.catalog.dto.AddressTypeOutput;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Casos de uso de domínio do catálogo de referência {@code AddressType}.
 */
public interface AddressTypeService {

    /** Cria um novo tipo de endereço, validando unicidade de {@code code} por {@code entityType}. */
    AddressTypeOutput create(@NonNull AddressTypeInput addressTypeInput);

    /** Atualiza {@code code}/{@code description}/{@code entityType} de um tipo de endereço existente. */
    void update(@NonNull AddressTypeInput addressTypeInput);

    /** Busca um tipo de endereço pelo id. */
    AddressTypeOutput findById(@NonNull Long addressTypeId);

    /** Lista paginada, filtrando por {@code entityType} quando informado. */
    Page<AddressTypeOutput> findAll(EntityType entityType, @NonNull Pageable pageable);

    /** Reativa um tipo de endereço inativo. */
    void enable(@NonNull Long addressTypeId);

    /** Inativa um tipo de endereço ativo — não remove vínculos existentes. */
    void disable(@NonNull Long addressTypeId);

}
