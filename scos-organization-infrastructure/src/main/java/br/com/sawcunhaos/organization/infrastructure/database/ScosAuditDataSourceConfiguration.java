package br.com.sawcunhaos.organization.infrastructure.database;

import br.com.sawcunhaos.foundation.audit.service.ScosHibernateAuditListener;
import br.com.sawcunhaos.foundation.audit.specification.ScosAuditService;
import br.com.sawcunhaos.foundation.utils.specification.ScosUserAuthentication;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration(proxyBeanMethods = false)
@DependsOn("ScosEntityManagerFactory")
@ConditionalOnProperty(prefix="SawCunhaOS.audit", name = "enable", havingValue = "true")
final class ScosAuditDataSourceConfiguration {

    @Autowired
    private ScosAuditService insideAuditService;
    @Autowired
    private ScosUserAuthentication scosUserAuthentication;
    @Autowired
    @Qualifier("ScosEntityManagerFactory")
    private EntityManagerFactory entityManagerFactory;

    @PostConstruct
    public void registerListeners() {
        SessionFactoryImpl sessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl.class);
        EventListenerRegistry registry = sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);

        ScosHibernateAuditListener listener = new ScosHibernateAuditListener(insideAuditService, scosUserAuthentication);

        registry.getEventListenerGroup(EventType.POST_INSERT).appendListener(listener);
        registry.getEventListenerGroup(EventType.POST_UPDATE).appendListener(listener);
        registry.getEventListenerGroup(EventType.POST_DELETE).appendListener(listener);
    }
}
