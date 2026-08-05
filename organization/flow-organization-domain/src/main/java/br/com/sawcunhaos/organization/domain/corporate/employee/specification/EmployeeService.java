
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

package br.com.sawcunhaos.organization.domain.corporate.employee.specification;

import br.com.sawcunhaos.organization.domain.corporate.employee.dto.EmployeeInput;
import br.com.sawcunhaos.organization.domain.corporate.employee.dto.EmployeeOutput;
import org.jspecify.annotations.NonNull;

/**
 * Casos de uso de domínio do ciclo de vida do {@code Employee}.
 */
public interface EmployeeService {

    /** Admite um Funcionário vinculado a Empresa/Cargo ativos, copiando a Jornada de Trabalho do Cargo. */
    EmployeeOutput create(@NonNull EmployeeInput employeeInput);

}
