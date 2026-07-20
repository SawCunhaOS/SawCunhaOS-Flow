
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

import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonEnableOutput;
import br.com.sawcunhaos.organization.domain.access.status.internal.ReasonEnable;
import org.mapstruct.Mapper;

/**
 * MapStruct: {@link ReasonEnable} (entidade JPA) → {@link ReasonEnableOutput} (dto de domínio).
 */
@Mapper(componentModel = "spring")
public interface ReasonEnableMapper {

    ReasonEnableOutput toReasonEnableOutput(ReasonEnable reasonEnable);

}
