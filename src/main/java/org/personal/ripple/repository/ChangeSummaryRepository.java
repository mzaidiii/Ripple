package org.personal.ripple.repository;

import org.personal.ripple.entity.ChangeSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ChangeSummaryRepository extends JpaRepository<ChangeSummary , UUID> {
}
