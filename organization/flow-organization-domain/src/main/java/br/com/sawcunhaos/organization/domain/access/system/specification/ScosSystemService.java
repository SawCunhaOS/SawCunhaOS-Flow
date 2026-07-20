
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

package br.com.sawcunhaos.organization.domain.access.system.specification;

import br.com.sawcunhaos.organization.domain.access.system.dto.RegisterScosSystemInput;
import br.com.sawcunhaos.organization.domain.access.system.dto.ScosSystemOutput;
import org.jspecify.annotations.NonNull;

public interface ScosSystemService {

    ScosSystemOutput register(@NonNull RegisterScosSystemInput registerScosSystemInput);

    ScosSystemOutput findByCode(@NonNull String code);
    void validateSecretKey(@NonNull String code, @NonNull String secretKey);
}
