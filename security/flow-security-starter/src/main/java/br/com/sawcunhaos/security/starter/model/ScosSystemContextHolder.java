
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

package br.com.sawcunhaos.security.starter.model;

import br.com.sawcunhaos.foundation.core.exception.ScosSecurityException;
import br.com.sawcunhaos.security.starter.utils.SecurityExceptionCode;

import java.util.Objects;

public final class ScosSystemContextHolder {

    private static volatile ScosSystemContext context;

    private ScosSystemContextHolder() {}

    public static ScosSystemContext get() {
        ScosSystemContext ctx = context;
        if (ctx == null) {
            throw new ScosSecurityException(SecurityExceptionCode.SCOS_AUTH_005);
        }
        return ctx;
    }

    public static boolean isInitialized() {
        return context != null;
    }

    public static void set(ScosSystemContext ctx) {
        context = Objects.requireNonNull(ctx);
    }
}
