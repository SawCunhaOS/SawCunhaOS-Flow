
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

package br.com.sawcunhaos.organization.domain.corporate.employee.service;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.foundation.utils.specification.ScosUserAuthentication;
import br.com.sawcunhaos.foundation.utils.valueobjects.Cpf;
import br.com.sawcunhaos.foundation.utils.valueobjects.Email;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonActivateOutput;
import br.com.sawcunhaos.organization.domain.access.status.internal.EntityType;
import br.com.sawcunhaos.organization.domain.access.status.internal.EmployeeStatusHistory;
import br.com.sawcunhaos.organization.domain.access.status.internal.EmployeeStatusHistoryRepository;
import br.com.sawcunhaos.organization.domain.access.status.internal.ReasonActivate;
import br.com.sawcunhaos.organization.domain.access.status.specification.ReasonActivateService;
import br.com.sawcunhaos.organization.domain.configuration.internal.ConfigurationKey;
import br.com.sawcunhaos.organization.domain.configuration.internal.OrganizationConfigurationRepository;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.Company;
import br.com.sawcunhaos.organization.domain.corporate.company.specification.CompanyService;
import br.com.sawcunhaos.organization.domain.corporate.employee.dto.EmployeeInput;
import br.com.sawcunhaos.organization.domain.corporate.employee.dto.EmployeeOutput;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.Employee;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.EmployeePositionHistory;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.EmployeePositionHistoryRepository;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.EmployeeQueryRepository;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.EmployeeWorkSchedule;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.EmployeeWorkScheduleRepository;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.ReasonPositionChange;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.ReasonPositionChangeRepository;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.StatusEmployee;
import br.com.sawcunhaos.organization.domain.corporate.employee.specification.EmployeeService;
import br.com.sawcunhaos.organization.domain.corporate.position.internal.Position;
import br.com.sawcunhaos.organization.domain.corporate.position.internal.PositionWorkScheduleRepository;
import br.com.sawcunhaos.organization.domain.corporate.position.specification.PositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_CONFIGURATION_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_EMPLOYEE_002;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_EMPLOYEE_003;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_EMPLOYEE_004;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_EMPLOYEE_005;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_EMPLOYEE_006;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_EMPLOYEE_007;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_EMPLOYEE_008;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_EMPLOYEE_009;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_EMPLOYEE_010;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_EMPLOYEE_011;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_EMPLOYEE_012;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_EMPLOYEE_013;

/**
 * Implementação de {@link EmployeeService} — regras de negócio da admissão do Funcionário que
 * dependem do banco ou de outros agregados. Ordem de erro: unicidade (409) → FK (404) → regra (422).
 */
@Service
@RequiredArgsConstructor
@Slf4j
class EmployeeServiceBean implements EmployeeService {

    private static final String NEW_HIRE_REASON_CODE = "NEW_HIRE";

    private final EmployeeQueryRepository employeeQueryRepository;
    private final EmployeeStatusHistoryRepository employeeStatusHistoryRepository;
    private final EmployeePositionHistoryRepository employeePositionHistoryRepository;
    private final EmployeeWorkScheduleRepository employeeWorkScheduleRepository;
    private final ReasonPositionChangeRepository reasonPositionChangeRepository;
    private final PositionWorkScheduleRepository positionWorkScheduleRepository;
    private final PositionService positionService;
    private final CompanyService companyService;
    private final ReasonActivateService reasonActivateService;
    private final OrganizationConfigurationRepository organizationConfigurationRepository;
    private final Clock clock;
    private final ScosUserAuthentication scosUserAuthentication;

    /**
     * @throws ScosException SCOS_EMPLOYEE_002/003 (409) se {@code taxIdentifier}/{@code email} já existirem.
     * @throws ScosException SCOS_COMPANY_001 / SCOS_POSITION_001 / SCOS_REASON_ACTIVATE_001 / SCOS_EMPLOYEE_004 (404) se alguma FK não existir.
     * @throws ScosException SCOS_EMPLOYEE_005/006/007/008/009/010/011/012/013 (422) para as regras de estado/idade/domínio/data.
     */
    @Override
    @Transactional(rollbackFor = ScosException.class)
    public EmployeeOutput create(@NonNull EmployeeInput input) {
        log.info("Create Employee: {}", input.taxIdentifier());

        if (employeeQueryRepository.existsByTaxIdentifier(input.taxIdentifier())) {
            throw new ScosException(SCOS_EMPLOYEE_002);
        }
        if (employeeQueryRepository.existsByEmail(input.email())) {
            throw new ScosException(SCOS_EMPLOYEE_003);
        }

        Company company = findActiveCompanyOrThrow(input.companyId());
        Position position = findActivePositionOrThrow(input.positionId());
        Employee supervisor = resolveActiveSupervisor(input.supervisorId());
        validateReasonActivate(input.reasonActivateId());
        assertEmailDomain(input.email());
        assertBirthDateAndHiringDate(input.birthDate(), input.dateOfHiring());
        assertMinimumAge(input.birthDate(), input.dateOfHiring());

        String user = scosUserAuthentication.findUserAuthentication();

        Employee employee = Employee.builder()
                .name(input.name())
                .nameTreatment(input.nameTreatment())
                .taxIdentifier(new Cpf(input.taxIdentifier()))
                .email(new Email(input.email()))
                .birthDate(input.birthDate())
                .observation(input.observation())
                .dateOfHiring(input.dateOfHiring())
                .contractType(input.contractType())
                .probationEndDate(input.probationEndDate())
                .status(StatusEmployee.ACTIVE)
                .supervisor(supervisor)
                .position(position)
                .company(company)
                .build();
        employee.updateAuditInfo(user);
        employee = employeeQueryRepository.merge(employee);

        employeeStatusHistoryRepository.merge(
                EmployeeStatusHistory.builder()
                        .employee(employee)
                        .status(StatusEmployee.ACTIVE)
                        .reasonActivate(ReasonActivate.builder().id(input.reasonActivateId()).build())
                        .userAt(user)
                        .build()
        );

        employeePositionHistoryRepository.merge(
                EmployeePositionHistory.builder()
                        .employee(employee)
                        .position(position)
                        .startDate(input.dateOfHiring())
                        .reasonPositionChange(newHireReason())
                        .userAt(user)
                        .build()
        );

        copyWorkScheduleFromPosition(employee, position.getId(), user);

        return toEmployeeOutput(employee);
    }

