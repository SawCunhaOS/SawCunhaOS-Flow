
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

package br.com.sawcunhaos.flow.infrastructure.message;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

@Configuration
class MessageConfiguration {

	@Bean
	public MessageSource messageSource() {
		ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
		messageSource.setBasenames(
			"classpath:messages",
            "classpath:scos_message_validation",
			"classpath:messages_exception",
			"classpath:messages_security",
            "classpath:scos_message_organization",
            "classpath:scos_utils_messages",
            "classpath:key_configuration_description",
            "classpath:messages_permission"
		);
		messageSource.setDefaultEncoding("UTF-8");
		messageSource.setCacheSeconds(360);
		messageSource.setFallbackToSystemLocale(false);
		return messageSource;
	}

}
