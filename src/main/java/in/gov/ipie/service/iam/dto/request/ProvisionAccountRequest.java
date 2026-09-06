package in.gov.ipie.service.iam.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * {@code ipieUserId} is the caller's (ipie-user-service's) already-assigned internal user id -
 * stamped onto the newly-created Keycloak user's {@code ipie_id} attribute in the same call, the
 * same two-step sequence {@code UserServiceImpl.completeRegistration} used to perform directly
 * against {@code KeycloakUserManagementClient} before account provisioning moved here.
 */
public record ProvisionAccountRequest(

        @NotBlank
        String ipieUserId,

        @NotBlank
        String username,

        @NotBlank
        @Email
        String email,

        @NotBlank
        String firstName,

        @NotBlank
        String lastName,

        @NotBlank
        String password) {
}