    private void copyWorkScheduleFromPosition(Employee employee, Long positionId, String user) {
        positionWorkScheduleRepository.findAllByPositionId(positionId).forEach(template -> {
            EmployeeWorkSchedule workSchedule = EmployeeWorkSchedule.builder()
                    .employee(employee)
                    .dayOfWeek(template.getDayOfWeek())
                    .startTime(template.getStartTime())
                    .lunchStart(template.getLunchStart())
                    .lunchEnd(template.getLunchEnd())
                    .endTime(template.getEndTime())
                    .build();
            workSchedule.updateAuditInfo(user);
            employeeWorkScheduleRepository.merge(workSchedule);
        });
    }

    private ReasonPositionChange newHireReason() {
        return reasonPositionChangeRepository.findByCode(NEW_HIRE_REASON_CODE)
                .orElseThrow(() -> new ScosException(SCOS_CONFIGURATION_001));
    }

    private Company findActiveCompanyOrThrow(Long companyId) {
        Company company = companyService.findCompanyById(companyId);
        if (!company.isActive()) {
            throw new ScosException(SCOS_EMPLOYEE_005);
        }
        return company;
    }

    private Position findActivePositionOrThrow(Long positionId) {
        Position position = positionService.findPositionById(positionId);
        if (!position.isActive()) {
            throw new ScosException(SCOS_EMPLOYEE_006);
        }
        return position;
    }

    private Employee resolveActiveSupervisor(Long supervisorId) {
        if (supervisorId == null) {
            return null;
        }
        Employee supervisor = employeeQueryRepository.findById(supervisorId)
                .orElseThrow(() -> new ScosException(SCOS_EMPLOYEE_004));
        if (supervisor.getStatus() != StatusEmployee.ACTIVE) {
            throw new ScosException(SCOS_EMPLOYEE_007);
        }
        return supervisor;
    }

    private void validateReasonActivate(Long reasonActivateId) {
        ReasonActivateOutput reasonActivate = reasonActivateService.findById(reasonActivateId);
        if (!reasonActivate.active()) {
            throw new ScosException(SCOS_EMPLOYEE_008);
        }
        if (reasonActivate.entityType() != EntityType.EMPLOYEE) {
            throw new ScosException(SCOS_EMPLOYEE_009);
        }
    }

    private void assertEmailDomain(String email) {
        String domain = employeeEmailDomain();
        if (!email.endsWith("@" + domain)) {
            throw new ScosException(SCOS_EMPLOYEE_010);
        }
    }

    private void assertBirthDateAndHiringDate(LocalDate birthDate, LocalDate dateOfHiring) {
        if (birthDate.isAfter(LocalDate.now(clock))) {
            throw new ScosException(SCOS_EMPLOYEE_012);
        }
        if (dateOfHiring.isBefore(birthDate)) {
            throw new ScosException(SCOS_EMPLOYEE_013);
        }
    }

    private void assertMinimumAge(LocalDate birthDate, LocalDate dateOfHiring) {
        int age = Period.between(birthDate, dateOfHiring).getYears();
        if (age < minimumEmployeeAge()) {
            throw new ScosException(SCOS_EMPLOYEE_011);
        }
    }

    private int minimumEmployeeAge() {
        return Integer.parseInt(
                organizationConfigurationRepository.findById(ConfigurationKey.EMPLOYEE_MIN_AGE)
                        .orElseThrow(() -> new ScosException(SCOS_CONFIGURATION_001))
                        .getValue()
        );
    }

    private String employeeEmailDomain() {
        return organizationConfigurationRepository.findById(ConfigurationKey.EMPLOYEE_EMAIL_DOMAIN)
                .orElseThrow(() -> new ScosException(SCOS_CONFIGURATION_001))
                .getValue();
    }

    private EmployeeOutput toEmployeeOutput(Employee employee) {
        return EmployeeOutput.builder()
                .id(employee.getId())
                .name(employee.getName())
                .nameTreatment(employee.getNameTreatment())
                .taxIdentifier(employee.getTaxIdentifier().getCpf())
                .email(employee.getEmail().getEmail())
                .birthDate(employee.getBirthDate())
                .observation(employee.getObservation())
                .dateOfHiring(employee.getDateOfHiring())
                .contractType(employee.getContractType())
                .probationEndDate(employee.getProbationEndDate())
                .status(employee.getStatus())
                .supervisorId(employee.getSupervisor() != null ? employee.getSupervisor().getId() : null)
                .companyId(employee.getCompany().getId())
                .positionId(employee.getPosition().getId())
                .build();
    }
}
