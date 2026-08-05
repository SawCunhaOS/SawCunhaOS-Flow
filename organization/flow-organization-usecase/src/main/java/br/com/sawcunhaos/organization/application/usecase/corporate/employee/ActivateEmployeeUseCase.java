
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

package br.com.sawcunhaos.organization.application.usecase.corporate.employee;

import br.com.sawcunhaos.organization.api.dto.EmployeeStatusTransitionRequest;
import org.jspecify.annotations.NonNull;

/** Ativa um Funcionário INACTIVE (rota {@code PUT /v1/employees/{id}/enable}, UC-039). */
public interface ActivateEmployeeUseCase {
    void execute(@NonNull Long id, @NonNull EmployeeStatusTransitionRequest request);
}
