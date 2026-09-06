package in.gov.ipie.service.iam.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import in.gov.ipie.common.audit.annotation.Auditable;
import in.gov.ipie.common.audit.model.AuditEventType;
import in.gov.ipie.common.security.keycloak.directory.AccountDirectory;
import in.gov.ipie.service.iam.command.ProvisionAccountCommand;

/**
 * {@link AccountProvisioningService} implementation - writes through {@link AccountDirectory},
 * the platform port whose one adapter is the same Keycloak client {@link RoleServiceImpl} uses for
 * role sync, so no new Keycloak-client bean wiring was needed in this service to add this.
 *
 * <p>Depends on the port and not on that adapter: this class needs to create an account and stamp
 * an attribute on it, and nothing here should compile against realm-role management (see
 * {@code RealmRoleDirectory}) or against an HTTP client's transport concerns.
 */
@Service
public class AccountProvisioningServiceImpl implements AccountProvisioningService {

    private final AccountDirectory accountDirectory;

    public AccountProvisioningServiceImpl(AccountDirectory accountDirectory) {
        this.accountDirectory = accountDirectory;
    }

    @Override
    @Auditable(
            action = "ACCOUNT_PROVISIONED", entityType = "KEYCLOAK_ACCOUNT", entityId = "#result", eventType = AuditEventType.BUSINESS,
            newValue = "#command")
    public UUID provisionAccount(ProvisionAccountCommand command) {
        UUID keycloakUserId = accountDirectory.createUser(
                command.username(), command.email(), command.firstName(), command.lastName(), command.password());
        // Same two-call sequence UserServiceImpl.completeRegistration used to perform directly -
        // makes ipie_id a reliable claim on every token this realm issues from this point on (see
        // the ipie-identity client scope).
        accountDirectory.setUserAttribute(keycloakUserId, "ipie_id", command.ipieUserId().toString());
        return keycloakUserId;
    }

}
