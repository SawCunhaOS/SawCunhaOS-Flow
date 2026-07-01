
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

import br.com.sawcunhaos.foundation.utils.annotation.audit.Auditable;
import br.com.sawcunhaos.foundation.utils.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "SCOS_REASON_ENABLE")
@Auditable
public class ReasonEnable extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "REASON_ENABLE_ID")
    private Long id;
    @Column(name = "CODE")
    private String code;
    @Column(name = "DESCRIPTION")
    private String description;
    @Column(name = "ENTITY_TYPE")
    @Enumerated(EnumType.STRING)
    private EntityType entityType;
    @Builder.Default
    @Column(name = "ACTIVE")
    private boolean active = true;
}
