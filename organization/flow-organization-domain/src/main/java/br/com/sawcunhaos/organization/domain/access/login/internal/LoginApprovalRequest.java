
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

package br.com.sawcunhaos.organization.domain.access.login.internal;

import br.com.sawcunhaos.flow.audit.sdk.api.AuditableEntity;
import br.com.sawcunhaos.foundation.jpa.entity.BaseEntity;
import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.organization.domain.access.profile.internal.Profile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_LOGIN_APPROVAL_REQUEST_002;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "SCOS_LOGIN_APPROVAL_REQUEST")
@AuditableEntity
public class LoginApprovalRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LOGIN_APPROVAL_REQUEST_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LOGIN_ID")
    private Login login;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REQUESTED_BY_LOGIN_ID")
    private Login requestedByLogin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DECIDED_BY_LOGIN_ID")
    private Login decidedByLogin;

    @Column(name = "CURRENT_LEVEL")
    @Enumerated(EnumType.STRING)
    private LoginApprovalRequestLevel currentLevel;
    @Column(name = "ESCALATION_POLICY")
    @Enumerated(EnumType.STRING)
    private LoginApprovalRequestEscalationPolicy escalationPolicy;
    @Column(name = "STATUS")
    @Enumerated(EnumType.STRING)
    private LoginApprovalRequestStatus status;
    @Column(name = "REQUEST_TYPE")
    @Enumerated(EnumType.STRING)
    private LoginApprovalRequestType requestType;
    @Column(name = "IS_EXCEPTION_SELF_APPROVAL")
    private boolean isExceptionSelfApproval;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REQUESTED_PROFILE_ID")
    private Profile requestedProfile;
    @Column(name = "PROFILE_CHANGE_KIND")
    @Enumerated(EnumType.STRING)
    private LoginApprovalRequestProfileChangeKind profileChangeKind;

    @Column(name = "SLA_DEADLINE")
    private Instant slaDeadline;
    @Column(name = "SUPERVISOR_NOTIFIED_AT")
    private Instant supervisorNotifiedAt;
    @Column(name = "MANAGER_ESCALATED_AT")
    private Instant managerEscalatedAt;
    @Column(name = "SYSTEM_GROUP_ESCALATED_AT")
    private Instant systemGroupEscalatedAt;
    @Column(name = "DECIDED_AT")
    private Instant decidedAt;

    /**
     * Escala a solicitação para o próximo nível da cadeia, gravando o campo {@code *_ESCALATED_AT}
     * do nível alcançado e um novo {@code slaDeadline}. Não persiste.
     *
     * <p>{@code nextLevel=SUPERVISOR} nunca ocorre na prática - o único chamador
     * ({@code LoginApprovalEscalationJob}) sempre passa o resultado de
     * {@code LoginApprovalChainResolver.nextLevelAfter()}, que nunca devolve {@code SUPERVISOR}
     * (a cadeia só avança). {@code SUPERVISOR_NOTIFIED_AT} fica reservado para o Motor de
     * Notificação (Etapa 3/P2, fora de escopo) e não é escrito por este método.
     *
     * @throws ScosException SCOS_LOGIN_APPROVAL_REQUEST_002 se {@code status != PENDING}.
     */
    public void escalate(LoginApprovalRequestLevel nextLevel, Instant now, Instant newDeadline) {
        assertPending();
        this.currentLevel = nextLevel;
        if (nextLevel == LoginApprovalRequestLevel.MANAGER) {
            this.managerEscalatedAt = now;
        } else if (nextLevel == LoginApprovalRequestLevel.SYSTEM_ACCESS_GROUP) {
            this.systemGroupEscalatedAt = now;
        }
        this.slaDeadline = newDeadline;
    }

    /**
     * Aprova a solicitação. Não persiste.
     * @throws ScosException SCOS_LOGIN_APPROVAL_REQUEST_002 se {@code status != PENDING}.
     */
    public void approve(Login decidedByLogin, boolean isException, Instant now) {
        assertPending();
        this.status = LoginApprovalRequestStatus.APPROVED;
        this.decidedByLogin = decidedByLogin;
        this.decidedAt = now;
        this.isExceptionSelfApproval = isException;
    }

    /**
     * Rejeita a solicitação. Não persiste.
     * @throws ScosException SCOS_LOGIN_APPROVAL_REQUEST_002 se {@code status != PENDING}.
     */
    public void reject(Login decidedByLogin, boolean isException, Instant now) {
        assertPending();
        this.status = LoginApprovalRequestStatus.REJECTED;
        this.decidedByLogin = decidedByLogin;
        this.decidedAt = now;
        this.isExceptionSelfApproval = isException;
    }

    /**
     * Cancela automaticamente a solicitação por estouro do teto de 5 dias úteis desde a criação
     * (Story 3.5 AC 2) - sem {@code decidedByLogin}, ninguém decidiu. Não persiste.
     * @throws ScosException SCOS_LOGIN_APPROVAL_REQUEST_002 se {@code status != PENDING}.
     */
    public void cancel(Instant now) {
        assertPending();
        this.status = LoginApprovalRequestStatus.CANCELLED;
        this.decidedAt = now;
    }

    private void assertPending() {
        if (this.status != LoginApprovalRequestStatus.PENDING) {
            throw new ScosException(SCOS_LOGIN_APPROVAL_REQUEST_002);
        }
    }
}
