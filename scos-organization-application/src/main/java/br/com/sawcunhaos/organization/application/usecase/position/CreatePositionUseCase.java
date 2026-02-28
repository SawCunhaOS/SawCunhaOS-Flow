
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
import br.com.sawcunhaos.organization.application.dto.CreatePositionDTO;
import br.com.sawcunhaos.organization.application.mapper.position.PositionMapper;
import br.com.sawcunhaos.organization.domain.model.department.Position;
import br.com.sawcunhaos.organization.domain.repository.department.PositionRepository;
import br.com.sawcunhaos.organization.domain.service.department.PositionDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class, noRollbackFor = ScosNoRollbackException.class)
public class CreatePositionUseCase implements ScosBaseUseCase<CreatePositionDTO, Long> {

    private final PositionRepository positionRepository;
    private final PositionDomainService positionDomainService;
    private final PositionMapper positionMapper;
    private final ScosUserAuthentication scosUserAuthentication;

    @Override
    public Long execute(CreatePositionDTO createPositionDTO) {
        log.info("Creating position: {}", createPositionDTO);

        // Validate department exists and is active
        positionDomainService.validateDepartmentExistsAndActiveValidation(createPositionDTO.getDepartmentId());

        // Validate position code uniqueness
        positionDomainService.validatePositionCodeExistsValidation(createPositionDTO.getCode());

        Position position = positionMapper.toPosition(createPositionDTO);
        position.updateAuditInfo(scosUserAuthentication.findUserAuthentication());

        return positionRepository.persist(position).getId();
    }
}

