
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

import br.com.sawcunhaos.organization.api.dto.Department;
import br.com.sawcunhaos.organization.api.dto.Employee;
import br.com.sawcunhaos.organization.api.dto.EmployeeCompany;
import br.com.sawcunhaos.organization.api.dto.EmployeeContractType;
import br.com.sawcunhaos.organization.api.dto.EmployeeStatus;
import br.com.sawcunhaos.organization.api.dto.Position;
import br.com.sawcunhaos.organization.api.dto.Supervisor;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.CompanyOutput;
import br.com.sawcunhaos.organization.domain.corporate.department.dto.DepartmentOutput;
import br.com.sawcunhaos.organization.domain.corporate.employee.dto.EmployeeOutput;
import br.com.sawcunhaos.organization.domain.corporate.position.dto.PositionOutput;

final class EmployeeApiMapper {

    private EmployeeApiMapper() {
    }

    static Employee toApiEmployee(EmployeeOutput employee, CompanyOutput company, PositionOutput position, EmployeeOutput supervisor) {
        return Employee.builder()
                .id(employee.id())
                .name(employee.name())
                .nameTreatment(employee.nameTreatment())
                .taxIdentifier(employee.taxIdentifier())
                .email(employee.email())
                .birthDate(employee.birthDate())
                .dateOfHiring(employee.dateOfHiring())
                .observation(employee.observation())
                .status(EmployeeStatus.valueOf(employee.status().name()))
                .contractType(EmployeeContractType.valueOf(employee.contractType().name()))
                .probationEndDate(employee.probationEndDate())
                .supervisor(toApiSupervisor(supervisor))
                .company(toApiEmployeeCompany(company))
                .position(toApiPosition(position))
                .build();
    }

    private static Supervisor toApiSupervisor(EmployeeOutput supervisor) {
        if (supervisor == null) {
            return null;
        }
        return Supervisor.builder().id(supervisor.id()).name(supervisor.name()).build();
    }

    private static EmployeeCompany toApiEmployeeCompany(CompanyOutput company) {
        return EmployeeCompany.builder().id(company.id()).name(company.name()).build();
    }

    private static Position toApiPosition(PositionOutput position) {
        return Position.builder()
                .id(position.id())
                .code(position.code())
                .description(position.description())
                .active(position.active())
                .isTrustPosition(position.isTrustPosition())
                .department(toApiDepartment(position.department()))
                .build();
    }

    private static Department toApiDepartment(DepartmentOutput department) {
        if (department == null) {
            return null;
        }
        return Department.builder()
                .id(department.id())
                .code(department.code())
                .description(department.description())
                .active(department.active())
                .build();
    }
}
