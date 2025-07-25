package edu.pnu.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

import edu.pnu.service.batch.BatchTriggerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableBatchProcessing // Spring Batch 기능 활성화
@EnableScheduling
@RequiredArgsConstructor

// ---------- 배치 설정 파일-----------//
public class MyBatchConfig {
	
	private final JobRepository jobRepo; // ⭐️ batch에서 필수!
    private final PlatformTransactionManager transactionManager; // ⭐️ Step 생성에 필요
	
    private final BatchTriggerService batchTriggerService;
    
    
	@Bean// 정적분석 플러그인에서 	@Bean으로 선언한 메서드는 public이 아니어도 된다
	Job analyzedTripBatchJob(Step analyzedTripStep) {
		return new JobBuilder("analyzedTripBatchJob", jobRepo)
				.incrementer(new RunIdIncrementer()) // RunIdIncrementer 추가
	            .start(analyzedTripStep()) // StepScope를 위해 null 전달
				.build();
	}

	
	@Bean
	Step analyzedTripStep() {
	    return new StepBuilder("analyzedTripStep", jobRepo)
	        .tasklet((contribution, chunkContext) -> {
	            // 여기에 배치 작업 로직 작성 (예: 로그, DB 저장 등)
	        	
	        	log.info("[실행] : [MyBatchConfig] Step 실행됨!");
	        	// [1] 새로운 Csv 저장되면, AnalyzedTrip 로직 저장
	        	batchTriggerService.analyzeAndSaveAllTrips(); 
	        	
	            return RepeatStatus.FINISHED;
	        }, transactionManager)
	        .build();
	}
		
}
