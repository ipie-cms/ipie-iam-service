package in.gov.ipie.service.iam.service;

import java.util.UUID;

import in.gov.ipie.service.iam.command.ProvisionAccountCommand;

/**
 * Keycloak account provisioning - the "authentication" half of "authentication and
 * authorisation is iam-service's responsibility". The one place any service creates a Keycloak
 * identity; ipie-user-service used to call {@code KeycloakUserManagementClient} directly for
 * this, now calls this service's {@code /internal/accounts} endpoint instead.
 */
public interface AccountProvisioningService {

    /** Returns the newly created Keycloak user's id. */
    UUID provisionAccount(ProvisionAccountCommand command);

    // Deliberately no password operation here. Credentials are owned by CredentialService, which
    // stores an Argon2id hash in this service's own database - Keycloak holds no password at all
    // (ARCHITECTURE_WORKING_PLAN.md, D1). This interface is about the Keycloak *account*: creating it,
    // and the identity attributes that hang off it.
}
