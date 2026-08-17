
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

package br.com.sawcunhaos.organization.domain.access.login.service;

import br.com.sawcunhaos.organization.domain.access.login.dto.AuthorityResponseOutput;
import br.com.sawcunhaos.organization.domain.access.login.internal.VwAuthorityResponse;
import br.com.sawcunhaos.organization.domain.access.login.internal.VwAuthorityResponseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/** {@link AuthorityResponseServiceBean#findAllByPermission(String)} - único método usado por FindSystemAccessApproversUseCase. */
@ExtendWith(MockitoExtension.class)
class AuthorityResponseServiceBeanTest {

    @Mock
    private VwAuthorityResponseRepository vwAuthorityResponseRepository;
    @Mock
    private AuthorityResponseMapper authorityResponseMapper;
    @Mock
    private LoginRolesService loginRolesService;

    @InjectMocks
    private AuthorityResponseServiceBean service;

    @Test
    void findAllByPermissionShouldMapEachRowFromRepository() {
        VwAuthorityResponse row = new VwAuthorityResponse();
        AuthorityResponseOutput output = AuthorityResponseOutput.builder().loginId(1L).login("supervisor.a").build();

        given(vwAuthorityResponseRepository.findAllByPermission("APPROVE_SYSTEM_ACCESS")).willReturn(List.of(row));
        given(authorityResponseMapper.toOutput(row)).willReturn(output);

        List<AuthorityResponseOutput> result = service.findAllByPermission("APPROVE_SYSTEM_ACCESS");

        assertThat(result).containsExactly(output);
        then(vwAuthorityResponseRepository).should().findAllByPermission("APPROVE_SYSTEM_ACCESS");
    }

    @Test
    void findAllByPermissionShouldReturnEmptyListWhenNoHolders() {
        given(vwAuthorityResponseRepository.findAllByPermission("APPROVE_SYSTEM_ACCESS")).willReturn(List.of());

        List<AuthorityResponseOutput> result = service.findAllByPermission("APPROVE_SYSTEM_ACCESS");

        assertThat(result).isEmpty();
    }
}
