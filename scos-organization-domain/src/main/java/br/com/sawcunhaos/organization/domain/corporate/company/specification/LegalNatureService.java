
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

import br.com.sawcunhaos.organization.domain.corporate.company.dto.LegalNatureInput;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.LegalNatureOutput;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Casos de uso de domínio do catálogo fiscal {@code LegalNature}.
 */
public interface LegalNatureService {

    /** Cria uma nova natureza jurídica, validando unicidade de {@code code}. */
    LegalNatureOutput create(@NonNull LegalNatureInput legalNatureInput);

    /** Atualiza {@code code}/{@code description} de uma natureza jurídica existente. */
    void update(@NonNull LegalNatureInput legalNatureInput);

    /** Busca uma natureza jurídica pelo id. */
    LegalNatureOutput findById(@NonNull Long legalNatureId);

    /** Lista paginada de naturezas jurídicas. */
    Page<LegalNatureOutput> findAll(@NonNull Pageable pageable);

    /** Remove fisicamente uma natureza jurídica — rejeita se ainda vinculada a alguma empresa. */
    void delete(@NonNull Long legalNatureId);

}
