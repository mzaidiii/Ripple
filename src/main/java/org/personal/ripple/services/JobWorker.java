package org.personal.ripple.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.personal.ripple.github.CompareResult;
import org.personal.ripple.github.FileItem;
import org.personal.ripple.github.GitHubClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobWorker {

    private final JobClaimService jobClaimService;
    private final GitHubClient gitHubClient;


    @Scheduled(fixedDelay = 5000)
    void poll(){

        jobClaimService.claim().ifPresent(job ->{
            try {
                log.info("Received the job with id {} and name {}" , job.getId(),job.getRepoFullName());
                CompareResult result = gitHubClient.compare(job.getRepoFullName(),job.getBeforeSha(),job.getAfterSha());
                log.info("Job {}: {} commits, {} files changed", job.getId(), result.commits().size(), result.files().size());
                for (FileItem file : result.files()){
                    log.info("File Name {} , Status {} , Additions {} , Deletion {}" , file.filename(),file.status() , file.additions(),file.deletions());
                    if(file.patch()!=null){
                        log.info("Patch result {}" , file.patch());
                    }
                }

                jobClaimService.markDone(job.getId());
            } catch (Exception e){
                jobClaimService.markFailed(job.getId(),e.toString());
            }

                }
                );
    }

    @Scheduled(fixedDelay = 60000)
    void clean(){
        jobClaimService.cleanUp();
    }
}
