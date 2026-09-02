package in.gov.ipie.service.iam.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.gov.ipie.common.audit.annotation.Auditable;
import in.gov.ipie.common.audit.model.AuditEventType;
import in.gov.ipie.common.security.context.CurrentUser;
import in.gov.ipie.common.security.context.CurrentUserProvider;
import in.gov.ipie.common.security.keycloak.admin.KeycloakUserManagementClient;
import in.gov.ipie.service.iam.command.AssignRoleCommand;
import in.gov.ipie.service.iam.command.CreatePermissionCommand;
import in.gov.ipie.service.iam.command.CreateRoleCommand;
import in.gov.ipie.service.iam.command.RevokeRoleCommand;
import in.gov.ipie.service.iam.command.UpdateRoleCommand;
import in.gov.ipie.service.iam.exception.RoleNotFoundException;
import in.gov.ipie.service.iam.domain.Permission;
import in.gov.ipie.service.iam.domain.Role;
import in.gov.ipie.service.iam.repository.PermissionRepository;
import in.gov.ipie.service.iam.repository.RoleRepository;
import in.gov.ipie.service.iam.repository.UserRoleRepository;

/**
 * {@link RoleService} implementation. This service's own database is the source of truth (master
 * standards doc, 5.1) - {@link KeycloakUserManagementClient} calls here only sync that truth into
 * Keycloak's realm-role model so issued JWTs keep carrying the right {@code permissions} claim
 * (see {@code JwtPermissionsConverter}), never the other way round. Role-name/permission-existence
 * validation for {@link #createRole} lives in {@code RoleValidationAspect}, not here.
 */
@Service
public class RoleServiceImpl implements RoleService {

    /**
     * The role defined as holding every permission (see {@link #grantToSuperAdmin}). Named here
     * rather than looked up by a marker permission, because the invariant is about this one role.
     */
    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PermissionRepository permissionRepository;
    private final KeycloakUserManagementClient keycloakUserManagementClient;
    private final CurrentUserProvider currentUserProvider;

