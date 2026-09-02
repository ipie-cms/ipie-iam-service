package in.gov.ipie.service.iam.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import in.gov.ipie.common.audit.annotation.Auditable;
import in.gov.ipie.common.audit.model.AuditEventType;
import in.gov.ipie.common.security.keycloak.admin.KeycloakUserManagementClient;
import in.gov.ipie.service.iam.command.ProvisionAccountCommand;

/**
 * {@link AccountProvisioningService} implementation - wraps {@link KeycloakUserManagementClient},
 * the same client {@link RoleServiceImpl} already uses for role sync, so no new Keycloak-client
 * bean wiring was needed in this service to add this.
 */
@Service
public class AccountProvisioningServiceImpl implements AccountProvisioningService {

    private final KeycloakUserManagementClient keycloakUserManagementClient;

    public AccountProvisioningServiceImpl(KeycloakUserManagementClient keycloakUserManagementClient) {
        this.keycloakUserManagementClient = keycloakUserManagementClient;
    }

    @Override
    @Auditable(
            action = "ACCOUNT_PROVISIONED", entityType = "KEYCLOAK_ACCOUNT", entityId = "#result", eventType = AuditEventType.BUSINESS,
            newValue = "#command")
    public UUID provisionAccount(ProvisionAccountCommand command) {
        UUID keycloakUserId = keycloakUserManagementClient.createUser(
                command.username(), command.email(), command.firstName(), command.lastName(), command.password());
        // Same two-call sequence UserServiceImpl.completeRegistration used to perform directly -
        // makes ipie_id a reliable claim on every token this realm issues from this point on (see
        // the ipie-identity client scope).
        keycloakUserManagementClient.setUserAttribute(keycloakUserId, "ipie_id", command.ipieUserId().toString());
        return keycloakUserId;
    }

}
