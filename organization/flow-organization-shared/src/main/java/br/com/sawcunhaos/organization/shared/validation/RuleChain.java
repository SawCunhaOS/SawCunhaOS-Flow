
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

package br.com.sawcunhaos.organization.shared.validation;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError;

import java.util.List;

public final class RuleChain<T> {
    private final List<? extends BusinessRule<T>> rules;

    private RuleChain(List<? extends BusinessRule<T>> rules) { this.rules = rules; }

    public static <T> RuleChain<T> of(List<? extends BusinessRule<T>> rules) {
        return new RuleChain<>(rules);
    }

    public void checkFirst(T context) {
        rules.stream().flatMap(r -> r.validate(context).stream())
                .findFirst()
                .ifPresent(reason -> { throw new ScosException(ExceptionCodeError.valueOf(reason)); });
    }

    public List<String> checkAll(T context) {
        return rules.stream().flatMap(r -> r.validate(context).stream()).toList();
    }
}
