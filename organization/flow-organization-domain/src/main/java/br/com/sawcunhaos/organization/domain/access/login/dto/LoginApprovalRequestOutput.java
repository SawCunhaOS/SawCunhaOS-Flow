
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

package br.com.sawcunhaos.organization.domain.access.login.dto;

import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestEscalationPolicy;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestLevel;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestStatus;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestType;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Retorno de leitura de {@code LoginApprovalRequest} — flat, com ids de FK (sem entidades
 * aninhadas). {@code currentApproverEmployee*} é calculado na hora da consulta pelo
 * {@code LoginApprovalChainResolver} (AC 10), nunca persistido - {@code null} quando
 * {@code currentLevel=SYSTEM_ACCESS_GROUP} (esse nível não resolve uma pessoa).
 */
@Builder
public record LoginApprovalRequestOutput(
        Long id,
        Long loginId,
        LoginApprovalRequestType requestType,
        LoginApprovalRequestLevel currentLevel,
        LoginApprovalRequestStatus status,
        LoginApprovalRequestEscalationPolicy escalationPolicy,
        Instant slaDeadline,
        Long decidedByLoginId,
        Instant decidedAt,
        LocalDateTime createdAt,
        Long currentApproverEmployeeId,
        String currentApproverEmployeeName
) {
}
