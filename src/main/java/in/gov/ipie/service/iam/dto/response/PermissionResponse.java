package in.gov.ipie.service.iam.dto.response;

/**
 * A permission from the seeded catalogue. {@code resource} is the ABAC axis the permission applies
 * to, and is what the admin UI groups the list by.
 */
public record PermissionResponse(String id, String name, String description, String resource) {
}
