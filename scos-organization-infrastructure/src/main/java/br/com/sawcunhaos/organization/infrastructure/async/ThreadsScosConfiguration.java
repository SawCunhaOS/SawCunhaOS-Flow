package br.com.sawcunhaos.organization.infrastructure.async;

import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration(proxyBeanMethods = false)
public final class ThreadsScosConfiguration {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatFactoryCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector -> {
            var virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
            connector.getProtocolHandler().setExecutor(virtualThreadExecutor);
        });
    }

    @Bean
    public Executor virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean(name = "ScosAsyncExecutor")
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(100);
        scheduler.setThreadNamePrefix("vt-scos-scheduler-");
        scheduler.setVirtualThreads(true);
        return scheduler;
    }
}
