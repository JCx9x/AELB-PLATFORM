package es.aelb.backendweb.infrastructure.observability;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public final class ApplicationVersionInfoContributor implements InfoContributor {
    private final String version;
    private final String commit;
    private final String environment;

    public ApplicationVersionInfoContributor(
            @Value("${APP_VERSION:unknown}") String version,
            @Value("${GIT_COMMIT:unknown}") String commit,
            @Value("${APP_ENV:local}") String environment) {
        this.version = version; this.commit = commit; this.environment = environment;
    }

    @Override public void contribute(Info.Builder builder) {
        builder.withDetail("app", Map.of("name", "aelb-backend", "version", version,
                "commit", commit, "environment", environment));
    }
}
