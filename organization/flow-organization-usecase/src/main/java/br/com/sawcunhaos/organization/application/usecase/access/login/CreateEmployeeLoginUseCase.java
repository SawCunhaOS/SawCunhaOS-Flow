
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

package br.com.sawcunhaos.organization.application.usecase.access.login;

import br.com.sawcunhaos.organization.api.dto.CreateEmployeeLoginRequest;
import br.com.sawcunhaos.organization.api.dto.Login;
import org.jspecify.annotations.NonNull;

/** Cria um Login EMPLOYEE vinculado a um Funcionário ACTIVE, em PENDING_APPROVAL (UC-054). */
public interface CreateEmployeeLoginUseCase {
    Login execute(@NonNull Long employeeId, @NonNull CreateEmployeeLoginRequest createEmployeeLoginRequest);
}
