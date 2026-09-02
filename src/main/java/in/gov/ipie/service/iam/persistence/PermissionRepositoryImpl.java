package in.gov.ipie.service.iam.persistence;

import java.util.Comparator;
import java.util.List;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import in.gov.ipie.service.iam.domain.Permission;
import in.gov.ipie.service.iam.repository.PermissionRepository;

@Repository
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class PermissionRepositoryImpl implements PermissionRepository {

    private final PermissionJpaRepository jpaRepository;

    @Override
    public boolean existsByName(String name) {
        return jpaRepository.existsByName(name);
    }

    /**
     * Sorted by resource then name so the catalogue arrives in the order the admin UI groups it in
     * - a stable order belongs here rather than being re-derived by every caller.
     */
    @Override
    public List<Permission> findAll() {
        return jpaRepository.findAll().stream()
                .map(PermissionRepositoryImpl::toDomain)
                .sorted(Comparator.comparing(Permission::resource).thenComparing(Permission::name))
                .toList();
    }

    @Override
    public Permission save(Permission permission) {
        return toDomain(jpaRepository.save(
                new PermissionJpaEntity(permission.name(), permission.description(), permission.resource())));
    }

    private static Permission toDomain(PermissionJpaEntity entity) {
        return new Permission(entity.getId(), entity.getName(), entity.getDescription(), entity.getResource());
    }
}
