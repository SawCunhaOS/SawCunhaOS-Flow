
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

package br.com.sawcunhaos.organization.domain.corporate.employee.service;

import br.com.sawcunhaos.organization.domain.corporate.employee.internal.EmployeeQueryRepository;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.StatusEmployee;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeePositionQueryServiceBeanTest {

    @Mock
    private EmployeeQueryRepository employeeQueryRepository;

    @InjectMocks
    private EmployeePositionQueryServiceBean employeePositionQueryServiceBean;

    @Test
    void shouldReturnTrueWhenActiveEmployeeIsLinkedToPosition() {
        when(employeeQueryRepository.existsByPositionIdAndStatus(10L, StatusEmployee.ACTIVE)).thenReturn(true);

        boolean result = employeePositionQueryServiceBean.existsActiveEmployeeInPosition(10L);

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseWhenNoActiveEmployeeIsLinkedToPosition() {
        when(employeeQueryRepository.existsByPositionIdAndStatus(10L, StatusEmployee.ACTIVE)).thenReturn(false);

        boolean result = employeePositionQueryServiceBean.existsActiveEmployeeInPosition(10L);

        assertThat(result).isFalse();
    }
}
