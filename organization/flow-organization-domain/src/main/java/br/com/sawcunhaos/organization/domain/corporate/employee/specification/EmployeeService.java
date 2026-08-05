
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

    /** Ativa um Funcionário INACTIVE, gravando o motivo em SCOS_EMPLOYEE_STATUS_HISTORY (status sincronizado por trigger). */
    void activate(@NonNull Long id, @NonNull Long reasonActivateId, String observation);

    /** Inativa um Funcionário ACTIVE/DISABLED, gravando o motivo (status sincronizado por trigger). */
    void inactivate(@NonNull Long id, @NonNull Long reasonInactivateId, String observation);

    /** Bloqueia um Funcionário ACTIVE (rota "block", corresponde ao método de domínio "disable"). */
    void disable(@NonNull Long id, @NonNull Long reasonDisableId, String observation);

    /** Desbloqueia um Funcionário DISABLED (rota "unblock", corresponde ao método de domínio "enable"). */
    void enable(@NonNull Long id, @NonNull Long reasonEnableId, String observation);

}
