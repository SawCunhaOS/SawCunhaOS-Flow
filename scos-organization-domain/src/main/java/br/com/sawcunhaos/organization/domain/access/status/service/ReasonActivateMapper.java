
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

package br.com.sawcunhaos.organization.domain.access.status.service;

import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonActivateOutput;
import br.com.sawcunhaos.organization.domain.access.status.internal.ReasonActivate;
import org.mapstruct.Mapper;

/**
 * MapStruct: {@link ReasonActivate} (entidade JPA) → {@link ReasonActivateOutput} (dto de domínio).
 */
@Mapper(componentModel = "spring")
public interface ReasonActivateMapper {

    ReasonActivateOutput toReasonActivateOutput(ReasonActivate reasonActivate);

}
