
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

package br.com.sawcunhaos.organization.domain.model.department;

import br.com.sawcunhaos.foundation.utils.annotation.audit.Auditable;
import br.com.sawcunhaos.foundation.utils.entity.BaseEntity;
import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

import static br.com.sawcunhaos.organization.domain.exception.ExceptionCodeError.SCOS_DEPARTMENT_004;
import static br.com.sawcunhaos.organization.domain.exception.ExceptionCodeError.SCOS_DEPARTMENT_005;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "SCOS_DEPARTMENT")
@Auditable
public class Department extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DEPARTMENT_ID")
    private Long id;
    @Column(name = "CODE", nullable = false)
    private String code;
    @Column(name = "DESCRIPTION", nullable = false)
    private String description;
    @Builder.Default
    @Column(name = "ACTIVE", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    private Set<Position> positions;

    /**
     * Ativa o department.
     * @throws ScosException SCOS_DEPARTMENT_004 se já estiver ativo.
     */
    public void activate() {
        if (this.active) {
            throw new ScosException(SCOS_DEPARTMENT_004);
        }
        this.active = true;
    }

    /**
     * Inativa o department.
     * @throws ScosException SCOS_DEPARTMENT_005 se já estiver inativo.
     */
    public void deactivate() {
        if (!this.active) {
            throw new ScosException(SCOS_DEPARTMENT_005);
        }
        this.active = false;
    }

}
