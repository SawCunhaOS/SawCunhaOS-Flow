
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

import br.com.sawcunhaos.foundation.utils.annotation.rules.ScosRuleService;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginStatus;
import br.com.sawcunhaos.organization.shared.validation.BusinessRule;
import br.com.sawcunhaos.organization.shared.validation.RuleChain;

import java.util.List;

@ScosRuleService
public class LoginRolesService {

    private final RuleChain<LoginStatus> ruleRuleChain;

    public LoginRolesService(List<BusinessRule<LoginStatus>> rules) {
        this.ruleRuleChain = RuleChain.of(rules);
    }

    public void validateStatusLogin(LoginStatus status) {
        ruleRuleChain.checkFirst(status);
    }

}
