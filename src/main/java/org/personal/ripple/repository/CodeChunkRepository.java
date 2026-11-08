package org.personal.ripple.repository;

import org.personal.ripple.entity.CodeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CodeChunkRepository extends JpaRepository<CodeChunk , UUID> {

    @Query(value = """
        SELECT * FROM code_chunks
        WHERE repo_full_name = :repo
        ORDER BY embedding <=> CAST(:embedding AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<CodeChunk> findNearest(@Param("repo") String repo,
                                @Param("embedding") String embedding,
                                @Param("limit") int limit);
}
