
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

package br.com.sawcunhaos.organization.application.usecase.access.resource.registry;

import java.util.List;

/**
 * Registro em lote de resources — a implementação cobre a iteração inteira com uma única
 * transação, de modo que a falha em qualquer resource faz rollback de todos (batch atômico).
 */
public interface RegistryResourcesUseCase {
    void execute(List<RegistryResourceInput> requests);
}
