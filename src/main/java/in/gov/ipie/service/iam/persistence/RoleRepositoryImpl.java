package in.gov.ipie.service.iam.persistence;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import in.gov.ipie.service.iam.exception.RoleNotFoundException;
import in.gov.ipie.service.iam.exception.UnknownPermissionException;
import in.gov.ipie.service.iam.domain.Role;
import in.gov.ipie.service.iam.repository.RoleRepository;

/**
 * Infrastructure-layer adapter implementing the domain-owned {@link RoleRepository} port. The
 * only class allowed to know about {@link RoleJpaEntity}/{@link RoleJpaRepository}.
 */
@Repository
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class RoleRepositoryImpl implements RoleRepository {

    private final RoleJpaRepository jpaRepository;
    private final PermissionJpaRepository permissionJpaRepository;
    private final RolePersistenceMapper mapper;

    @Override
    public Role save(Role role) {
        RoleJpaEntity entity = mapper.toNewEntity(role);
        entity.setPermissions(resolvePermissions(role.getPermissionNames()));
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Role> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Role> findByName(String name) {
        return jpaRepository.findByName(name).map(mapper::toDomain);
    }

    @Override
    public boolean existsByName(String name) {
        return jpaRepository.existsByName(name);
    }

    @Override
    public List<Role> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Role update(Role role) {
        RoleJpaEntity entity = jpaRepository.findById(role.getId())
                .orElseThrow(() -> new RoleNotFoundException(role.getId().toString()));
        entity.setDescription(role.getDescription());
        entity.setPermissions(resolvePermissions(role.getPermissionNames()));
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    private Set<PermissionJpaEntity> resolvePermissions(Set<String> permissionNames) {
        return permissionNames.stream()
                .map(name -> permissionJpaRepository.findByName(name).orElseThrow(() -> new UnknownPermissionException(name)))
                .collect(Collectors.toSet());
    }
}
