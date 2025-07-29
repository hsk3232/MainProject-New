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

import edu.pnu.config.DataShareProperties;
import edu.pnu.domain.Csv;
import edu.pnu.domain.EventHistory;
import edu.pnu.dto.dataShare.ExportDataToAiDTO;
import edu.pnu.dto.dataShare.ImportDatafromAiDTO;
import edu.pnu.exception.NoDataFoundException;
import edu.pnu.repo.CsvRepository;
import edu.pnu.repo.EventHistoryRepository;
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
	private final DataApplyService dataApplyService;

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

//	 ■■■■■■■■■■■■■  특정 파일 ID로 EventHistory 리스트를 DTO로 변환 (KPI 분석에서 사용) ■■■■■■■■■■■■■■
	@Transactional(readOnly = true) // 서비스 레이어에서 트랜잭션 보장 (Lazy 끊김 방지)
	public List<ExportDataToAiDTO> exportByFileId(Long fileId) {
		log.info("[진행] : EventHistory 엔티티 → ExportDataToAiDTO 변환 (fileId=" + fileId + ")");

		// [1] fileId null 체크를 쿼리 전에!
		if (fileId == null) {
			throw new NoDataFoundException("fileId가 null입니다. 업로드한 파일 ID를 확인하세요!");
		}

		// [2] 쿼리 실행
		List<EventHistory> entityList = eventHistoryRepo.findAllByCsv_FileIdWithEpcAndProduct(fileId);
		// epcCode로 그룹핑
		Map<String, List<EventHistory>> groupByEpc = entityList.stream()
				.collect(Collectors.groupingBy(e -> e.getEpc().getEpcCode()));

		return groupByEpc.entrySet().stream().map(e -> ExportDataToAiDTO.from(e.getKey(), e.getValue())).toList();
	}

//	 ■■■■■■■■■■■■■ [동기] 파일 ID로 분석 데이터 추출 + AI 서버에 전송 ■■■■■■■■■■■■■■
	public void sendDataAndSaveResult(Long fileId) {
		log.info("\n[START][동기] : [DataShareService] AI 데이터 수동 전송 트리거 (fileId=" + fileId + ")");

		log.info("[진행] : [DataShareService] 분석 데이터 추출 시도...");
		List<ExportDataToAiDTO> dtoList = exportByFileId(fileId);

		if (dtoList.isEmpty()) {
			log.error("[경고] : [DataShareService] ExportDataToAiDTO 리스트가 비어있음! (fileId=" + fileId + ")");
			return;
		}

		log.info("[진행] : 분석 데이터 추출 완료 (" + dtoList.size() + "건)");
		sendToAiAndSave(dtoList);
		log.info("[완료] : [DataShareService] AI 데이터 처리 완료 (fileId={})", fileId);
	}

//	 ■■■■■■■■■■  AI 서버에 데이터 전송 및 결과 수신 로직  ■■■■■■■■■■
	
	public void sendToAiAndSave(List<ExportDataToAiDTO> dtoList) {

		int batchSize = props.getBatchSize();
		int maxRetries = props.getRetryMaxAttempts();
		long retryDelayMillis = props.getRetryDelayMs();
		long batchDelayMillis = props.getBatchDelayMs();

		int total = dtoList.size();
		int sent = 0;
		int successCount = 0;
		int failCount = 0;
		int consecutiveFailures = 0;
		List<Integer> failedBatches = new ArrayList<>();

		log.info("[시작] AI 배치 전송 - 전체: {}건, 배치 사이즈: {}", total, batchSize);

		// [1-3] HTTP 헤더 구성
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		log.info("[시작] AI 배치 전송 - 전체: {}건, 배치 사이즈: {}", total, batchSize);
		
		// [2] 배치 단위로 분할 전송
		for (int i = 0; i < total; i += batchSize) {
			int end = Math.min(i + batchSize, total);
			List<ExportDataToAiDTO> batch = dtoList.subList(i, end);
			int batchIndex = (i / batchSize) + 1;

			log.info("[배치전송][{}] {}~{}번 전송 시도", batchIndex, i + 1, end);

			// [2-2] 전송 및 재시도
			ImportDatafromAiDTO importData = sendBatchWithRetry(batch, maxRetries, retryDelayMillis, batchIndex);

			if (importData != null) {
				try {
					dataApplyService.applyAnomalyResult(importData);
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
		log.info("[완료] AI 배치 전송 요약 - 총 전송: {}건, 성공: {}건, 실패: {}건", sent, successCount, failCount);
		if (!failedBatches.isEmpty()) {
			log.warn("[실패 배치 인덱스]: {}", failedBatches);
		}
	}

	// ======= 실제 전송 및 재시도 로직 (중복 분리, RestTemplate 생성 로직 합침) =======
	private ImportDatafromAiDTO sendBatchWithRetry(List<ExportDataToAiDTO> batch, int maxRetries, long retryDelayMillis,
			int batchIndex) {
		RestTemplate restTemplate = createRestTemplate();
		HttpEntity<Map<String, Object>> request = createHttpRequest(batch);

		String aiApiUrl = props.getAiApiUrl();

		for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
			try {
				ResponseEntity<ImportDatafromAiDTO> response = restTemplate.postForEntity(aiApiUrl, request,
						ImportDatafromAiDTO.class);
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

	@Transactional(readOnly = true)
	public ImportDatafromAiDTO sendAndReceiveFromAi(Long fileId) {
		List<ExportDataToAiDTO> dtoList = exportByFileId(fileId);
		if (dtoList.isEmpty()) {
			throw new NoDataFoundException("[DataShareService] 전송할 데이터가 없습니다 (fileId=" + fileId + ")");
		}
		log.info("[진행] : AI 전송 대상 DTO {}건 생성 완료", dtoList.size());

		return sendBatchWithRetry(dtoList, props.getRetryMaxAttempts(), props.getRetryDelayMs(), 1);
	}

	// ======= RestTemplate, HttpRequest 생성 분리 =======
	private RestTemplate createRestTemplate() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(props.getRestConnectTimeout());
		factory.setReadTimeout(props.getRestReadTimeout());
		return new RestTemplate(factory);
	}

	private HttpEntity<Map<String, Object>> createHttpRequest(List<ExportDataToAiDTO> batch) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("data", batch);

		return new HttpEntity<>(requestBody, headers);
	}

}