
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
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.Employee;
import br.com.sawcunhaos.organization.domain.corporate.position.internal.Position;

import java.util.Optional;

import static br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestLevel.MANAGER;
import static br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestLevel.SUPERVISOR;
import static br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestLevel.SYSTEM_ACCESS_GROUP;

/**
 * Resolve o nível/aprovador da cadeia de aprovação de acesso (FR-23/24/25/26). Função pura sobre
 * entidades já carregadas - sem estado, sem dependência de infraestrutura, reaproveitável por
 * Use Cases (módulo usecase) e pelo {@link LoginApprovalEscalationJob}.
 *
 * <p>Login {@code EMPLOYEE}: {@code SUPERVISOR → MANAGER → SYSTEM_ACCESS_GROUP}, pulando um nível
 * quando o Funcionário resolvido para aquele nível está ausente. Login {@code EXTERNAL}/{@code SERVICE}
 * (sem Funcionário vinculado): sempre {@code SYSTEM_ACCESS_GROUP} direto (FR-26) - infraestrutura
 * genérica reaproveitada pelo Epic 4.
 */
public final class LoginApprovalChainResolver {

    private LoginApprovalChainResolver() {
    }

    /** Primeiro nível da cadeia para um Login recém-criado. */
    public static Optional<LoginApprovalRequestLevel> firstLevelFor(Login login) {
        return resolveFrom(SUPERVISOR, login);
    }

    /** Próximo nível após {@code current}, pulando ausentes. Vazio se {@code current} já for o último (não escala mais). */
    public static Optional<LoginApprovalRequestLevel> nextLevelAfter(LoginApprovalRequestLevel current, Login login) {
        return switch (current) {
            case SUPERVISOR -> resolveFrom(MANAGER, login);
            case MANAGER -> resolveFrom(SYSTEM_ACCESS_GROUP, login);
            case SYSTEM_ACCESS_GROUP -> Optional.empty();
        };
    }

    /** Funcionário resolvido para {@code level}. Vazio para {@code SYSTEM_ACCESS_GROUP} - esse nível não resolve uma pessoa, resolve uma permissão. */
    public static Optional<Employee> resolveApproverEmployee(LoginApprovalRequestLevel level, Login login) {
        Employee employee = login.getEmployee();
        if (employee == null) {
            return Optional.empty();
        }
        return switch (level) {
            case SUPERVISOR -> Optional.ofNullable(employee.getSupervisor());
            case MANAGER -> Optional.ofNullable(departmentManager(employee));
            case SYSTEM_ACCESS_GROUP -> Optional.empty();
        };
    }

    private static Optional<LoginApprovalRequestLevel> resolveFrom(LoginApprovalRequestLevel level, Login login) {
        if (login.getType() != LoginType.EMPLOYEE || login.getEmployee() == null) {
            return Optional.of(SYSTEM_ACCESS_GROUP);
        }
        Employee employee = login.getEmployee();
        return switch (level) {
            case SUPERVISOR -> employee.getSupervisor() != null ? Optional.of(SUPERVISOR) : resolveFrom(MANAGER, login);
            case MANAGER -> departmentManager(employee) != null ? Optional.of(MANAGER) : resolveFrom(SYSTEM_ACCESS_GROUP, login);
            case SYSTEM_ACCESS_GROUP -> Optional.of(SYSTEM_ACCESS_GROUP);
        };
    }

    private static Employee departmentManager(Employee employee) {
        Position position = employee.getPosition();
        if (position == null || position.getDepartment() == null) {
            return null;
        }
        return position.getDepartment().getManager();
    }
}
