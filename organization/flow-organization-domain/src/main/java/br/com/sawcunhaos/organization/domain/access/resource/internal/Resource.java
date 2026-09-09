
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

package br.com.sawcunhaos.organization.domain.access.resource.internal;

import br.com.sawcunhaos.flow.audit.sdk.api.AuditableEntity;
import br.com.sawcunhaos.foundation.jpa.entity.BaseEntity;
import br.com.sawcunhaos.organization.domain.access.system.internal.ScosSystem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "SCOS_RESOURCE")
@AuditableEntity
public class Resource extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "RESOURCE_ID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SYSTEM_ID")
    private ScosSystem system;

    @Column(name = "CODE")
    private String code;
    @Column(name = "DESCRIPTION_PT")
    private String descriptionPt;
    @Column(name = "DESCRIPTION_EN")
    private String descriptionEn;
    @Column(name = "ACTIVE")
    private boolean active;
    @Column(name = "RESOURCE_GROUP")
    private String resourceGroup;
    @Column(name = "SUB_GROUP")
    private String subGroup;
    @Column(name = "VERSION")
    private String version;
    @Column(name = "DEFINITION_UPDATED_AT")
    private Instant definitionUpdatedAt;
}
