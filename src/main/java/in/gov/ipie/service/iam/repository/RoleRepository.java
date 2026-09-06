package in.gov.ipie.service.iam.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import in.gov.ipie.service.iam.domain.Role;

/** Domain-owned port for Role persistence - the JPA-backed implementation lives in infrastructure. */
public interface RoleRepository {

    Role save(Role role);

    Optional<Role> findById(UUID id);

    Optional<Role> findByName(String name);

    boolean existsByName(String name);

    List<Role> findAll();

    /**
     * Persists an edit to an existing role, replacing its permission set with whatever the passed
     * role now carries.
     */
    Role update(Role role);

    void deleteById(UUID id);
}
