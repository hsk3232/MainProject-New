package edu.pnu.service.datashare;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import edu.pnu.Repo.CsvRepository;
import edu.pnu.Repo.EventHistoryRepository;
import edu.pnu.config.DataShareProperties;
import edu.pnu.domain.Csv;
import edu.pnu.domain.EventHistory;
import edu.pnu.dto.dataShere.ExportDataToAiDTO;
import edu.pnu.dto.dataShere.ImportAiDataDTO;
import edu.pnu.dto.dataShere.ImportDatafromAiDTO;
import edu.pnu.exception.NoDataFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataShareService {

	private final EventHistoryRepository eventHistoryRepo;
//	private final AiDataRepository aiDataRepo;
//	private final EpcRepository epcRepo;
//	private final EpcAnomalyStatsRepository epcAnomalyStatsRepo;
	private final CsvRepository csvRepo;
//	private final FileAnomalyStatsRepository fileAnomalyStatsRepo;
	private final DataShareProperties props; // 주입된 커스텀 설정 클래스

	@PersistenceContext
	private EntityManager em;
	
//	 ■■■■■■■■■■■■■ 외부 트리거 진입점 ■■■■■■■■■■■■■■

	public void autoSendLatestFile() {
		log.info("[진입] : [DataShareService] 최신 CSV 파일로 AI 전송 시작");
		Long lastFileId = csvRepo.findTopByOrderByFileIdDesc().map(Csv::getFileId).orElse(null);

		if (lastFileId == null) {
			log.error("[경고] : [DataShareService] CSV 파일이 하나도 없음! [자동 전송 종료]");
			log.error("[경고] : [DataShareService] 프로세스 중단\n");
			return;
		}

		log.info("[진행] :[DataShareService] 최근 파일 ID = " + lastFileId + " / AI 자동 전송 시작");
		sendDataAndSaveResult(lastFileId);
		log.info("[END][비동기] : [DataShareService] 최근 파일 자동 전송 프로세스 완료\n");
	}

	

//	 ■■■■■■■■■■■■■ [동기] 파일 ID로 분석 데이터 추출 + AI 서버에 전송 ■■■■■■■■■■■■■■
	public void sendDataAndSaveResult(Long fileId) {
		log.info("\n[START][동기] : [DataShareService] AI 데이터 수동 전송 트리거 (fileId=" + fileId + ")");

		log.info("[진행] : [DataShareService] 분석 데이터 추출 시도...");
		List<ExportDataToAiDTO> dtoList = exportByFileId(fileId);

		if (dtoList.isEmpty()) {
			log.error("[경고] : [DataShareService] ExportRowDTO 리스트가 비어있음! (fileId=" + fileId + ")");
			return;
		}

		log.info("[진행] : 분석 데이터 추출 완료 (" + dtoList.size() + "건)");
		sendToAiAndSave(dtoList);
		log.info("[완료] : [DataShareService] AI 데이터 처리 완료 (fileId={})", fileId);
	}

	
//	 ■■■■■■■■■■■■■  특정 파일 ID로 EventHistory 리스트를 DTO로 변환 (KPI 분석에서 사용) ■■■■■■■■■■■■■■
	 @Transactional(readOnly = true) // 서비스 레이어에서 트랜잭션 보장 (Lazy 끊김 방지)
	public List<ExportDataToAiDTO> exportByFileId(Long fileId) {
		log.info("[진행] : EventHistory 엔티티 → ExportRowDTO 변환 (fileId=" + fileId + ")");

		// [1] fileId null 체크를 쿼리 전에!
		if (fileId == null) {
			throw new NoDataFoundException("fileId가 null입니다. 업로드한 파일 ID를 확인하세요!");
		}

		// [2] 쿼리 실행
		List<EventHistory> entityList = eventHistoryRepo.findAllByCsv_FileIdWithEpcAndProduct(fileId);
		 // epcCode로 그룹핑
	    Map<String, List<EventHistory>> groupByEpc = entityList.stream()
	            .collect(Collectors.groupingBy(e -> e.getEpc().getEpcCode()));

	    return groupByEpc.entrySet().stream()
	            .map(e -> ExportDataToAiDTO.from(e.getKey(), e.getValue()))
	            .toList();
	}

	
//	 ■■■■■■■■■■  AI 서버에 데이터 전송 및 결과 수신 로직  ■■■■■■■■■■
	@Transactional
	public void sendToAiAndSave(List<ExportDataToAiDTO> dtoList) {
		// [1-1] 설정값 읽기
		int batchSize = props.getBatchSize();
		int maxRetries = props.getRetryMaxAttempts();
		long retryDelayMillis = props.getRetryDelayMs();
		long batchDelayMillis = props.getBatchDelayMs();
		int connectTimeout = props.getRestConnectTimeout();
		int readTimeout = props.getRestReadTimeout();
		String aiApiUrl = props.getAiApiUrl();

		// [1-2] RestTemplate 설정 (timeout 적용)
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(connectTimeout);
		factory.setReadTimeout(readTimeout);
		RestTemplate restTemplate = new RestTemplate(factory);

		// [1-3] HTTP 헤더 구성
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		// [1-4] 초기 통계 변수 선언
		int total = dtoList.size();
		int sent = 0;
		int successCount = 0;
		int failCount = 0;
		int consecutiveFailures = 0;
		List<Integer> failedBatches = new ArrayList<>();

		log.info("[시작] AI 배치 전송 - 전체: {}건, 배치 사이즈: {}", total, batchSize);
		// [2] 배치 단위로 분할 전송
		for (int i = 0; i < total; i += batchSize) {
			int end = Math.min(i + batchSize, total);
			List<ExportDataToAiDTO> batch = dtoList.subList(i, end);
			int batchIndex = (i / batchSize) + 1;

			log.info("[배치전송][{}] {}~{}번 전송 시도", batchIndex, i + 1, end);

			// [2-1] 요청 바디 구성
			Map<String, Object> requestBody = new HashMap<>();
			requestBody.put("data", batch);
			HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

			// [2-2] 전송 및 재시도
			ImportDatafromAiDTO importData = sendBatchWithRetry(restTemplate, aiApiUrl, request, maxRetries, retryDelayMillis,
					batchIndex);

			if (importData != null) {
				try {
					applyAnomalyResult(importData);
					successCount += batch.size();
					sent += batch.size();
					log.info("[성공][{}] 분석 결과 저장 완료 ({}건)", batchIndex, batch.size());
					consecutiveFailures = 0;
				} catch (Exception e) {
					log.error("[오류][{}] DB 저장 중 예외 발생 - {}", batchIndex, e.getMessage(), e);
					failCount += batch.size();
					failedBatches.add(batchIndex);
					consecutiveFailures++;
				}
			} else {
				log.error("[실패][{}] 최종 전송 실패 - 해당 배치는 누락 처리", batchIndex);
				failCount += batch.size();
				failedBatches.add(batchIndex);
				consecutiveFailures++;
			}

			// [2-3] 연속 실패 3회 이상 시 중단
			if (consecutiveFailures >= 3) {
				log.error("[중단] 연속 3회 실패 발생 - 배치 전송 중단");
				break;
			}

			// [2-4] 배치 간 대기
			try {
				Thread.sleep(batchDelayMillis);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				log.error("[오류] 배치 간 딜레이 중 인터럽트 발생");
				break;
			}
		}

		// [3] 최종 요약 로그
		log.info("[완료] AI 배치 전송 요약 - 총 전송: {}건", sent);
		log.info("[완료] 성공: {}건 / 실패: {}건", successCount, failCount);
		if (!failedBatches.isEmpty()) {
			log.warn("[실패 배치 인덱스]: {}", failedBatches);
		}
	}

	// [재시도 처리 로직]
	private ImportDatafromAiDTO sendBatchWithRetry(RestTemplate restTemplate, String url, HttpEntity<?> request,
			int maxRetries, long retryDelayMillis, int batchIndex) {
		for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
			try {
				ResponseEntity<ImportDatafromAiDTO> response = restTemplate.postForEntity(url, request, ImportDatafromAiDTO.class);

				if (response.getStatusCode().is2xxSuccessful()) {
					log.info("[응답][{}][시도:{}] AI 서버 응답 수신 완료 (status: {})", batchIndex, attempt,
							response.getStatusCode());
					return response.getBody();
				} else {
					log.warn("[응답][{}][시도:{}] 비정상 응답 코드 수신 - status: {}", batchIndex, attempt,
							response.getStatusCode());
				}
			} catch (Exception e) {
				log.error("[전송실패][{}][시도:{}] 예외 발생 - {}", batchIndex, attempt, e.getMessage());
			}

			// 재시도 간 딜레이
			try {
				Thread.sleep(retryDelayMillis);
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
				log.error("[오류][{}] 재시도 대기 중 인터럽트 발생", batchIndex);
				break;
			}
		}

		return null;
	}


	// ■■■■■■■■■■■■■■■■ 결과 반영 (DB 업데이트) ■■■■■■■■■■■■■■■■
	@Transactional
	public void applyAnomalyResult(ImportDatafromAiDTO importData) {
		log.info("[진입] : [DataShareService] AI로부터 받은 이상치 결과 DB 반영 시작");

		if (importData.getImportAiDataList() != null) {
			List<ImportAiDataDTO> dtoList = importData.getImportAiDataList();

			log.info("[진행] : [DataShareService] AiData 업데이트 대상 건수 = {}", dtoList.size());

			// [1-1] eventId 목록 추출
			List<Long> eventIds = dtoList.stream().map(ImportAiDataDTO::getEventId).toList();

			// 여기서 bulk update으로 한번에 저장
	        int updatedCount = eventHistoryRepo.bulkUpdatseAnomaly(eventIds);
	        em.clear(); // 영속성 컨텍스트 비우기

	        log.info("[완료] anomaly 업데이트 적용 ({}건), 영속성 컨텍스트 clear!", updatedCount);


		
		}
	}

}