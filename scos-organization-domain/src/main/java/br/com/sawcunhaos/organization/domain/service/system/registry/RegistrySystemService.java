
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

package br.com.sawcunhaos.organization.domain.service.system.registry;

import br.com.sawcunhaos.organization.domain.model.system.ScosSystem;
import org.jspecify.annotations.NonNull;

public interface RegistrySystemService {

    ScosSystem register(@NonNull String name, @NonNull String code, @NonNull String description);

}