    public RoleServiceImpl(
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            PermissionRepository permissionRepository,
            KeycloakUserManagementClient keycloakUserManagementClient,
            CurrentUserProvider currentUserProvider) {
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.permissionRepository = permissionRepository;
        this.keycloakUserManagementClient = keycloakUserManagementClient;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    @Transactional
    @Auditable(
            action = "ROLE_CREATED", entityType = "ROLE", entityId = "#result.id", eventType = AuditEventType.BUSINESS,
            newValue = "#result")
    public Role createRole(CreateRoleCommand command) {
        Role saved = roleRepository.save(Role.createNew(command.name(), command.description(), command.permissionNames()));
        syncToKeycloak(saved);
        return saved;
    }

    @Override
    @Transactional
    @Auditable(
            action = "ROLE_UPDATED", entityType = "ROLE", entityId = "#command.roleId()", eventType = AuditEventType.BUSINESS,
            newValue = "#result")
    @CacheEvict(cacheNames = "user-roles", allEntries = true)
    public Role updateRole(UpdateRoleCommand command) {
        Role role = roleRepository.findById(command.roleId())
                .orElseThrow(() -> new RoleNotFoundException(command.roleId().toString()));
        role.update(command.description(), command.permissionNames());
        Role updated = roleRepository.update(role);
        syncToKeycloak(updated);
        return updated;
    }

    /**
     * Evicts every cached user-roles entry rather than one: a role's permission set is shared by
     * everyone holding it, and this service does not track who that is without a further query.
     * Editing a role is rare and the cache refills on demand, so the blunt eviction is the cheaper
     * trade against serving stale permissions.
     */
    @Override
    @Transactional
    @Auditable(
            action = "ROLE_DELETED", entityType = "ROLE", entityId = "#roleId", eventType = AuditEventType.BUSINESS)
    @CacheEvict(cacheNames = "user-roles", allEntries = true)
    public void deleteRole(UUID roleId) {
        Role role = roleRepository.findById(roleId).orElseThrow(() -> new RoleNotFoundException(roleId.toString()));
        roleRepository.deleteById(roleId);
        keycloakUserManagementClient.deleteRealmRole(role.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> listRoles() {
        return roleRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Permission> listPermissions() {
        return permissionRepository.findAll();
    }

    /**
     * No Keycloak call here, deliberately. A permission becomes a realm role when a role granting
     * it is created or updated - {@code syncToKeycloak} calls {@code ensureRealmRole} for each of
     * the role's permission names - so mirroring it now would create an unattached realm role that
     * nothing composites and nobody holds. It is created in Keycloak at the moment it first means
     * something there.
     */
    @Override
    @Transactional
    @Auditable(
            action = "PERMISSION_CREATED", entityType = "PERMISSION", entityId = "#result.id()",
            eventType = AuditEventType.BUSINESS, newValue = "#result")
    public Permission createPermission(CreatePermissionCommand command) {
        Permission created = permissionRepository.save(
                Permission.createNew(command.name(), command.description(), command.resource()));
        grantToSuperAdmin(created.name());
        return created;
    }

    /**
     * Every permission the caller holds, unioned across their assigned roles, read from this
     * service's own tables. Falls back to the token's claim only when the subject is not a
     * {@code user_roles} principal at all - a service account such as ipie-user-service's, which
     * holds realm roles and has no row here.
     */
    private Set<String> permissionsHeldBy(CurrentUser grantor) {
        UUID subject;
        try {
            subject = UUID.fromString(grantor.userId());
        } catch (IllegalArgumentException notAKeycloakUserId) {
            // A subject that is not a UUID is not a realm user - nothing in user_roles can match it,
            // so the token's own claim is the only answer available. Never a throw: an unparseable
            // subject must not turn an authorization check into a 500.
            return grantor.permissions();
        }
        Set<String> fromRoles = resolveRoles(userRoleRepository.findRoleIdsByKeycloakUserId(subject))
                .stream()
                .flatMap(assigned -> assigned.getPermissionNames().stream())
                .collect(Collectors.toCollection(TreeSet::new));
        return fromRoles.isEmpty() ? grantor.permissions() : fromRoles;
    }

    /**
     * Keeps {@code SUPER_ADMIN} the union of every permission that exists - which is what the name
     * has always claimed and what the programme confirmed on 2026-08-17: the super admin holds every
     * power, and the tiers below hold subsets of it.
     *
     * <p>It held all six seeded permissions by coincidence of migration, not by rule, and this
     * method is the rule. Without it the invariant broke at the least convenient moment: creating a
     * permission requires {@code RBAC_DEFINE}, which only a super admin has, so the first permission
     * a super admin defined was one they did not hold - and, now that {@link
     * #enforceDelegationCeiling} exists, one they could never assign to anyone either. The ceiling
     * and this invariant are two halves of the same idea and neither is safe to add alone.
     *
     * <p>Re-syncing the role to Keycloak matters as much as the database write: the permission has
     * to become a realm role and be composited into {@code SUPER_ADMIN}, or the token keeps saying
     * otherwise and every gate on the new permission denies a super admin.
     */
    private void grantToSuperAdmin(String permissionName) {
        roleRepository.findByName(SUPER_ADMIN_ROLE).ifPresent(superAdmin -> {
            Set<String> widened = new TreeSet<>(superAdmin.getPermissionNames());
            if (!widened.add(permissionName)) {
                return;
            }
            superAdmin.update(superAdmin.getDescription(), widened);
            syncToKeycloak(roleRepository.update(superAdmin));
        });
    }


    /**
     * A grantor may assign only roles whose permissions they already hold themselves (Stage 10.3,
     * "delegation ceiling").
     *
     * <p>Without this, {@code ROLES_MANAGE} is the whole story: any holder can assign any role in
     * the catalogue, including {@code SUPER_ADMIN}. That was demonstrable rather than theoretical -
     * a {@code PILLAR_ADMIN}, who holds {@code ROLES_MANAGE} and nothing above it, could grant
     * {@code SUPER_ADMIN} to anyone including themselves, and the call returned 204. Permission to
     * hand out capabilities is not permission to hand out *every* capability.
     *
     * <p>The rule is deliberately expressed as a subset test rather than a list of roles nobody may
     * assign. A denylist has to be updated every time a role is defined, and the one that gets
     * forgotten is the dangerous one; the subset test is total by construction and needs no
     * maintenance as the catalogue grows. It also composes with the tiers the programme described
     * (2026-08-17): a super admin holds every permission and so may assign anything, a pillar admin
     * may assign only within what their own pillar role grants, and the same rule will scope an
     * entity or IP admin once {@code user_roles.scope_id} exists.
     *
     * <p>THE GRANTOR'S PERMISSIONS COME FROM THIS SERVICE, NOT FROM THEIR TOKEN. The token carries a
     * {@code permissions} claim, but that claim is a Keycloak realm-role projection and it drifts:
     * the realm composite for {@code SUPER_ADMIN} was missing four of the six permissions the
     * catalogue grants it, so a ceiling read from the token refused a super admin their own role.
     * This service owns {@code user_roles} and the catalogue, so it answers the question directly
     * and a stale realm can no longer produce a wrong authorization decision.
     *
     * <p>NO AUTHENTICATED CALLER MEANS AN INTERNAL PATH, not an anonymous one. The only route here
     * without a security context is {@link #assignDefaultRole}, called from the
     * user-verified event consumer to grant {@code STAKEHOLDER}; it is reached from a signed
     * inter-service message, never from a request. Applying the ceiling there would fail every
     * registration, since a message has no permissions to be a superset of.
     */
    private void enforceDelegationCeiling(Role role) {
        Optional<CurrentUser> caller = currentUserProvider.current();
        if (caller.isEmpty()) {
            return;
        }
        Set<String> grantorHolds = permissionsHeldBy(caller.get());
        Set<String> beyondCeiling = role.getPermissionNames().stream()
                .filter(permission -> !grantorHolds.contains(permission))
                .collect(Collectors.toCollection(TreeSet::new));
        if (!beyondCeiling.isEmpty()) {
            throw new AccessDeniedException("Cannot assign role '" + role.getName()
                    + "': it grants " + beyondCeiling + ", which the current user does not hold");
        }
    }

    /**
     * Mirrors a role into Keycloak: the role itself, each permission it grants (permissions are
     * realm roles too), then the composite links between them.
     *
     * <p>The composite step is the one that matters. Without it the realm role exists but contains
     * nothing, so a token issued for a user holding it carries no permissions - and the role still
     * looks correct in the admin console, which is what makes the omission expensive to find.
     */
    private void syncToKeycloak(Role role) {
        keycloakUserManagementClient.ensureRealmRole(role.getName(), role.getDescription());
        role.getPermissionNames().forEach(permission -> keycloakUserManagementClient.ensureRealmRole(permission, null));
        keycloakUserManagementClient.syncRealmRoleComposites(role.getName(), role.getPermissionNames());
    }

    /**
     * Assigns an existing role to a user - writes {@code user_roles} (this service's source of
     * truth) and additively syncs the realm-role mapping to Keycloak (see {@link
     * KeycloakUserManagementClient#assignRealmRoles}).
     */
    @Override
    @Transactional
    @Auditable(
            action = "ROLE_ASSIGNED", entityType = "USER", entityId = "#command.userId()", eventType = AuditEventType.BUSINESS,
            comment = "#command.comment()", newValue = "#command.roleName()")
    @CacheEvict(cacheNames = "user-roles", key = "#command.keycloakUserId()")
    public void assignRole(AssignRoleCommand command) {
        Role role = roleRepository.findByName(command.roleName()).orElseThrow(() -> new RoleNotFoundException(command.roleName()));
        enforceDelegationCeiling(role);
        String assignedBy = currentUserProvider.current().map(user -> user.username()).orElse("system");
        userRoleRepository.assign(command.userId(), command.keycloakUserId(), role.getId(), assignedBy);
        keycloakUserManagementClient.assignRealmRoles(command.keycloakUserId(), List.of(role.getName()));
    }

    /**
     * {@code @CacheEvict} here is required in addition to {@link #assignRole}'s own - this method
     * calls {@code assignRole(...)} as a bare intra-class call, which bypasses the Spring AOP
     * proxy entirely (a well-known Spring self-invocation limitation), so {@code assignRole}'s
     * eviction would never actually fire for this, the auto-assign-on-verification path.
     */
    @Override
    @Transactional
    @Auditable(
            action = "ROLE_AUTO_ASSIGNED", entityType = "USER", entityId = "#userId", eventType = AuditEventType.BUSINESS,
            newValue = "#defaultRoleName")
    @CacheEvict(cacheNames = "user-roles", key = "#keycloakUserId")
    public void assignDefaultRole(UUID userId, UUID keycloakUserId, String defaultRoleName) {
        assignRole(new AssignRoleCommand(userId, keycloakUserId, defaultRoleName, null));
    }

    @Override
    @Transactional
    @Auditable(
            action = "ROLE_REVOKED", entityType = "USER", entityId = "#command.userId()", eventType = AuditEventType.BUSINESS,
            comment = "#command.comment()", oldValue = "#command.roleName()")
    @CacheEvict(cacheNames = "user-roles", key = "#command.keycloakUserId()")
    public void revokeRole(RevokeRoleCommand command) {
        Role role = roleRepository.findByName(command.roleName()).orElseThrow(() -> new RoleNotFoundException(command.roleName()));
        userRoleRepository.revoke(command.userId(), role.getId());
        keycloakUserManagementClient.removeRealmRoles(command.keycloakUserId(), List.of(role.getName()));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "user-roles", key = "#keycloakUserId")
    public List<Role> getRolesForKeycloakUser(UUID keycloakUserId) {
        return resolveRoles(userRoleRepository.findRoleIdsByKeycloakUserId(keycloakUserId));
    }

    /**
     * Deliberately not cached: the "user-roles" cache is keyed on Keycloak user id, and an
     * administrator reading someone else's roles arrives with a different id for the same person -
     * caching here under that second key would let the two views drift apart.
     */
    @Override
    @Transactional(readOnly = true)
    public List<Role> getRolesForUser(UUID userId) {
        return resolveRoles(userRoleRepository.findRoleIdsByUserId(userId));
    }

    private List<Role> resolveRoles(List<UUID> roleIds) {
        return roleIds.stream()
                .map(roleId -> roleRepository.findById(roleId).orElseThrow(() -> new RoleNotFoundException(roleId.toString())))
                .toList();
    }
}
