package org.personal.ripple.services;

import lombok.RequiredArgsConstructor;
import org.personal.ripple.entity.ChangeSummary;
import org.personal.ripple.repository.ChangeSummaryRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final ChangeSummaryRepository changeSummaryRepository;

    public void save (UUID jobId , String summary , String model ){
        ChangeSummary ans = new ChangeSummary();

        ans.setJobId(jobId);
        ans.setSummary(summary);
        ans.setModel(model);

        changeSummaryRepository.save(ans);
    }
}
