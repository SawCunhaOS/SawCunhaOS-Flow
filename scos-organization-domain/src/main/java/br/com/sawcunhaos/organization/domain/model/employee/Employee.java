
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

package br.com.sawcunhaos.organization.domain.model.employee;


import br.com.sawcunhaos.foundation.utils.annotation.audit.Auditable;
import br.com.sawcunhaos.foundation.utils.entity.BaseEntity;
import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.foundation.utils.valueobjects.Cpf;
import br.com.sawcunhaos.foundation.utils.valueobjects.Email;
import br.com.sawcunhaos.organization.domain.exception.ExceptionCodeError;
import br.com.sawcunhaos.organization.domain.model.company.Company;
import br.com.sawcunhaos.organization.domain.model.department.Position;
import br.com.sawcunhaos.organization.domain.model.login.Login;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "SCOS_EMPLOYEE")
@Auditable(auditRead = true)
public class Employee extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EMPLOYEE_ID")
    private Long id;
    @Column(name = "NAME")
    private String name;
    @Column(name = "NAME_TREATMENT")
    private String nameTreatment;

    @Embedded
    @AttributeOverride(name = "cpf", column = @Column(name = "TAX_IDENTIFIER"))
    private Cpf taxIdentifier;
    @Embedded
    @AttributeOverride(name = "email", column = @Column(name = "EMAIL"))
    private Email email;
    @Column(name = "BIRTH_DATE")
    private LocalDate birthDate;
    @Column(name = "OBSERVATION")
    private String observation;
    @Column(name = "DATE_OF_HIRING")
    private LocalDate dateOfHiring;
    @Column(name = "STATUS")
    @Enumerated(EnumType.STRING)
    private StatusEmployee status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SUPERVISOR_ID")
    private Employee supervisor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "POSITION_ID")
    private Position position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COMPANY_ID")
    private Company company;

    @OneToMany(mappedBy = "employee", fetch = FetchType.LAZY)
    private Set<Login> login;

    public void activate() {
        if (this.status == StatusEmployee.DISABLED || this.status == StatusEmployee.DELETED) {
            throw new ScosException(ExceptionCodeError.SCOS_EMPLOYEE_001);
        }
        this.status = StatusEmployee.ACTIVE;
    }

    public void inactivate() {
        if (this.status == StatusEmployee.DISABLED || this.status == StatusEmployee.DELETED) {
            throw new ScosException(ExceptionCodeError.SCOS_EMPLOYEE_001);
        }
        this.status = StatusEmployee.INACTIVE;
    }

    public void disable() {
        this.status = StatusEmployee.DISABLED;
    }

    public void delete() {
        this.status = StatusEmployee.DELETED;
    }
}
