
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

package br.com.sawcunhaos.flow.infrastructure.database;

import br.com.sawcunhaos.foundation.core.specification.ScosUserAuthentication;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class ScosDatabaseApplicationName {

    private final JdbcTemplate jdbcTemplate;
    private final ScosUserAuthentication scosUserAuthentication;

    @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    public Object rastrear(ProceedingJoinPoint pjp) throws Throwable {
        String login = scosUserAuthentication.findUserAuthentication();
        if (login != null) {
            jdbcTemplate.execute("SET LOCAL application_name = '" + login.replace("'", "''") + "'");
        }
        return pjp.proceed();
    }
}
