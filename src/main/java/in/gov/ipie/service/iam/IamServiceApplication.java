package in.gov.ipie.service.iam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Owns RBAC/ABAC as the source of truth (roles, permissions, role_permissions, user_roles),
 * syncing role definitions and assignments to Keycloak realm roles - see {@code
 * application.service.RoleService}. Auto-assigns the default {@code STAKEHOLDER} role when
 * ipie-user-service publishes {@code USER_VERIFIED} - see {@code
 * infrastructure.messaging.consumer.UserVerifiedEventConsumer}.
 *
 * <p>{@code @EnableScheduling} drives {@code OutboxRelayScheduler} - the transactional outbox
 * relay (master standards doc, section 9) - not any business-specific scheduled job.
 */
@SpringBootApplication
@EnableScheduling
public class IamServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IamServiceApplication.class, args);
    }
}
