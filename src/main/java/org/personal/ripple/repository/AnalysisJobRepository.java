package org.personal.ripple.repository;

import org.personal.ripple.entity.AnalysisJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface AnalysisJobRepository extends JpaRepository<AnalysisJob, UUID> {

    @Query(value = """  
        SELECT * FROM analysis_jobs
        WHERE status = 'PENDING' AND next_run_at <= now()
        ORDER BY next_run_at
        LIMIT 1
        FOR UPDATE SKIP LOCKED
        """,nativeQuery = true)
    Optional<AnalysisJob> claimNext();
}
