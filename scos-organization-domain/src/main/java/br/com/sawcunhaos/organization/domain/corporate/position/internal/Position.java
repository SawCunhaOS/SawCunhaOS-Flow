
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

package br.com.sawcunhaos.organization.domain.corporate.position.internal;

import br.com.sawcunhaos.foundation.utils.annotation.audit.Auditable;
import br.com.sawcunhaos.foundation.utils.entity.BaseEntity;
import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.domain.corporate.department.internal.Department;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_POSITION_004;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_POSITION_005;


@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "SCOS_POSITION")
@Auditable
public class Position extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "POSITION_ID")
    private Long id;
    @Column(name = "CODE", nullable = false)
    private String code;
    @Column(name = "DESCRIPTION", nullable = false)
    private String description;
    @Builder.Default
    @Column(name = "ACTIVE", nullable = false)
    private boolean active = true;
    @Builder.Default
    @Column(name = "IS_TRUST_POSITION", nullable = false)
    private boolean isTrustPosition = false;

    @ManyToOne
    @JoinColumn(name = "DEPARTMENT_ID")
    private Department department;

    /**
     * Ativa a position.
     * @throws ScosException SCOS_POSITION_004 se já estiver ativa.
     */
    public void activate() {
        if (this.active) {
            throw new ScosException(SCOS_POSITION_004);
        }
        this.active = true;
    }

    /**
     * Inativa a position.
     * @throws ScosException SCOS_POSITION_005 se já estiver inativa.
     */
    public void deactivate() {
        if (!this.active) {
            throw new ScosException(SCOS_POSITION_005);
        }
        this.active = false;
    }

}
