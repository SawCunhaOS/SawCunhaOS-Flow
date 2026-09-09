
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

package br.com.sawcunhaos.organization.domain.corporate.catalog.internal;

import br.com.sawcunhaos.flow.audit.sdk.api.AuditableEntity;
import br.com.sawcunhaos.foundation.jpa.entity.BaseEntity;
import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.organization.domain.access.status.internal.EntityType;
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

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_ADDRESS_TYPE_003;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_ADDRESS_TYPE_004;

/**
 * Catálogo de referência de tipos de endereço ({@code SCOS_ADDRESS_TYPE}), compartilhado entre
 * {@code CompanyAddress} e {@code EmployeeAddress}. Sem exclusão física — só ativação/inativação lógica.
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "SCOS_ADDRESS_TYPE")
@AuditableEntity
public class AddressType extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ADDRESS_TYPE_ID")
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

    /**
     * Ativa o tipo de endereço.
     * @throws ScosException SCOS_ADDRESS_TYPE_003 se já estiver ativo.
     */
    public void activate() {
        if (this.active) {
            throw new ScosException(SCOS_ADDRESS_TYPE_003);
        }
        this.active = true;
    }

    /**
     * Inativa o tipo de endereço.
     * @throws ScosException SCOS_ADDRESS_TYPE_004 se já estiver inativo.
     */
    public void deactivate() {
        if (!this.active) {
            throw new ScosException(SCOS_ADDRESS_TYPE_004);
        }
        this.active = false;
    }
}
