
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

package br.com.sawcunhaos.organization.domain.corporate.company.specification;

import br.com.sawcunhaos.organization.domain.corporate.company.dto.CnaeInput;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.CnaeOutput;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.Cnae;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Casos de uso de domínio do catálogo fiscal {@code Cnae}.
 */
public interface CnaeService {

    /** Cria um novo CNAE, validando unicidade de {@code code}. */
    CnaeOutput create(@NonNull CnaeInput cnaeInput);

    /** Atualiza {@code code}/{@code description} de um CNAE existente. */
    void update(@NonNull CnaeInput cnaeInput);

    /** Busca um CNAE pelo id. */
    CnaeOutput findById(@NonNull Long cnaeId);

    /** Lista paginada de CNAEs. */
    Page<CnaeOutput> findAll(@NonNull Pageable pageable);

    /** Remove fisicamente um CNAE — rejeita se ainda vinculado a alguma empresa (principal ou secundário). */
    void delete(@NonNull Long cnaeId);

    Cnae findCnaeById(@NonNull Long cnaeId);

}
