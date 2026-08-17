
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

package br.com.sawcunhaos.organization.domain.access.login.service;

import br.com.sawcunhaos.organization.domain.access.login.internal.Login;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestLevel;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginType;
import br.com.sawcunhaos.organization.domain.corporate.department.internal.Department;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.Employee;
import br.com.sawcunhaos.organization.domain.corporate.position.internal.Position;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestLevel.MANAGER;
import static br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestLevel.SUPERVISOR;
import static br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestLevel.SYSTEM_ACCESS_GROUP;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobre a decisão de design mais importante da Story 3.2: a cadeia pula um nível quando o
 * Funcionário resolvido está ausente (sem supervisor / departamento sem gerente), e Login
 * EXTERNAL/SERVICE (sem Funcionário vinculado) vai direto para SYSTEM_ACCESS_GROUP (FR-26).
 */
class LoginApprovalChainResolverTest {

    private Employee supervisor(Long id) {
        return Employee.builder().id(id).name("Supervisor").build();
    }

    private Department department(Employee manager) {
        return Department.builder().id(1L).code("TI").description("TI").manager(manager).build();
    }

    private Position position(Department department) {
        return Position.builder().id(1L).code("DEV").description("Dev").department(department).build();
    }

    private Employee employee(Employee supervisor, Position position) {
        return Employee.builder().id(2L).name("Jane Doe").supervisor(supervisor).position(position).build();
    }

    private Login employeeLogin(Employee employee) {
        return Login.builder().id(10L).login("jane.doe").type(LoginType.EMPLOYEE).employee(employee).build();
    }

    // ---- firstLevelFor ----

    @Test
    void firstLevelForShouldResolveSupervisorWhenPresent() {
        Employee supervisor = supervisor(1L);
        Employee employee = employee(supervisor, position(department(null)));
        Login login = employeeLogin(employee);

        Optional<LoginApprovalRequestLevel> result = LoginApprovalChainResolver.firstLevelFor(login);

        assertThat(result).contains(SUPERVISOR);
    }

    @Test
    void firstLevelForShouldFallBackToManagerWhenSupervisorIsAbsent() {
        Employee manager = supervisor(3L);
        Employee employee = employee(null, position(department(manager)));
        Login login = employeeLogin(employee);

        Optional<LoginApprovalRequestLevel> result = LoginApprovalChainResolver.firstLevelFor(login);

        assertThat(result).contains(MANAGER);
    }

    @Test
    void firstLevelForShouldFallBackToSystemAccessGroupWhenSupervisorAndManagerAreAbsent() {
        Employee employee = employee(null, position(department(null)));
        Login login = employeeLogin(employee);

        Optional<LoginApprovalRequestLevel> result = LoginApprovalChainResolver.firstLevelFor(login);

        assertThat(result).contains(SYSTEM_ACCESS_GROUP);
    }

    @Test
    void firstLevelForShouldGoDirectlyToSystemAccessGroupForExternalOrServiceLogin() {
        Login externalLogin = Login.builder().id(11L).login("partner-x").type(LoginType.EXTERNAL).employee(null).build();

        Optional<LoginApprovalRequestLevel> result = LoginApprovalChainResolver.firstLevelFor(externalLogin);

        assertThat(result).contains(SYSTEM_ACCESS_GROUP);
    }

    // ---- nextLevelAfter ----

    @Test
    void nextLevelAfterSupervisorShouldResolveManagerWhenPresent() {
        Employee manager = supervisor(3L);
        Employee employee = employee(supervisor(1L), position(department(manager)));
        Login login = employeeLogin(employee);

        Optional<LoginApprovalRequestLevel> result = LoginApprovalChainResolver.nextLevelAfter(SUPERVISOR, login);

        assertThat(result).contains(MANAGER);
    }

    @Test
    void nextLevelAfterManagerShouldFallBackToSystemAccessGroupWhenManagerIsAbsent() {
        Employee employee = employee(supervisor(1L), position(department(null)));
        Login login = employeeLogin(employee);

        Optional<LoginApprovalRequestLevel> result = LoginApprovalChainResolver.nextLevelAfter(MANAGER, login);

        assertThat(result).contains(SYSTEM_ACCESS_GROUP);
    }

    @Test
    void nextLevelAfterSystemAccessGroupShouldNotEscalateFurther() {
        Employee employee = employee(supervisor(1L), position(department(null)));
        Login login = employeeLogin(employee);

        Optional<LoginApprovalRequestLevel> result = LoginApprovalChainResolver.nextLevelAfter(SYSTEM_ACCESS_GROUP, login);

        assertThat(result).isEmpty();
    }

    // ---- resolveApproverEmployee ----

    @Test
    void resolveApproverEmployeeShouldReturnSupervisorForSupervisorLevel() {
        Employee supervisor = supervisor(1L);
        Employee employee = employee(supervisor, position(department(null)));
        Login login = employeeLogin(employee);

        Optional<Employee> result = LoginApprovalChainResolver.resolveApproverEmployee(SUPERVISOR, login);

        assertThat(result).contains(supervisor);
    }

    @Test
    void resolveApproverEmployeeShouldReturnManagerForManagerLevel() {
        Employee manager = supervisor(3L);
        Employee employee = employee(null, position(department(manager)));
        Login login = employeeLogin(employee);

        Optional<Employee> result = LoginApprovalChainResolver.resolveApproverEmployee(MANAGER, login);

        assertThat(result).contains(manager);
    }

    @Test
    void resolveApproverEmployeeShouldReturnEmptyForSystemAccessGroupLevel() {
        Employee employee = employee(supervisor(1L), position(department(null)));
        Login login = employeeLogin(employee);

        Optional<Employee> result = LoginApprovalChainResolver.resolveApproverEmployee(SYSTEM_ACCESS_GROUP, login);

        assertThat(result).isEmpty();
    }

    @Test
    void resolveApproverEmployeeShouldReturnEmptyWhenLoginHasNoEmployee() {
        Login login = Login.builder().id(11L).login("partner-x").type(LoginType.EXTERNAL).employee(null).build();

        Optional<Employee> result = LoginApprovalChainResolver.resolveApproverEmployee(SYSTEM_ACCESS_GROUP, login);

        assertThat(result).isEmpty();
    }
}
