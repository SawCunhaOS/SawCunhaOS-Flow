
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

package br.com.sawcunhaos.organization.domain.corporate.catalog.service;

import br.com.sawcunhaos.organization.domain.corporate.catalog.dto.AddressTypeOutput;
import br.com.sawcunhaos.organization.domain.corporate.catalog.internal.AddressType;
import org.mapstruct.Mapper;

/**
 * MapStruct: {@link AddressType} (entidade JPA) → {@link AddressTypeOutput} (dto de domínio).
 */
@Mapper(componentModel = "spring")
public interface AddressTypeMapper {

    AddressTypeOutput toAddressTypeOutput(AddressType addressType);

}
