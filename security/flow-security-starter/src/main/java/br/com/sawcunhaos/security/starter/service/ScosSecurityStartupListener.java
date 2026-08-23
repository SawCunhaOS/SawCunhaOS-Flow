package br.com.sawcunhaos.security.starter.service;

import br.com.sawcunhaos.foundation.spring.specification.ScosStartupListener;
import br.com.sawcunhaos.security.starter.specification.ScosSystemRegistration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;

@Slf4j
@RequiredArgsConstructor
public class ScosSecurityStartupListener implements ScosStartupListener {

    private final ScosSystemRegistration scosSystemRegistrationService;

    @Override
    public void onStartupSystem(ApplicationReadyEvent event) {
        log.info("Starting ScosSecurityStartupListener");

        scosSystemRegistrationService.register(1);

        log.info("Finished ScosSecurityStartupListener");
    }





}
