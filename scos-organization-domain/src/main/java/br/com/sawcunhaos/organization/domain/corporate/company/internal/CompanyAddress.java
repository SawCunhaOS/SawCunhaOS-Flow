
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
import br.com.sawcunhaos.organization.domain.corporate.catalog.internal.AddressType;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "SCOS_COMPANY_ADDRESS")
@Auditable
public class CompanyAddress extends BaseEntity {

    @EmbeddedId
    private CompanyAddressPk id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ADDRESS_TYPE_ID")
    private AddressType addressType;
    @Column(name = "NUMBER")
    private int number;
    @Column(name = "COMPLEMENT")
    private String complement;
    @Column(name = "GEOLOCATION")
    private Point geolocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COMPANY_ID", insertable = false, updatable = false)
    private Company company;
}
