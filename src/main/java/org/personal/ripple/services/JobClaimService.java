package org.personal.ripple.services;

import lombok.RequiredArgsConstructor;
import org.personal.ripple.entity.AnalysisJob;
import org.personal.ripple.entity.JobStatus;
import org.personal.ripple.repository.AnalysisJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobClaimService {

    private final AnalysisJobRepository analysisJobRepository;

    @Transactional
    public Optional<AnalysisJob> claim(){
        Optional<AnalysisJob> claimed = analysisJobRepository.claimNext();

        claimed.ifPresent(job -> {
            job.setStatus(JobStatus.RUNNING);
            job.setStartedAt(Instant.now());
            job.setAttempts(job.getAttempts() + 1);
        });
        return claimed;
    }

    @Transactional
    public void markDone(UUID id ){
        analysisJobRepository.findById(id).ifPresent(job ->{
                    job.setStatus(JobStatus.DONE);
                    job.setFinishedAt(Instant.now());
                }
                );
    }

    @Transactional
    public void markFailed(UUID id , String error ){
        analysisJobRepository.findById(id).ifPresent(job ->{
            int attempts = job.getAttempts();
            job.setLastError(error);
            if(attempts >=3){
                job.setStatus(JobStatus.FAILED);
                job.setFinishedAt(Instant.now());
            } else {
                job.setStatus(JobStatus.PENDING);
                job.setNextRunAt(Instant.now().plusSeconds(attempts*30L));
            }
        });
    }

    @Transactional
    public void cleanUp() {
        Instant cutoff = Instant.now().minusSeconds(600);

        List<AnalysisJob> leftOver =
                analysisJobRepository.findByStatusAndStartedAtBefore(
                        JobStatus.RUNNING, cutoff
                );

        for (AnalysisJob job : leftOver) {
            if (job.getAttempts() >= 3) {
                job.setStatus(JobStatus.FAILED);
                job.setFinishedAt(Instant.now());
            } else {
                job.setStatus(JobStatus.PENDING);
                job.setNextRunAt(Instant.now().plusSeconds(30));
            }

            job.setLastError("Recovered after worker timeout");
            job.setStartedAt(null);
        }
    }
}
