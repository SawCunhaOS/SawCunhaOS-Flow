
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

package br.com.sawcunhaos.organization.domain.model.company;


import br.com.sawcunhaos.foundation.utils.annotation.audit.Auditable;
import br.com.sawcunhaos.foundation.utils.entity.BaseEntity;
import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.foundation.utils.valueobjects.Cnpj;
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
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static br.com.sawcunhaos.organization.domain.exception.ExceptionCodeError.SCOS_COMPANY_007;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "SCOS_COMPANY")
@Auditable
public class Company extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "COMPANY_ID")
    private Long id;

    @Column(name = "NAME")
    private String name;
    @Column(name = "NAME_TREATMENT")
    private String nameTreatment;

    @Embedded
    @AttributeOverride(name = "cnpj", column = @Column(name = "TAX_IDENTIFIER"))
    private Cnpj taxIdentifier;
    @Column(name = "FOUNDATION_DATE")
    private LocalDate foundationDate;
    @Column(name = "SECTOR_OF_ACTIVITY")
    private String sectorOfActivity;
    @Column(name = "OBSERVATION")
    private String observation;
    @Column(name = "DATE_CREATED")
    private LocalDate dateCreated;
    @Column(name = "STATUS")
    @Enumerated(EnumType.STRING)
    private StatusCompany status;
    @Column(name = "ACTIVE")
    private boolean active;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_COMPANY_ID")
    private Company parentCompany;

    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY)
    private Set<CompanyContact> companyContacts = new HashSet<>();

    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY)
    private Set<CompanyAddress> companyAddresses = new HashSet<>();

    // Métodos de negócio
    /**
     * Verifica se a empresa é uma matriz (não tem empresa-mãe)
     */
    public boolean isMatrix() {
        return parentCompany == null;
    }

    /**
     * Verifica se a empresa está ativa
     */
    public boolean isActive() {
        return StatusCompany.ACTIVE == this.status && this.active;
    }

    /**
     * Inativa a empresa
     */
    public void inactivate() {
        if (this.status == StatusCompany.DELETED) {
            throw new ScosException(SCOS_COMPANY_007);
        }
        this.active = false;
        this.status = StatusCompany.INACTIVE;
    }

    /**
     * Ativa a empresa
     */
    public void activate() {
        if (this.status == StatusCompany.DELETED) {
            throw new ScosException(SCOS_COMPANY_007);
        }
        this.active = true;
        this.status = StatusCompany.ACTIVE;
    }

    /**
     * Desativa a empresa (soft-delete)
     */
    public void delete() {
        this.active = false;
        this.status = StatusCompany.DELETED;
    }

    /**
     * Define a data de criação se ainda não estiver definida.
     * Deve ser chamado antes de persistir a entidade.
     */
    public void defineDateCreated() {
        if (Objects.isNull(this.dateCreated)) {
            this.dateCreated = LocalDate.now();
        }
    }
}
