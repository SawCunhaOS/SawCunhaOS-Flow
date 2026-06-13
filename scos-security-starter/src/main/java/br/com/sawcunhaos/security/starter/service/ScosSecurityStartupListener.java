package br.com.sawcunhaos.security.starter.service;

import br.com.sawcunhaos.foundation.utils.specification.ScosStartupListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;

@Slf4j
@RequiredArgsConstructor
public class ScosSecurityStartupListener implements ScosStartupListener {

    private final ScosSystemRegistrationService scosSystemRegistrationService;

    @Override
    public void onStartupSystem(ApplicationReadyEvent event) {
        log.info("Starting ScosSecurityStartupListener");

        scosSystemRegistrationService.register(1);

        log.info("Finished ScosSecurityStartupListener");
    }





}
