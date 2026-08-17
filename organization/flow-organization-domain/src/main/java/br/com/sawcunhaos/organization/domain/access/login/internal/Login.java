
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

import br.com.sawcunhaos.foundation.utils.annotation.audit.Auditable;
import br.com.sawcunhaos.foundation.utils.entity.BaseEntity;
import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.domain.access.status.internal.LoginStatusHistory;
import br.com.sawcunhaos.organization.domain.access.status.internal.ReasonActivate;
import br.com.sawcunhaos.organization.domain.access.status.internal.ReasonDisable;
import br.com.sawcunhaos.organization.domain.access.status.internal.ReasonEnable;
import br.com.sawcunhaos.organization.domain.access.status.internal.ReasonInactivate;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.Employee;
import br.com.sawcunhaos.organization.domain.access.profile.internal.Profile;
import br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError;
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
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "SCOS_LOGIN")
@Auditable(auditRead = true)
public class Login extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LOGIN_ID")
    private Long id;

    @Column(name = "EXTERNAL_ID")
    private UUID externalId;
    @Column(name = "LOGIN")
    private String login;
    @Column(name = "STATUS")
    @Enumerated(EnumType.STRING)
    private LoginStatus status;
    @Column(name = "TYPE")
    @Enumerated(EnumType.STRING)
    private LoginType type;
    @Column(name = "LAST_USED_AT")
    private Instant lastUsedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROFILE_ID")
    private Profile profile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EMPLOYEE_ID")
    private Employee employee;

    /**
     * Reativa o login a partir de INACTIVE. Não persiste — o chamador salva o histórico retornado.
     * @throws ScosException SCOS_LOGIN_013 se o status atual não for INACTIVE.
     */
    public LoginStatusHistory activate(Long reasonActivateId) {
        if (this.status != LoginStatus.INACTIVE) {
            throw new ScosException(ExceptionCodeError.SCOS_LOGIN_013);
        }
        return LoginStatusHistory.builder()
                .login(this)
                .status(LoginStatus.ACTIVE)
                .reasonActivate(ReasonActivate.builder().id(reasonActivateId).build())
                .build();
    }

    /**
     * Encerra definitivamente o login a partir de ACTIVE ou BLOCKED. Não persiste.
     * @throws ScosException se já estiver INACTIVE.
     */
    public LoginStatusHistory inactivate(Long reasonInactivateId) {
        if (this.status == LoginStatus.INACTIVE) {
            throw new ScosException(ExceptionCodeError.SCOS_LOGIN_013);
        }
        return LoginStatusHistory.builder()
                .login(this)
                .status(LoginStatus.INACTIVE)
                .reasonInactivate(ReasonInactivate.builder().id(reasonInactivateId).build())
                .build();
    }

    /**
     * Bloqueia temporariamente o login a partir de ACTIVE. Não persiste.
     * @throws ScosException se o status atual não for ACTIVE.
     */
    public LoginStatusHistory disable(Long reasonDisableId) {
        if (this.status != LoginStatus.ACTIVE) {
            throw new ScosException(ExceptionCodeError.SCOS_LOGIN_013);
        }
        return LoginStatusHistory.builder()
                .login(this)
                .status(LoginStatus.BLOCKED)
                .reasonDisable(ReasonDisable.builder().id(reasonDisableId).build())
                .build();
    }

    /**
     * Desbloqueia o login a partir de BLOCKED, retornando a ACTIVE. Não persiste.
     * @throws ScosException SCOS_LOGIN_013 se o status atual não for BLOCKED.
     */
    public LoginStatusHistory enable(Long reasonEnableId) {
        if (this.status != LoginStatus.BLOCKED) {
            throw new ScosException(ExceptionCodeError.SCOS_LOGIN_013);
        }
        return LoginStatusHistory.builder()
                .login(this)
                .status(LoginStatus.ACTIVE)
                .reasonEnable(ReasonEnable.builder().id(reasonEnableId).build())
                .build();
    }

    /**
     * Aprova o login a partir de PENDING_APPROVAL. Não persiste.
     * @throws ScosException SCOS_LOGIN_013 se o status atual não for PENDING_APPROVAL.
     */
    public LoginStatusHistory approve(Long reasonActivateId) {
        if (this.status != LoginStatus.PENDING_APPROVAL) {
            throw new ScosException(ExceptionCodeError.SCOS_LOGIN_013);
        }
        return LoginStatusHistory.builder()
                .login(this)
                .status(LoginStatus.ACTIVE)
                .reasonActivate(ReasonActivate.builder().id(reasonActivateId).build())
                .build();
    }

    /**
     * Rejeita o login a partir de PENDING_APPROVAL. Estado terminal, sem reversão. Não persiste.
     * @throws ScosException SCOS_LOGIN_013 se o status atual não for PENDING_APPROVAL.
     */
    public LoginStatusHistory reject(Long reasonInactivateId) {
        if (this.status != LoginStatus.PENDING_APPROVAL) {
            throw new ScosException(ExceptionCodeError.SCOS_LOGIN_013);
        }
        return LoginStatusHistory.builder()
                .login(this)
                .status(LoginStatus.REJECTED)
                .reasonInactivate(ReasonInactivate.builder().id(reasonInactivateId).build())
                .build();
    }
}
