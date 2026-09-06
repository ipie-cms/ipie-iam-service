package in.gov.ipie.service.iam.domain;

import java.util.UUID;

/**
 * A single grantable capability, e.g. {@code USER_READ}. The admin UI reads this catalogue to offer
 * the valid choices when composing a role.
 *
 * <p>{@code resource} is the ABAC attribute axis the permission applies to (e.g. {@code CLAIMS},
 * {@code DASHBOARD}) and is what the UI groups the catalogue by.
 *
 * <p><b>A permission created here is inert until some service checks for it.</b> Permissions were
 * originally seed-only for exactly this reason: a permission grants nothing by existing, only by
 * being named in a {@code @RequiresPermission} somewhere in the code. Creating one at runtime is
 * therefore a way to prepare the catalogue - so a role can be composed, and the realm role mirrored,
 * before or alongside the code that enforces it - and not a way to grant a new capability. An
 * administrator inventing a name no service checks produces a permission that does nothing, and
 * nothing in the system will report that as an error.
 */
public record Permission(UUID id, String name, String description, String resource) {

    /** A permission not yet persisted - the id is assigned by the database on save. */
    public static Permission createNew(String name, String description, String resource) {
        return new Permission(null, name, description, resource);
    }
}
