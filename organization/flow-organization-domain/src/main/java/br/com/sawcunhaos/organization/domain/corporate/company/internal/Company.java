
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

package br.com.sawcunhaos.organization.domain.corporate.company.internal;


import br.com.sawcunhaos.foundation.utils.annotation.audit.Auditable;
import br.com.sawcunhaos.foundation.utils.entity.BaseEntity;
import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.foundation.utils.valueobjects.Cnpj;
import br.com.sawcunhaos.organization.domain.access.status.internal.CompanyStatusHistory;
import br.com.sawcunhaos.organization.domain.access.status.internal.ReasonActivate;
import br.com.sawcunhaos.organization.domain.access.status.internal.ReasonDisable;
import br.com.sawcunhaos.organization.domain.access.status.internal.ReasonEnable;
import br.com.sawcunhaos.organization.domain.access.status.internal.ReasonInactivate;
import br.com.sawcunhaos.organization.shared.converter.ZoneIdConverter;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_COMPANY_007;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "SCOS_COMPANY")
@Auditable
public class Company extends BaseEntity {

    public static final ZoneId DEFAULT_TIME_ZONE = ZoneId.of("America/Sao_Paulo");

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
    @Column(name = "STATUS")
    @Enumerated(EnumType.STRING)
    private StatusCompany status;

    @Column(name = "STATE_REGISTRATION")
    private String stateRegistration;
    @Column(name = "MUNICIPAL_REGISTRATION")
    private String municipalRegistration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LEGAL_NATURE_ID")
    private LegalNature legalNature;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CNAE_PRINCIPAL_ID")
    private Cnae cnaePrincipal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_COMPANY_ID")
    private Company parentCompany;

    /**
     * Fuso horário efetivo desta empresa (IANA). Imutável após a criação — decisão de
     * produto D3 (2026-07-19): alterar reinterpretaria decisões de bloqueio de turno já
     * auditadas. Nenhum Use Case/endpoint desta Etapa expõe alteração; não adicionar um
     * "update timezone" sem revisitar essa decisão.
     */
    @Convert(converter = ZoneIdConverter.class)
    @Column(name = "TIME_ZONE")
    @Builder.Default
    private ZoneId timeZone = DEFAULT_TIME_ZONE;

    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY)
    private Set<CompanyContact> companyContacts = new HashSet<>();

    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY)
    private Set<CompanyAddress> companyAddresses = new HashSet<>();

    public boolean isMatrix() {
        return parentCompany == null;
    }

    public boolean isActive() {
        return StatusCompany.ACTIVE == this.status;
    }

    /**
     * Reativa a empresa a partir de INACTIVE. Não persiste — o chamador salva o histórico retornado.
     * @throws ScosException SCOS_COMPANY_007 se o status atual não for INACTIVE.
     */
    public CompanyStatusHistory activate(Long reasonActivateId) {
        if (this.status != StatusCompany.INACTIVE) {
            throw new ScosException(SCOS_COMPANY_007);
        }
        return CompanyStatusHistory.builder()
                .company(this)
                .status(StatusCompany.ACTIVE)
                .reasonActivate(ReasonActivate.builder().id(reasonActivateId).build())
                .build();
    }

    /**
     * Encerra definitivamente a empresa a partir de ACTIVE ou DISABLED. Não persiste.
     * @throws ScosException SCOS_COMPANY_007 se já estiver INACTIVE.
     */
    public CompanyStatusHistory inactivate(Long reasonInactivateId) {
        if (this.status == StatusCompany.INACTIVE) {
            throw new ScosException(SCOS_COMPANY_007);
        }
        return CompanyStatusHistory.builder()
                .company(this)
                .status(StatusCompany.INACTIVE)
                .reasonInactivate(ReasonInactivate.builder().id(reasonInactivateId).build())
                .build();
    }

    /**
     * Bloqueia temporariamente a empresa a partir de ACTIVE. Não persiste.
     * @throws ScosException SCOS_COMPANY_007 se o status atual não for ACTIVE.
     */
    public CompanyStatusHistory disable(Long reasonDisableId) {
        if (this.status != StatusCompany.ACTIVE) {
            throw new ScosException(SCOS_COMPANY_007);
        }
        return CompanyStatusHistory.builder()
                .company(this)
                .status(StatusCompany.DISABLED)
                .reasonDisable(ReasonDisable.builder().id(reasonDisableId).build())
                .build();
    }

    /**
     * Desbloqueia a empresa a partir de DISABLED, retornando a ACTIVE. Não persiste.
     * @throws ScosException SCOS_COMPANY_007 se o status atual não for DISABLED.
     */
    public CompanyStatusHistory enable(Long reasonEnableId) {
        if (this.status != StatusCompany.DISABLED) {
            throw new ScosException(SCOS_COMPANY_007);
        }
        return CompanyStatusHistory.builder()
                .company(this)
                .status(StatusCompany.ACTIVE)
                .reasonEnable(ReasonEnable.builder().id(reasonEnableId).build())
                .build();
    }
}
