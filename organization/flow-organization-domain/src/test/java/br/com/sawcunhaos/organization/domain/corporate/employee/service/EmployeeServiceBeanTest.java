
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
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonDisableOutput;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonEnableOutput;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonInactivateOutput;
import br.com.sawcunhaos.organization.domain.access.status.internal.EmployeeStatusHistory;
import br.com.sawcunhaos.organization.domain.access.status.internal.EmployeeStatusHistoryRepository;
import br.com.sawcunhaos.organization.domain.access.status.internal.EntityType;
import br.com.sawcunhaos.organization.domain.access.status.specification.ReasonActivateService;
import br.com.sawcunhaos.organization.domain.access.status.specification.ReasonDisableService;
import br.com.sawcunhaos.organization.domain.access.status.specification.ReasonEnableService;
import br.com.sawcunhaos.organization.domain.access.status.specification.ReasonInactivateService;
import br.com.sawcunhaos.organization.domain.configuration.internal.ConfigurationKey;
import br.com.sawcunhaos.organization.domain.configuration.internal.OrganizationConfiguration;
import br.com.sawcunhaos.organization.domain.configuration.internal.OrganizationConfigurationRepository;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.Company;
import br.com.sawcunhaos.organization.domain.corporate.company.specification.CompanyService;
import br.com.sawcunhaos.organization.domain.corporate.employee.dto.EmployeeInput;
import br.com.sawcunhaos.organization.domain.corporate.employee.dto.EmployeeOutput;
import br.com.sawcunhaos.organization.domain.corporate.employee.dto.RehireEmployeeInput;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.Employee;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.EmployeeContractType;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.EmployeePositionHistory;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.EmployeePositionHistoryRepository;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.EmployeeQueryRepository;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.EmployeeWorkSchedule;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.EmployeeWorkScheduleRepository;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.ReasonPositionChange;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.ReasonPositionChangeRepository;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.StatusEmployee;
import br.com.sawcunhaos.organization.domain.corporate.position.internal.DayOfWeek;
import br.com.sawcunhaos.organization.domain.corporate.position.internal.Position;
import br.com.sawcunhaos.organization.domain.corporate.position.internal.PositionWorkSchedule;
import br.com.sawcunhaos.organization.domain.corporate.position.internal.PositionWorkScheduleRepository;
import br.com.sawcunhaos.organization.domain.corporate.position.specification.PositionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes de {@link EmployeeServiceBean#create}: unicidade de CPF/e-mail, integridade de FKs,
 * compatibilidade do motivo de ativação, domínio de e-mail, idade mínima e datas. Ordem de erro
 * documentada é {@code 409 → 404 → 422}.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeServiceBeanTest {

    private static final String VALID_CPF = "11144477735";
    private static final String VALID_EMAIL = "novo.funcionario@sawcunhaos.com.br";
    private static final String EMAIL_DOMAIN = "sawcunhaos.com.br";

    @Mock
    private EmployeeQueryRepository employeeQueryRepository;
    @Mock
    private EmployeeStatusHistoryRepository employeeStatusHistoryRepository;
    @Mock
    private EmployeePositionHistoryRepository employeePositionHistoryRepository;
    @Mock
    private EmployeeWorkScheduleRepository employeeWorkScheduleRepository;
    @Mock
    private ReasonPositionChangeRepository reasonPositionChangeRepository;
    @Mock
    private PositionWorkScheduleRepository positionWorkScheduleRepository;
    @Mock
    private PositionService positionService;
    @Mock
    private CompanyService companyService;
    @Mock
    private ReasonActivateService reasonActivateService;
    @Mock
    private ReasonInactivateService reasonInactivateService;
    @Mock
    private ReasonDisableService reasonDisableService;
    @Mock
    private ReasonEnableService reasonEnableService;
    @Mock
    private OrganizationConfigurationRepository organizationConfigurationRepository;
    @Mock
    private ScosUserAuthentication scosUserAuthentication;

    /** {@code Clock} não é mockado (o teste quer controlar o instante) — o bean é construído manualmente, sem {@code @InjectMocks}. */
    private EmployeeServiceBean employeeServiceBean;

    @BeforeEach
    void setUp() {
        employeeServiceBean = new EmployeeServiceBean(
                employeeQueryRepository, employeeStatusHistoryRepository, employeePositionHistoryRepository,
                employeeWorkScheduleRepository, reasonPositionChangeRepository, positionWorkScheduleRepository,
                positionService, companyService, reasonActivateService, reasonInactivateService, reasonDisableService,
                reasonEnableService, organizationConfigurationRepository,
                Clock.fixed(Instant.parse("2026-08-05T10:00:00Z"), ZoneOffset.UTC), scosUserAuthentication);
    }

    private EmployeeInput.EmployeeInputBuilder validInput() {
        return EmployeeInput.builder()
                .name("Novo Funcionário")
                .nameTreatment("Novo")
                .taxIdentifier(VALID_CPF)
                .email(VALID_EMAIL)
                .birthDate(LocalDate.of(2000, 1, 1))
                .dateOfHiring(LocalDate.of(2026, 8, 1))
                .contractType(EmployeeContractType.CLT)
                .companyId(1L)
                .positionId(1L)
                .reasonActivateId(10L);
    }

    private Company activeCompany() {
        return Company.builder().id(1L).status(br.com.sawcunhaos.organization.domain.corporate.company.internal.StatusCompany.ACTIVE).build();
    }

    private Position activePosition() {
        return Position.builder().id(1L).active(true).build();
    }

    private ReasonActivateOutput reasonActivate(boolean active, EntityType entityType) {
        return ReasonActivateOutput.builder().id(10L).code("NEW_HIRE_REASON").active(active).entityType(entityType).build();
    }

    private void stubMinimumAge(int age) {
        when(organizationConfigurationRepository.findById(ConfigurationKey.EMPLOYEE_MIN_AGE))
                .thenReturn(Optional.of(OrganizationConfiguration.builder().value(String.valueOf(age)).build()));
    }

    private void stubEmailDomain() {
        when(organizationConfigurationRepository.findById(ConfigurationKey.EMPLOYEE_EMAIL_DOMAIN))
                .thenReturn(Optional.of(OrganizationConfiguration.builder().value(EMAIL_DOMAIN).build()));
    }

    private void stubHappyPathCollaborators() {
        when(employeeQueryRepository.existsByTaxIdentifier(VALID_CPF)).thenReturn(false);
        when(employeeQueryRepository.existsByEmail(VALID_EMAIL)).thenReturn(false);
        when(companyService.findCompanyById(1L)).thenReturn(activeCompany());
        when(positionService.findPositionById(1L)).thenReturn(activePosition());
        when(reasonActivateService.findById(10L)).thenReturn(reasonActivate(true, EntityType.EMPLOYEE));
        stubEmailDomain();
        stubMinimumAge(18);
        when(reasonPositionChangeRepository.findByCode("NEW_HIRE")).thenReturn(Optional.of(ReasonPositionChange.builder().id(1L).code("NEW_HIRE").build()));
        when(employeeQueryRepository.merge(any(Employee.class))).thenAnswer(invocation -> {
            Employee employee = invocation.getArgument(0);
            employee.setId(1L);
            return employee;
        });
    }

    // ---- create: unicidade (409) ----

    @Test
    void createShouldThrowWhenTaxIdentifierAlreadyExists() {
        when(employeeQueryRepository.existsByTaxIdentifier(VALID_CPF)).thenReturn(true);

        assertThatThrownBy(() -> employeeServiceBean.create(validInput().build()))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_EMPLOYEE_002.getCode());

        verify(employeeQueryRepository, never()).merge(any());
    }

    @Test
    void createShouldThrowWhenEmailAlreadyExists() {
        when(employeeQueryRepository.existsByTaxIdentifier(VALID_CPF)).thenReturn(false);
        when(employeeQueryRepository.existsByEmail(VALID_EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> employeeServiceBean.create(validInput().build()))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_EMPLOYEE_003.getCode());

        verify(employeeQueryRepository, never()).merge(any());
    }

    // ---- create: FKs (404) ----

    @Test
    void createShouldPropagateWhenCompanyDoesNotExist() {
        when(employeeQueryRepository.existsByTaxIdentifier(VALID_CPF)).thenReturn(false);
        when(employeeQueryRepository.existsByEmail(VALID_EMAIL)).thenReturn(false);
        when(companyService.findCompanyById(1L)).thenThrow(new ScosException(br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_COMPANY_001));

        assertThatThrownBy(() -> employeeServiceBean.create(validInput().build()))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_COMPANY_001.getCode());
    }

    @Test
    void createShouldThrowWhenSupervisorDoesNotExist() {
        when(employeeQueryRepository.existsByTaxIdentifier(VALID_CPF)).thenReturn(false);
        when(employeeQueryRepository.existsByEmail(VALID_EMAIL)).thenReturn(false);
        when(companyService.findCompanyById(1L)).thenReturn(activeCompany());
        when(positionService.findPositionById(1L)).thenReturn(activePosition());
        when(employeeQueryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeServiceBean.create(validInput().supervisorId(99L).build()))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_EMPLOYEE_004.getCode());
    }

    // ---- create: estado/regra (422) ----

    @Test
    void createShouldThrowWhenCompanyIsNotActive() {
        Company inactiveCompany = Company.builder().id(1L).status(br.com.sawcunhaos.organization.domain.corporate.company.internal.StatusCompany.INACTIVE).build();
        when(employeeQueryRepository.existsByTaxIdentifier(VALID_CPF)).thenReturn(false);
        when(employeeQueryRepository.existsByEmail(VALID_EMAIL)).thenReturn(false);
        when(companyService.findCompanyById(1L)).thenReturn(inactiveCompany);

        assertThatThrownBy(() -> employeeServiceBean.create(validInput().build()))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_EMPLOYEE_005.getCode());
    }

    @Test
    void createShouldThrowWhenPositionIsInactive() {
        when(employeeQueryRepository.existsByTaxIdentifier(VALID_CPF)).thenReturn(false);
        when(employeeQueryRepository.existsByEmail(VALID_EMAIL)).thenReturn(false);
        when(companyService.findCompanyById(1L)).thenReturn(activeCompany());
        when(positionService.findPositionById(1L)).thenReturn(Position.builder().id(1L).active(false).build());

        assertThatThrownBy(() -> employeeServiceBean.create(validInput().build()))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_EMPLOYEE_006.getCode());
    }

    @Test
    void createShouldThrowWhenSupervisorIsNotActive() {
        Employee supervisor = Employee.builder().id(99L).status(StatusEmployee.INACTIVE).build();
        when(employeeQueryRepository.existsByTaxIdentifier(VALID_CPF)).thenReturn(false);
        when(employeeQueryRepository.existsByEmail(VALID_EMAIL)).thenReturn(false);
        when(companyService.findCompanyById(1L)).thenReturn(activeCompany());
        when(positionService.findPositionById(1L)).thenReturn(activePosition());
        when(employeeQueryRepository.findById(99L)).thenReturn(Optional.of(supervisor));

        assertThatThrownBy(() -> employeeServiceBean.create(validInput().supervisorId(99L).build()))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_EMPLOYEE_007.getCode());
    }

    @Test
    void createShouldThrowWhenReasonActivateIsInactive() {
        when(employeeQueryRepository.existsByTaxIdentifier(VALID_CPF)).thenReturn(false);
        when(employeeQueryRepository.existsByEmail(VALID_EMAIL)).thenReturn(false);
        when(companyService.findCompanyById(1L)).thenReturn(activeCompany());
        when(positionService.findPositionById(1L)).thenReturn(activePosition());
        when(reasonActivateService.findById(10L)).thenReturn(reasonActivate(false, EntityType.EMPLOYEE));

        assertThatThrownBy(() -> employeeServiceBean.create(validInput().build()))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_EMPLOYEE_008.getCode());
    }

    @Test
    void createShouldThrowWhenReasonActivateEntityTypeIsIncompatible() {
        when(employeeQueryRepository.existsByTaxIdentifier(VALID_CPF)).thenReturn(false);
        when(employeeQueryRepository.existsByEmail(VALID_EMAIL)).thenReturn(false);
        when(companyService.findCompanyById(1L)).thenReturn(activeCompany());
        when(positionService.findPositionById(1L)).thenReturn(activePosition());
        when(reasonActivateService.findById(10L)).thenReturn(reasonActivate(true, EntityType.COMPANY));

        assertThatThrownBy(() -> employeeServiceBean.create(validInput().build()))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_EMPLOYEE_009.getCode());
    }

    @Test
    void createShouldThrowWhenEmailDomainIsNotAllowed() {
        when(employeeQueryRepository.existsByTaxIdentifier(VALID_CPF)).thenReturn(false);
        when(employeeQueryRepository.existsByEmail(VALID_EMAIL)).thenReturn(false);
        when(companyService.findCompanyById(1L)).thenReturn(activeCompany());
        when(positionService.findPositionById(1L)).thenReturn(activePosition());
        when(reasonActivateService.findById(10L)).thenReturn(reasonActivate(true, EntityType.EMPLOYEE));
        when(organizationConfigurationRepository.findById(ConfigurationKey.EMPLOYEE_EMAIL_DOMAIN))
                .thenReturn(Optional.of(OrganizationConfiguration.builder().value("outrodominio.com").build()));

        assertThatThrownBy(() -> employeeServiceBean.create(validInput().build()))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_EMPLOYEE_010.getCode());
    }

    @Test
    void createShouldThrowWhenBirthDateIsInTheFuture() {
        when(employeeQueryRepository.existsByTaxIdentifier(VALID_CPF)).thenReturn(false);
        when(employeeQueryRepository.existsByEmail(VALID_EMAIL)).thenReturn(false);
        when(companyService.findCompanyById(1L)).thenReturn(activeCompany());
        when(positionService.findPositionById(1L)).thenReturn(activePosition());
        when(reasonActivateService.findById(10L)).thenReturn(reasonActivate(true, EntityType.EMPLOYEE));
        stubEmailDomain();

        assertThatThrownBy(() -> employeeServiceBean.create(validInput().birthDate(LocalDate.of(2027, 1, 1)).build()))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_EMPLOYEE_012.getCode());
    }

    @Test
    void createShouldThrowWhenHiringDateIsBeforeBirthDate() {
        when(employeeQueryRepository.existsByTaxIdentifier(VALID_CPF)).thenReturn(false);
        when(employeeQueryRepository.existsByEmail(VALID_EMAIL)).thenReturn(false);
        when(companyService.findCompanyById(1L)).thenReturn(activeCompany());
        when(positionService.findPositionById(1L)).thenReturn(activePosition());
        when(reasonActivateService.findById(10L)).thenReturn(reasonActivate(true, EntityType.EMPLOYEE));
        stubEmailDomain();

        assertThatThrownBy(() -> employeeServiceBean.create(validInput().birthDate(LocalDate.of(2000, 1, 1)).dateOfHiring(LocalDate.of(1999, 1, 1)).build()))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_EMPLOYEE_013.getCode());
    }

    @Test
    void createShouldThrowWhenAgeAtHiringDateIsBelowMinimum() {
        when(employeeQueryRepository.existsByTaxIdentifier(VALID_CPF)).thenReturn(false);
        when(employeeQueryRepository.existsByEmail(VALID_EMAIL)).thenReturn(false);
        when(companyService.findCompanyById(1L)).thenReturn(activeCompany());
        when(positionService.findPositionById(1L)).thenReturn(activePosition());
        when(reasonActivateService.findById(10L)).thenReturn(reasonActivate(true, EntityType.EMPLOYEE));
        stubEmailDomain();
        stubMinimumAge(18);

        assertThatThrownBy(() -> employeeServiceBean.create(validInput()
                .birthDate(LocalDate.of(2015, 1, 1))
                .dateOfHiring(LocalDate.of(2026, 1, 1))
                .build()))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_EMPLOYEE_011.getCode());
    }

    @Test
    void createShouldSucceedWhenMinimumAgeIsReachedOnlyOnFutureHiringDate() {
        stubHappyPathCollaborators();

        // birthDate 2008-08-10, dateOfHiring 2026-08-10 -> exatamente 18 anos na admissão (futura)
        EmployeeOutput result = employeeServiceBean.create(validInput()
                .birthDate(LocalDate.of(2008, 8, 10))
                .dateOfHiring(LocalDate.of(2026, 8, 10))
                .build());

        assertThat(result.status()).isEqualTo(StatusEmployee.ACTIVE);
    }

    // ---- create: happy path ----

    @Test
    void createShouldPersistEmployeeStatusHistoryAndPositionHistoryWhenValid() {
        stubHappyPathCollaborators();
        when(positionWorkScheduleRepository.findAllByPositionId(1L)).thenReturn(List.of());

        EmployeeOutput result = employeeServiceBean.create(validInput().build());

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.status()).isEqualTo(StatusEmployee.ACTIVE);
        assertThat(result.companyId()).isEqualTo(1L);
        assertThat(result.positionId()).isEqualTo(1L);

        ArgumentCaptor<EmployeeStatusHistory> historyCaptor = ArgumentCaptor.forClass(EmployeeStatusHistory.class);
        verify(employeeStatusHistoryRepository).merge(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getStatus()).isEqualTo(StatusEmployee.ACTIVE);
        assertThat(historyCaptor.getValue().getReasonActivate().getId()).isEqualTo(10L);

        ArgumentCaptor<EmployeePositionHistory> positionHistoryCaptor = ArgumentCaptor.forClass(EmployeePositionHistory.class);
        verify(employeePositionHistoryRepository).merge(positionHistoryCaptor.capture());
        assertThat(positionHistoryCaptor.getValue().getReasonPositionChange().getCode()).isEqualTo("NEW_HIRE");
        assertThat(positionHistoryCaptor.getValue().getStartDate()).isEqualTo(validInput().build().dateOfHiring());

        verify(reasonPositionChangeRepository).findByCode("NEW_HIRE");
        verify(employeeWorkScheduleRepository, never()).merge(any());
    }

    @Test
    void createShouldCopyAllWorkScheduleDaysFromPositionTemplate() {
        stubHappyPathCollaborators();
        List<PositionWorkSchedule> template = List.of(
                PositionWorkSchedule.builder().dayOfWeek(DayOfWeek.MONDAY).startTime(java.time.LocalTime.of(8, 0)).lunchStart(java.time.LocalTime.of(12, 0)).lunchEnd(java.time.LocalTime.of(13, 0)).endTime(java.time.LocalTime.of(17, 0)).build(),
                PositionWorkSchedule.builder().dayOfWeek(DayOfWeek.TUESDAY).startTime(java.time.LocalTime.of(8, 0)).lunchStart(java.time.LocalTime.of(12, 0)).lunchEnd(java.time.LocalTime.of(13, 0)).endTime(java.time.LocalTime.of(17, 0)).build()
        );
        when(positionWorkScheduleRepository.findAllByPositionId(1L)).thenReturn(template);

        employeeServiceBean.create(validInput().build());

        verify(employeeWorkScheduleRepository, times(2)).merge(any(EmployeeWorkSchedule.class));
    }

    // ---- activate (rota enable, UC-039) ----

    @Test
    void activateShouldPersistHistoryWhenValid() {
        Employee employee = Employee.builder().id(1L).status(StatusEmployee.INACTIVE).build();
        when(employeeQueryRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(reasonActivateService.findById(10L)).thenReturn(reasonActivate(true, EntityType.EMPLOYEE));
        when(scosUserAuthentication.findUserAuthentication()).thenReturn("tester");

        employeeServiceBean.activate(1L, 10L, "Retorno de licença");

        ArgumentCaptor<EmployeeStatusHistory> captor = ArgumentCaptor.forClass(EmployeeStatusHistory.class);
        verify(employeeStatusHistoryRepository).merge(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(StatusEmployee.ACTIVE);
        assertThat(captor.getValue().getReasonActivate().getId()).isEqualTo(10L);
        assertThat(captor.getValue().getObservation()).isEqualTo("Retorno de licença");
        assertThat(captor.getValue().getUserAt()).isEqualTo("tester");
    }

    @Test
    void activateShouldThrowWhenEmployeeNotFound() {
        when(employeeQueryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeServiceBean.activate(1L, 10L, null))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_EMPLOYEE_014.getCode());
    }

    @Test
    void activateShouldThrowWhenEmployeeIsNotInactive() {
        Employee employee = Employee.builder().id(1L).status(StatusEmployee.ACTIVE).build();
        when(employeeQueryRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(reasonActivateService.findById(10L)).thenReturn(reasonActivate(true, EntityType.EMPLOYEE));

        assertThatThrownBy(() -> employeeServiceBean.activate(1L, 10L, null))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_EMPLOYEE_001.getCode());

        verify(employeeStatusHistoryRepository, never()).merge(any());
    }

    @Test
    void activateShouldThrowWhenReasonIsInactive() {
        Employee employee = Employee.builder().id(1L).status(StatusEmployee.INACTIVE).build();
        when(employeeQueryRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(reasonActivateService.findById(10L)).thenReturn(reasonActivate(false, EntityType.EMPLOYEE));

        assertThatThrownBy(() -> employeeServiceBean.activate(1L, 10L, null))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_EMPLOYEE_008.getCode());
    }

    @Test
    void activateShouldThrowWhenReasonEntityTypeIsIncompatible() {
        Employee employee = Employee.builder().id(1L).status(StatusEmployee.INACTIVE).build();
        when(employeeQueryRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(reasonActivateService.findById(10L)).thenReturn(reasonActivate(true, EntityType.COMPANY));

        assertThatThrownBy(() -> employeeServiceBean.activate(1L, 10L, null))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_EMPLOYEE_009.getCode());
    }

    // ---- inactivate (rota disable, UC-040) ----

    @Test
    void inactivateShouldPersistHistoryWhenValid() {
        Employee employee = Employee.builder().id(1L).status(StatusEmployee.ACTIVE).build();
        when(employeeQueryRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(reasonInactivateService.findById(20L)).thenReturn(reasonInactivate(true, EntityType.EMPLOYEE));
        when(scosUserAuthentication.findUserAuthentication()).thenReturn("tester");

        employeeServiceBean.inactivate(1L, 20L, "Desligamento");

        ArgumentCaptor<EmployeeStatusHistory> captor = ArgumentCaptor.forClass(EmployeeStatusHistory.class);
        verify(employeeStatusHistoryRepository).merge(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(StatusEmployee.INACTIVE);
        assertThat(captor.getValue().getReasonInactivate().getId()).isEqualTo(20L);
        assertThat(captor.getValue().getObservation()).isEqualTo("Desligamento");
    }

    @Test
    void inactivateShouldAcceptDisabledAsOrigin() {
        Employee employee = Employee.builder().id(1L).status(StatusEmployee.DISABLED).build();
        when(employeeQueryRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(reasonInactivateService.findById(20L)).thenReturn(reasonInactivate(true, EntityType.EMPLOYEE));

        employeeServiceBean.inactivate(1L, 20L, null);

        verify(employeeStatusHistoryRepository).merge(any(EmployeeStatusHistory.class));
    }

    @Test
    void inactivateShouldThrowWhenEmployeeNotFound() {
        when(employeeQueryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeServiceBean.inactivate(1L, 20L, null))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_EMPLOYEE_014.getCode());
    }

    @Test
    void inactivateShouldThrowWhenEmployeeIsAlreadyInactive() {
        Employee employee = Employee.builder().id(1L).status(StatusEmployee.INACTIVE).build();
        when(employeeQueryRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(reasonInactivateService.findById(20L)).thenReturn(reasonInactivate(true, EntityType.EMPLOYEE));

        assertThatThrownBy(() -> employeeServiceBean.inactivate(1L, 20L, null))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_EMPLOYEE_001.getCode());
    }

    @Test
    void inactivateShouldThrowWhenReasonIsInactive() {
        Employee employee = Employee.builder().id(1L).status(StatusEmployee.ACTIVE).build();
        when(employeeQueryRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(reasonInactivateService.findById(20L)).thenReturn(reasonInactivate(false, EntityType.EMPLOYEE));

        assertThatThrownBy(() -> employeeServiceBean.inactivate(1L, 20L, null))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_EMPLOYEE_015.getCode());
    }

    @Test
    void inactivateShouldThrowWhenReasonEntityTypeIsIncompatible() {
        Employee employee = Employee.builder().id(1L).status(StatusEmployee.ACTIVE).build();
        when(employeeQueryRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(reasonInactivateService.findById(20L)).thenReturn(reasonInactivate(true, EntityType.COMPANY));

        assertThatThrownBy(() -> employeeServiceBean.inactivate(1L, 20L, null))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_EMPLOYEE_016.getCode());
    }

    // ---- disable (rota block, UC-043) ----

    @Test
    void disableShouldPersistHistoryWhenValid() {
        Employee employee = Employee.builder().id(1L).status(StatusEmployee.ACTIVE).build();
        when(employeeQueryRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(reasonDisableService.findById(30L)).thenReturn(reasonDisable(true, EntityType.EMPLOYEE));
        when(scosUserAuthentication.findUserAuthentication()).thenReturn("tester");

        employeeServiceBean.disable(1L, 30L, "Suspensão em auditoria");

        ArgumentCaptor<EmployeeStatusHistory> captor = ArgumentCaptor.forClass(EmployeeStatusHistory.class);
        verify(employeeStatusHistoryRepository).merge(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(StatusEmployee.DISABLED);
        assertThat(captor.getValue().getReasonDisable().getId()).isEqualTo(30L);
    }

    @Test
    void disableShouldThrowWhenEmployeeNotFound() {
        when(employeeQueryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeServiceBean.disable(1L, 30L, null))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_EMPLOYEE_014.getCode());
    }

    @Test
    void disableShouldThrowWhenEmployeeIsNotActive() {
        Employee employee = Employee.builder().id(1L).status(StatusEmployee.INACTIVE).build();
        when(employeeQueryRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(reasonDisableService.findById(30L)).thenReturn(reasonDisable(true, EntityType.EMPLOYEE));

        assertThatThrownBy(() -> employeeServiceBean.disable(1L, 30L, null))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_EMPLOYEE_001.getCode());
    }

    @Test
    void disableShouldThrowWhenReasonIsInactive() {
        Employee employee = Employee.builder().id(1L).status(StatusEmployee.ACTIVE).build();
        when(employeeQueryRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(reasonDisableService.findById(30L)).thenReturn(reasonDisable(false, EntityType.EMPLOYEE));

        assertThatThrownBy(() -> employeeServiceBean.disable(1L, 30L, null))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_EMPLOYEE_017.getCode());
    }

    @Test
    void disableShouldThrowWhenReasonEntityTypeIsIncompatible() {
        Employee employee = Employee.builder().id(1L).status(StatusEmployee.ACTIVE).build();
        when(employeeQueryRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(reasonDisableService.findById(30L)).thenReturn(reasonDisable(true, EntityType.COMPANY));

        assertThatThrownBy(() -> employeeServiceBean.disable(1L, 30L, null))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_EMPLOYEE_018.getCode());
    }

    // ---- enable (rota unblock, UC-139) ----

    @Test
    void enableShouldPersistHistoryWhenValid() {
        Employee employee = Employee.builder().id(1L).status(StatusEmployee.DISABLED).build();
        when(employeeQueryRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(reasonEnableService.findById(40L)).thenReturn(reasonEnable(true, EntityType.EMPLOYEE));
        when(scosUserAuthentication.findUserAuthentication()).thenReturn("tester");

        employeeServiceBean.enable(1L, 40L, "Auditoria concluída");

        ArgumentCaptor<EmployeeStatusHistory> captor = ArgumentCaptor.forClass(EmployeeStatusHistory.class);
        verify(employeeStatusHistoryRepository).merge(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(StatusEmployee.ACTIVE);
        assertThat(captor.getValue().getReasonEnable().getId()).isEqualTo(40L);
    }

    @Test
    void enableShouldThrowWhenEmployeeNotFound() {
        when(employeeQueryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeServiceBean.enable(1L, 40L, null))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_EMPLOYEE_014.getCode());
    }

    @Test
    void enableShouldThrowWhenEmployeeIsNotDisabled() {
        Employee employee = Employee.builder().id(1L).status(StatusEmployee.ACTIVE).build();
        when(employeeQueryRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(reasonEnableService.findById(40L)).thenReturn(reasonEnable(true, EntityType.EMPLOYEE));

        assertThatThrownBy(() -> employeeServiceBean.enable(1L, 40L, null))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_EMPLOYEE_001.getCode());
    }

    @Test
    void enableShouldThrowWhenReasonIsInactive() {
        Employee employee = Employee.builder().id(1L).status(StatusEmployee.DISABLED).build();
        when(employeeQueryRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(reasonEnableService.findById(40L)).thenReturn(reasonEnable(false, EntityType.EMPLOYEE));

        assertThatThrownBy(() -> employeeServiceBean.enable(1L, 40L, null))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_EMPLOYEE_019.getCode());
    }

    @Test
    void enableShouldThrowWhenReasonEntityTypeIsIncompatible() {
        Employee employee = Employee.builder().id(1L).status(StatusEmployee.DISABLED).build();
        when(employeeQueryRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(reasonEnableService.findById(40L)).thenReturn(reasonEnable(true, EntityType.COMPANY));

        assertThatThrownBy(() -> employeeServiceBean.enable(1L, 40L, null))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_EMPLOYEE_020.getCode());
    }

    private ReasonInactivateOutput reasonInactivate(boolean active, EntityType entityType) {
        return ReasonInactivateOutput.builder().id(20L).code("RESIGNATION").active(active).entityType(entityType).build();
    }

    private ReasonDisableOutput reasonDisable(boolean active, EntityType entityType) {
        return ReasonDisableOutput.builder().id(30L).code("UNDER_AUDIT").active(active).entityType(entityType).build();
    }

    private ReasonEnableOutput reasonEnable(boolean active, EntityType entityType) {
        return ReasonEnableOutput.builder().id(40L).code("AUDIT_CLEARED").active(active).entityType(entityType).build();
    }

    // ---- rehire (UC-041) ----

    private RehireEmployeeInput.RehireEmployeeInputBuilder validRehireInput() {
        return RehireEmployeeInput.builder()
                .taxIdentifier(VALID_CPF)
                .companyId(1L)
                .positionId(1L)
                .contractType(EmployeeContractType.CLT)
                .reasonActivateId(10L)
                .reasonPositionChangeId(50L);
    }

    private void stubRehireHappyPathCollaborators(Employee inactiveEmployee) {
        when(employeeQueryRepository.findByTaxIdentifierAndStatus(VALID_CPF, StatusEmployee.INACTIVE)).thenReturn(Optional.of(inactiveEmployee));
        when(companyService.findCompanyById(1L)).thenReturn(activeCompany());
        when(positionService.findPositionById(1L)).thenReturn(activePosition());
        when(reasonActivateService.findById(10L)).thenReturn(reasonActivate(true, EntityType.EMPLOYEE));
        when(reasonPositionChangeRepository.findById(50L)).thenReturn(Optional.of(ReasonPositionChange.builder().id(50L).code("REINSTATEMENT").active(true).build()));
        when(employeeQueryRepository.merge(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(scosUserAuthentication.findUserAuthentication()).thenReturn("tester");
    }

    @Test
    void rehireShouldReassignFieldsAndPersistHistoriesWhenDateOfRehireInformed() {
        Employee previousSupervisor = Employee.builder().id(77L).status(StatusEmployee.ACTIVE).build();
        Employee employee = Employee.builder().id(1L).status(StatusEmployee.INACTIVE).supervisor(previousSupervisor)
                .taxIdentifier(new Cpf(VALID_CPF)).email(new Email(VALID_EMAIL)).build();
        stubRehireHappyPathCollaborators(employee);
        when(employeeQueryRepository.findById(88L)).thenReturn(Optional.of(Employee.builder().id(88L).status(StatusEmployee.ACTIVE).build()));

        EmployeeOutput result = employeeServiceBean.rehire(validRehireInput()
                .supervisorId(88L)
                .probationEndDate(LocalDate.of(2027, 2, 1))
                .dateOfRehire(LocalDate.of(2026, 9, 1))
                .observation("Retorno de licença")
                .build());

        assertThat(result.status()).isEqualTo(StatusEmployee.ACTIVE);
        assertThat(result.companyId()).isEqualTo(1L);
        assertThat(result.positionId()).isEqualTo(1L);
        assertThat(result.supervisorId()).isEqualTo(88L);
        assertThat(employee.getContractType()).isEqualTo(EmployeeContractType.CLT);
        assertThat(employee.getProbationEndDate()).isEqualTo(LocalDate.of(2027, 2, 1));
        assertThat(employee.getDateOfHiring()).isEqualTo(LocalDate.of(2026, 9, 1));

        ArgumentCaptor<EmployeeStatusHistory> statusHistoryCaptor = ArgumentCaptor.forClass(EmployeeStatusHistory.class);
        verify(employeeStatusHistoryRepository).merge(statusHistoryCaptor.capture());
        assertThat(statusHistoryCaptor.getValue().getStatus()).isEqualTo(StatusEmployee.ACTIVE);
        assertThat(statusHistoryCaptor.getValue().getReasonActivate().getId()).isEqualTo(10L);
        assertThat(statusHistoryCaptor.getValue().getObservation()).isEqualTo("Retorno de licença");

        ArgumentCaptor<EmployeePositionHistory> positionHistoryCaptor = ArgumentCaptor.forClass(EmployeePositionHistory.class);
        verify(employeePositionHistoryRepository).merge(positionHistoryCaptor.capture());
        assertThat(positionHistoryCaptor.getValue().getStartDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(positionHistoryCaptor.getValue().getReasonPositionChange().getId()).isEqualTo(50L);
    }

    @Test
    void rehireShouldUseClockNowAsStartDateAndKeepOriginalHiringDateWhenDateOfRehireOmitted() {
        Employee employee = Employee.builder().id(1L).status(StatusEmployee.INACTIVE).dateOfHiring(LocalDate.of(2020, 1, 1))
                .taxIdentifier(new Cpf(VALID_CPF)).email(new Email(VALID_EMAIL)).build();
        stubRehireHappyPathCollaborators(employee);

        employeeServiceBean.rehire(validRehireInput().build());

        assertThat(employee.getDateOfHiring()).isEqualTo(LocalDate.of(2020, 1, 1));
        ArgumentCaptor<EmployeePositionHistory> positionHistoryCaptor = ArgumentCaptor.forClass(EmployeePositionHistory.class);
        verify(employeePositionHistoryRepository).merge(positionHistoryCaptor.capture());
        assertThat(positionHistoryCaptor.getValue().getStartDate()).isEqualTo(LocalDate.of(2026, 8, 5)); // Clock.fixed do setUp
    }

    @Test
    void rehireShouldSetSupervisorNullWhenOmittedEvenIfEmployeeHadOneBefore() {
        Employee previousSupervisor = Employee.builder().id(77L).status(StatusEmployee.ACTIVE).build();
        Employee employee = Employee.builder().id(1L).status(StatusEmployee.INACTIVE).supervisor(previousSupervisor)
                .taxIdentifier(new Cpf(VALID_CPF)).email(new Email(VALID_EMAIL)).build();
        stubRehireHappyPathCollaborators(employee);

        EmployeeOutput result = employeeServiceBean.rehire(validRehireInput().build());

        assertThat(result.supervisorId()).isNull();
        assertThat(employee.getSupervisor()).isNull();
    }

    @Test
    void rehireShouldThrowWhenNoInactiveEmployeeFoundForTaxIdentifier() {
        // cobre os 3 casos do AC 6 com o mesmo código: CPF nunca cadastrado, ou pertence a um Funcionário ACTIVE, ou a um DISABLED —
        // em todos, employeeQueryRepository.findByTaxIdentifierAndStatus(cpf, INACTIVE) retorna vazio.
        when(employeeQueryRepository.findByTaxIdentifierAndStatus(VALID_CPF, StatusEmployee.INACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeServiceBean.rehire(validRehireInput().build()))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_EMPLOYEE_021.getCode());

        verify(employeeStatusHistoryRepository, never()).merge(any());
    }

    @Test
    void rehireShouldPropagateWhenCompanyDoesNotExist() {
        Employee employee = Employee.builder().id(1L).status(StatusEmployee.INACTIVE).build();
        when(employeeQueryRepository.findByTaxIdentifierAndStatus(VALID_CPF, StatusEmployee.INACTIVE)).thenReturn(Optional.of(employee));
        when(companyService.findCompanyById(1L)).thenThrow(new ScosException(br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_COMPANY_001));

        assertThatThrownBy(() -> employeeServiceBean.rehire(validRehireInput().build()))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_COMPANY_001.getCode());
    }

    @Test
    void rehireShouldThrowWhenCompanyIsNotActive() {
        Employee employee = Employee.builder().id(1L).status(StatusEmployee.INACTIVE).build();
        when(employeeQueryRepository.findByTaxIdentifierAndStatus(VALID_CPF, StatusEmployee.INACTIVE)).thenReturn(Optional.of(employee));
        when(companyService.findCompanyById(1L)).thenReturn(Company.builder().id(1L).status(br.com.sawcunhaos.organization.domain.corporate.company.internal.StatusCompany.INACTIVE).build());

        assertThatThrownBy(() -> employeeServiceBean.rehire(validRehireInput().build()))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_EMPLOYEE_005.getCode());
    }

    @Test
    void rehireShouldThrowWhenPositionIsInactive() {
        Employee employee = Employee.builder().id(1L).status(StatusEmployee.INACTIVE).build();
        when(employeeQueryRepository.findByTaxIdentifierAndStatus(VALID_CPF, StatusEmployee.INACTIVE)).thenReturn(Optional.of(employee));
        when(companyService.findCompanyById(1L)).thenReturn(activeCompany());
        when(positionService.findPositionById(1L)).thenReturn(Position.builder().id(1L).active(false).build());

        assertThatThrownBy(() -> employeeServiceBean.rehire(validRehireInput().build()))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_EMPLOYEE_006.getCode());
    }

    @Test
    void rehireShouldThrowWhenSupervisorDoesNotExist() {
        Employee employee = Employee.builder().id(1L).status(StatusEmployee.INACTIVE).build();
        when(employeeQueryRepository.findByTaxIdentifierAndStatus(VALID_CPF, StatusEmployee.INACTIVE)).thenReturn(Optional.of(employee));
        when(companyService.findCompanyById(1L)).thenReturn(activeCompany());
        when(positionService.findPositionById(1L)).thenReturn(activePosition());
        when(employeeQueryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeServiceBean.rehire(validRehireInput().supervisorId(99L).build()))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_EMPLOYEE_004.getCode());
    }

    @Test
    void rehireShouldThrowWhenSupervisorIsNotActive() {
        Employee employee = Employee.builder().id(1L).status(StatusEmployee.INACTIVE).build();
        when(employeeQueryRepository.findByTaxIdentifierAndStatus(VALID_CPF, StatusEmployee.INACTIVE)).thenReturn(Optional.of(employee));
        when(companyService.findCompanyById(1L)).thenReturn(activeCompany());
        when(positionService.findPositionById(1L)).thenReturn(activePosition());
        when(employeeQueryRepository.findById(99L)).thenReturn(Optional.of(Employee.builder().id(99L).status(StatusEmployee.INACTIVE).build()));

        assertThatThrownBy(() -> employeeServiceBean.rehire(validRehireInput().supervisorId(99L).build()))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_EMPLOYEE_007.getCode());
    }

    @Test
    void rehireShouldThrowWhenReasonActivateIsInactive() {
        Employee employee = Employee.builder().id(1L).status(StatusEmployee.INACTIVE).build();
        when(employeeQueryRepository.findByTaxIdentifierAndStatus(VALID_CPF, StatusEmployee.INACTIVE)).thenReturn(Optional.of(employee));
        when(companyService.findCompanyById(1L)).thenReturn(activeCompany());
        when(positionService.findPositionById(1L)).thenReturn(activePosition());
        when(reasonActivateService.findById(10L)).thenReturn(reasonActivate(false, EntityType.EMPLOYEE));

        assertThatThrownBy(() -> employeeServiceBean.rehire(validRehireInput().build()))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_EMPLOYEE_008.getCode());
    }

    @Test
    void rehireShouldThrowWhenReasonActivateEntityTypeIsIncompatible() {
        Employee employee = Employee.builder().id(1L).status(StatusEmployee.INACTIVE).build();
        when(employeeQueryRepository.findByTaxIdentifierAndStatus(VALID_CPF, StatusEmployee.INACTIVE)).thenReturn(Optional.of(employee));
        when(companyService.findCompanyById(1L)).thenReturn(activeCompany());
        when(positionService.findPositionById(1L)).thenReturn(activePosition());
        when(reasonActivateService.findById(10L)).thenReturn(reasonActivate(true, EntityType.COMPANY));

        assertThatThrownBy(() -> employeeServiceBean.rehire(validRehireInput().build()))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_EMPLOYEE_009.getCode());
    }

    @Test
    void rehireShouldThrowWhenReasonPositionChangeDoesNotExist() {
        Employee employee = Employee.builder().id(1L).status(StatusEmployee.INACTIVE).build();
        when(employeeQueryRepository.findByTaxIdentifierAndStatus(VALID_CPF, StatusEmployee.INACTIVE)).thenReturn(Optional.of(employee));
        when(companyService.findCompanyById(1L)).thenReturn(activeCompany());
        when(positionService.findPositionById(1L)).thenReturn(activePosition());
        when(reasonActivateService.findById(10L)).thenReturn(reasonActivate(true, EntityType.EMPLOYEE));
        when(reasonPositionChangeRepository.findById(50L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeServiceBean.rehire(validRehireInput().build()))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_EMPLOYEE_022.getCode());
    }

    @Test
    void rehireShouldThrowWhenReasonPositionChangeIsInactive() {
        Employee employee = Employee.builder().id(1L).status(StatusEmployee.INACTIVE).build();
        when(employeeQueryRepository.findByTaxIdentifierAndStatus(VALID_CPF, StatusEmployee.INACTIVE)).thenReturn(Optional.of(employee));
        when(companyService.findCompanyById(1L)).thenReturn(activeCompany());
        when(positionService.findPositionById(1L)).thenReturn(activePosition());
        when(reasonActivateService.findById(10L)).thenReturn(reasonActivate(true, EntityType.EMPLOYEE));
        when(reasonPositionChangeRepository.findById(50L)).thenReturn(Optional.of(ReasonPositionChange.builder().id(50L).active(false).build()));

        assertThatThrownBy(() -> employeeServiceBean.rehire(validRehireInput().build()))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_EMPLOYEE_023.getCode());
    }

    // ---- findById ----

    @Test
    void findByIdShouldReturnEmployeeOutputWhenExists() {
        Employee employee = Employee.builder().id(1L).status(StatusEmployee.ACTIVE)
                .taxIdentifier(new Cpf(VALID_CPF)).email(new Email(VALID_EMAIL))
                .company(activeCompany()).position(activePosition()).build();
        when(employeeQueryRepository.findById(1L)).thenReturn(Optional.of(employee));

        EmployeeOutput result = employeeServiceBean.findById(1L);

        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    void findByIdShouldThrowWhenEmployeeNotFound() {
        when(employeeQueryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeServiceBean.findById(1L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_EMPLOYEE_014.getCode());
    }
}
