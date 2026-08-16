
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
import br.com.sawcunhaos.organization.domain.corporate.employee.dto.RehireEmployeeInput;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.StatusEmployee;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

/**
 * Casos de uso de domínio do ciclo de vida do {@code Employee}.
 */
public interface EmployeeService {

    /** Admite um Funcionário vinculado a Empresa/Cargo ativos, copiando a Jornada de Trabalho do Cargo. */
    EmployeeOutput create(@NonNull EmployeeInput employeeInput);

    /** Ativa um Funcionário INACTIVE, gravando o motivo em SCOS_EMPLOYEE_STATUS_HISTORY (status sincronizado por trigger). */
    void activate(@NonNull Long id, @NonNull Long reasonActivateId, String observation);

    /** Inativa um Funcionário ACTIVE/DISABLED. expectedReturnDate só é aceito quando reasonInactivateId é VACATION/MEDICAL_LEAVE (422 SCOS_EMPLOYEE_024 caso contrário). */
    void inactivate(@NonNull Long id, @NonNull Long reasonInactivateId, String observation, LocalDate expectedReturnDate);

    /** Bloqueia um Funcionário ACTIVE (rota "block", corresponde ao método de domínio "disable"). */
    void disable(@NonNull Long id, @NonNull Long reasonDisableId, String observation);

    /** Desbloqueia um Funcionário DISABLED (rota "unblock", corresponde ao método de domínio "enable"). */
    void enable(@NonNull Long id, @NonNull Long reasonEnableId, String observation);

    /** Recontrata um Funcionário INACTIVE, reatribuindo empresa/cargo/supervisor/contrato. 404 SCOS_EMPLOYEE_021 se não houver Funcionário INACTIVE com esse CPF. */
    EmployeeOutput rehire(@NonNull RehireEmployeeInput input);

    /** Busca o Funcionário pelo id, para composição por outro fluxo (ex.: nome do supervisor no mapeamento de rehire). 404 SCOS_EMPLOYEE_014 se não existir. */
    EmployeeOutput findById(@NonNull Long id);

    /** Lista Funcionários paginados, filtrando por companyId/positionId/status quando informados (todos opcionais). */
    Page<EmployeeOutput> findAll(Long companyId, Long positionId, StatusEmployee status, @NonNull Pageable pageable);

}
