
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

import br.com.sawcunhaos.organization.api.dto.Employee;
import br.com.sawcunhaos.organization.api.dto.RehireEmployeeRequest;
import org.jspecify.annotations.NonNull;

/** Recontrata um Funcionário INACTIVE, reatribuindo empresa/cargo/supervisor/contrato (rota {@code POST /v1/employees/rehire}). */
public interface RehireEmployeeUseCase {
    Employee execute(@NonNull RehireEmployeeRequest request);
}
