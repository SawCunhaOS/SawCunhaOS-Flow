
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

package br.com.sawcunhaos.organization.domain.access.status.internal;

import br.com.sawcunhaos.foundation.audit.api.Auditable;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.Company;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.StatusCompany;
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
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "SCOS_COMPANY_STATUS_HISTORY")
@Auditable
public class CompanyStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "COMPANY_STATUS_HISTORY_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COMPANY_ID")
    private Company company;

    @Column(name = "STATUS")
    @Enumerated(EnumType.STRING)
    private StatusCompany status;
    @Column(name = "PREVIOUS_STATUS")
    @Enumerated(EnumType.STRING)
    private StatusCompany previousStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REASON_ACTIVATE_ID")
    private ReasonActivate reasonActivate;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REASON_INACTIVATE_ID")
    private ReasonInactivate reasonInactivate;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REASON_DISABLE_ID")
    private ReasonDisable reasonDisable;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REASON_ENABLE_ID")
    private ReasonEnable reasonEnable;

    @Column(name = "OBSERVATION")
    private String observation;

    @CreationTimestamp
    @Column(name = "CREATED_AT")
    private Instant createdAt;
    @Column(name = "USER_AT")
    private String userAt;
}
