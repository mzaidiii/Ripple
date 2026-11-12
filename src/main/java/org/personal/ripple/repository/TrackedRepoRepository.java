package org.personal.ripple.repository;

import org.personal.ripple.entity.TrackedRepo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrackedRepoRepository extends JpaRepository<TrackedRepo , UUID> {
    Optional<TrackedRepo> findByFullName(String fullName);

    @Modifying
    @Transactional
    @Query(value = """
        UPDATE repositories
        SET status = 'INDEXING', last_error = NULL
        WHERE full_name = :name AND status <> 'INDEXING'
        """, nativeQuery = true)
    int markIndexing(@Param("name") String name);

    @Modifying
    @Transactional
    @Query(value = """
        UPDATE repositories
        SET status = 'FAILED', last_error = 'Interrupted by application restart'
        WHERE status = 'INDEXING'
        """, nativeQuery = true)
    int failInterrupted();
}
