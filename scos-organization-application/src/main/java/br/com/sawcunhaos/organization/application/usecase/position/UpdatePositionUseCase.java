
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

package br.com.sawcunhaos.organization.application.usecase.position;

import br.com.sawcunhaos.foundation.exception.error.ScosNoRollbackException;
import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.foundation.utils.specification.ScosBaseUseCase;
import br.com.sawcunhaos.foundation.utils.specification.ScosUserAuthentication;
import br.com.sawcunhaos.organization.application.dto.UpdatePositionDTO;
import br.com.sawcunhaos.organization.application.mapper.position.PositionMapper;
import br.com.sawcunhaos.organization.domain.model.department.Position;
import br.com.sawcunhaos.organization.domain.repository.department.PositionRepository;
import br.com.sawcunhaos.organization.domain.service.department.PositionDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static br.com.sawcunhaos.organization.domain.exception.ExceptionCodeError.SCOS_POSITION_001;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class, noRollbackFor = ScosNoRollbackException.class)
public class UpdatePositionUseCase implements ScosBaseUseCase<UpdatePositionDTO, Void> {

    private final PositionRepository positionRepository;
    private final PositionDomainService positionDomainService;
    private final PositionMapper positionMapper;
    private final ScosUserAuthentication scosUserAuthentication;

    @Override
    public Void execute(UpdatePositionDTO updatePositionDTO) {
        log.info("Updating position: {}", updatePositionDTO);

        // Validate position exists
        positionDomainService.validatePositionExistsValidation(updatePositionDTO.getPositionId());

        // Validate department exists and is active
        positionDomainService.validateDepartmentExistsAndActiveValidation(updatePositionDTO.getDepartmentId());

        // Validate code uniqueness (excluding current position)
        positionDomainService.validatePositionCodeUniquenessValidation(updatePositionDTO.getCode(), updatePositionDTO.getPositionId());

        Position position = positionMapper.toPosition(updatePositionDTO);
        Position positionUpdated = positionRepository.findById(position.getId()).orElseThrow(() -> new ScosException(SCOS_POSITION_001));

        positionUpdated.setCode(position.getCode());
        positionUpdated.setDescription(position.getDescription());
        positionUpdated.setDepartment(position.getDepartment());
        positionUpdated.updateAuditInfo(scosUserAuthentication.findUserAuthentication());

        positionRepository.update(positionUpdated);
        log.info("Position updated: {}", updatePositionDTO.getPositionId());
        return null;
    }
}

