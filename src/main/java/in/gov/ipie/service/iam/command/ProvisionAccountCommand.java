package in.gov.ipie.service.iam.command;

import java.util.UUID;

public record ProvisionAccountCommand(
        UUID ipieUserId, String username, String email, String firstName, String lastName, String password) {
}
