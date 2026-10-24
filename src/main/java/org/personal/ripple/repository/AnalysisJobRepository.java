package org.personal.ripple.repository;

import org.personal.ripple.entity.AnalysisJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AnalysisJobRepository extends JpaRepository<AnalysisJob, UUID> {
}
