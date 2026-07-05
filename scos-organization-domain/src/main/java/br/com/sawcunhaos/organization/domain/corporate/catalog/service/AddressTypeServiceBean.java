
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

package br.com.sawcunhaos.organization.domain.corporate.catalog.service;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.foundation.utils.specification.ScosUserAuthentication;
import br.com.sawcunhaos.organization.domain.access.status.internal.EntityType;
import br.com.sawcunhaos.organization.domain.corporate.catalog.dto.AddressTypeInput;
import br.com.sawcunhaos.organization.domain.corporate.catalog.dto.AddressTypeOutput;
import br.com.sawcunhaos.organization.domain.corporate.catalog.internal.AddressType;
import br.com.sawcunhaos.organization.domain.corporate.catalog.internal.AddressTypeRepository;
import br.com.sawcunhaos.organization.domain.corporate.catalog.specification.AddressTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_ADDRESS_TYPE_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_ADDRESS_TYPE_002;

/**
 * Implementação de {@link AddressTypeService} — regras de unicidade de {@code code} por {@code entityType}
 * e idempotência de ativação/inativação ficam no agregado {@link AddressType}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AddressTypeServiceBean implements AddressTypeService {

    private final AddressTypeRepository addressTypeRepository;
    private final AddressTypeMapper addressTypeMapper;
    private final ScosUserAuthentication scosUserAuthentication;

    /**
     * @throws ScosException SCOS_ADDRESS_TYPE_002 se já existir um {@link AddressType} com o mesmo {@code code}/{@code entityType}.
     */
    @Override
    public AddressTypeOutput create(@NonNull AddressTypeInput addressTypeInput) {
        log.info("Create AddressType: {}", addressTypeInput.code());

        if (addressTypeRepository.existsByCodeAndEntityType(addressTypeInput.code(), addressTypeInput.entityType())) {
            throw new ScosException(SCOS_ADDRESS_TYPE_002);
        }

        AddressType addressType = AddressType.builder()
                .code(addressTypeInput.code())
                .description(addressTypeInput.description())
                .entityType(addressTypeInput.entityType())
                .active(true)
                .build();
        addressType.updateAuditInfo(scosUserAuthentication.findUserAuthentication());

        addressType = addressTypeRepository.merge(addressType);

        return AddressTypeOutput.builder()
                .id(addressType.getId())
                .code(addressType.getCode())
                .description(addressType.getDescription())
                .entityType(addressType.getEntityType())
                .active(addressType.isActive())
                .build();
    }

    /**
     * @throws ScosException SCOS_ADDRESS_TYPE_001 se o {@code id} não existir.
     * @throws ScosException SCOS_ADDRESS_TYPE_002 se o novo {@code code}/{@code entityType} colidir com outro registro.
     */
    @Override
    public void update(@NonNull AddressTypeInput addressTypeInput) {
        log.info("Update AddressType: {}", addressTypeInput.id());
        AddressType addressType = findAddressTypeById(addressTypeInput.id());

        if (addressTypeRepository.existsByCodeAndEntityTypeAndNotId(addressTypeInput.code(), addressTypeInput.entityType(), addressTypeInput.id())) {
            throw new ScosException(SCOS_ADDRESS_TYPE_002);
        }

        addressType.setCode(addressTypeInput.code());
        addressType.setDescription(addressTypeInput.description());
        addressType.setEntityType(addressTypeInput.entityType());
        addressType.updateAuditInfo(scosUserAuthentication.findUserAuthentication());
        addressTypeRepository.update(addressType);
    }

    /**
     * @throws ScosException SCOS_ADDRESS_TYPE_001 se o {@code id} não existir.
     */
    @Override
    public AddressTypeOutput findById(@NonNull Long addressTypeId) {
        log.info("Find AddressType by Id: {}", addressTypeId);
        AddressType addressType = findAddressTypeById(addressTypeId);
        return addressTypeMapper.toAddressTypeOutput(addressType);
    }

    /**
     * Lista paginada, filtrando por {@code entityType} quando informado.
     */
    @Override
    public Page<AddressTypeOutput> findAll(EntityType entityType, @NonNull Pageable pageable) {
        log.info("Find All AddressTypes, EntityType: {}", entityType);
        return addressTypeRepository.findAllFiltered(entityType, pageable)
                .map(addressTypeMapper::toAddressTypeOutput);
    }

    /**
     * @throws ScosException SCOS_ADDRESS_TYPE_001 se o {@code id} não existir.
     * @throws ScosException SCOS_ADDRESS_TYPE_003 se já estiver ativo.
     */
    @Override
    public void enable(@NonNull Long addressTypeId) {
        log.info("Enable AddressType: {}", addressTypeId);
        AddressType addressType = findAddressTypeById(addressTypeId);
        addressType.activate();

        addressTypeRepository.update(addressType);
        log.info("AddressType enabled");
    }

    /**
     * @throws ScosException SCOS_ADDRESS_TYPE_001 se o {@code id} não existir.
     * @throws ScosException SCOS_ADDRESS_TYPE_004 se já estiver inativo.
     */
    @Override
    public void disable(@NonNull Long addressTypeId) {
        log.info("Disable AddressType: {}", addressTypeId);
        AddressType addressType = findAddressTypeById(addressTypeId);
        addressType.deactivate();

        addressTypeRepository.update(addressType);
        log.info("AddressType disabled");
    }

    private AddressType findAddressTypeById(@NonNull Long addressTypeId) {
        log.info("Find AddressType by Id: {}", addressTypeId);
        return addressTypeRepository.findById(addressTypeId).orElseThrow(
                () -> new ScosException(SCOS_ADDRESS_TYPE_001)
        );
    }
}
