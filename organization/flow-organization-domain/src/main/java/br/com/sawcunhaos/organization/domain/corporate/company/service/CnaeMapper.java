
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

package br.com.sawcunhaos.organization.domain.corporate.company.service;

import br.com.sawcunhaos.organization.domain.corporate.company.dto.CnaeOutput;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.Cnae;
import org.mapstruct.Mapper;

/**
 * MapStruct: {@link Cnae} (entidade JPA) → {@link CnaeOutput} (dto de domínio).
 */
@Mapper(componentModel = "spring")
public interface CnaeMapper {

    CnaeOutput toCnaeOutput(Cnae cnae);

}
