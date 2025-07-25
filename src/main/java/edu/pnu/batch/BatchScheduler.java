package edu.pnu.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchScheduler {
	private final JobLauncher jobLauncher;
    private final Job analyzedTripBatchJob;

    @Scheduled(cron = "0 0 1 * * *")
    public void runAnalyzedTripBatchJob() throws Exception {
        JobParameters params = new JobParametersBuilder()
            .addLong("time", System.currentTimeMillis())
            .toJobParameters();
        jobLauncher.run(analyzedTripBatchJob, params);
        log.info("[배치] : [BatchScheduler] analyzedTripBatchJob 실행됨 (1시)");
    }
}
