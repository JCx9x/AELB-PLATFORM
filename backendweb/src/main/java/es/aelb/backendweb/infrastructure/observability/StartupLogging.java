package es.aelb.backendweb.infrastructure.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public final class StartupLogging {
    private static final Logger log = LoggerFactory.getLogger(StartupLogging.class);
    private final String environment;
    private final String version;
    private final String commit;

    public StartupLogging(@Value("${APP_ENV:local}") String environment,
                          @Value("${APP_VERSION:unknown}") String version,
                          @Value("${GIT_COMMIT:unknown}") String commit) {
        this.environment = environment; this.version = version; this.commit = commit;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ready() {
        log.info("event=APPLICATION_READY service=aelb-backend environment={} version={} commit={}", environment, version, commit);
    }
}
