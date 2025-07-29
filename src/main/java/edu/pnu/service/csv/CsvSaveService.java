package edu.pnu.service.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import edu.pnu.config.CustomUserDetails;
import edu.pnu.domain.Csv;
import edu.pnu.domain.Epc;
import edu.pnu.domain.EventHistory;
import edu.pnu.domain.Location;
import edu.pnu.domain.Member;
import edu.pnu.domain.Product;
import edu.pnu.dto.dataShare.ImportDatafromAiDTO;
import edu.pnu.exception.BadRequestException;
import edu.pnu.exception.CsvFileNotFoundException;
import edu.pnu.exception.CsvFileSaveToDiskException;
import edu.pnu.exception.FileUploadException;
import edu.pnu.exception.InvalidCsvFormatException;
import edu.pnu.repo.CsvRepository;
import edu.pnu.repo.EpcRepository;
import edu.pnu.repo.LocationRepository;
import edu.pnu.repo.MemberRepository;
import edu.pnu.repo.ProductRepository;
import edu.pnu.service.datashare.DataApplyService;
import edu.pnu.service.datashare.DataShareService;
import edu.pnu.service.statistics.StatisticsAdminService;
import edu.pnu.websocket.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvSaveService {

	// ---- 의존성 주입 ----
	private final ProductRepository productRepo;
	private final EpcRepository epcRepo;
	private final LocationRepository locationRepo;
//	private final EventHistoryRepository eventHistoryRepo;
	private final CsvRepository csvRepo;
	private final MemberRepository memberRepo;

	private final StatisticsAdminService statisticsAdminService;

	private final CsvSaveBatchService csvSaveBatchService; // JdbcTemplate batch insert
	private final DataShareService dataShareService;
	private final WebSocketService webSocketService;
	private final DataApplyService dataApplyService;

	private final int chunkSize = 1000; // 한 번에 읽어 처리할 row 수 (청크 단위)
	
	@Value("${file.upload.dir}")
    private String fileUploadDir;

	// ■■■■■■■■■■■■■■■■■■■■■■■■■■■ [하이브리드 메서드 : 파이프 라인] ■■■■■■■■■■■■■■■■■■■■■■■■
	// CSV 파일을 chunk 단위로 버퍼링해서 효율적으로 파싱 및 저장하는 메인 메서드
	
	@Async //CompletableFuture 를 붙이지 않았기 때문에 내부는 순차적으로 실행되며, 파이프 라인만 다른 스레드로 실행됨
	public void processAiAndStatistics(Long fileId, String userId) {
		try {
			// Step 1: AI 데이터 분석 준비
			webSocketService.sendMessage(userId, "AI 분석 데이터 준비 중");


			// Step 2: AI 서버로 데이터 전송 및 분석
			webSocketService.sendMessage(userId, "AI 분석 중 (시간이 걸릴 수 있습니다)");
			dataShareService.sendDataAndSaveResult(fileId);
			
			ImportDatafromAiDTO result = dataShareService.sendAndReceiveFromAi(fileId);
			dataApplyService.applyAnomalyResult(result);
			
			// Step 3: 통계 생성
			webSocketService.sendMessage(userId, "통계 데이터 생성 중");
			statisticsAdminService.processAllStatistics(fileId, userId);

			// Step 4: 완료 알림
			webSocketService.sendMessage(userId, "분석 및 통계 생성이 완료되었습니다!");
			log.info("[완료] 파일 ID {} 전체 처리 완료", fileId);

		} catch (Exception e) {
			log.error("[오류] 파일 ID {} 처리 중 오류: {}", fileId, e.getMessage(), e);
			webSocketService.sendMessage(userId, "처리 중 오류 발생: " + e.getMessage());
		}
	}


	public Long postCsv(MultipartFile file, CustomUserDetails user) {

		log.info("[시작] : [CsvSaveService] CSV 파일 업로드 요청 처리 시작 - file: {}", file.getOriginalFilename());

		Map<String, List<Integer>> errorRows = new HashMap<>(); // ★ 오류 누적 집계용

		
		// [1] 유저 및 파일 검증 (Null 체크, 형식 체크, 로그인 체크 등)
		if (user == null)
			throw new BadRequestException("[오류] : [CsvSaveService] 로그인 정보 없음");

		String userId = user.getUserId();
		Member member = memberRepo.findByUserId(userId).orElseThrow(() -> new RuntimeException("회원 정보 없음"));

		if (file == null || file.isEmpty())
			throw new CsvFileNotFoundException("[오류] : [CsvSaveService] 파일을 찾지 못했음");

		if (!file.getOriginalFilename().endsWith(".csv"))
			throw new FileUploadException("[오류] : [CsvSaveService] CSV 파일 아님");

		// 1. 실제 저장용 파일명 생성
		String originalFileName = file.getOriginalFilename();
		String ext = "";
		int idx = originalFileName.lastIndexOf(".");
		if (idx > -1) ext = originalFileName.substring(idx);
		String safeFileName = UUID.randomUUID().toString() + ext;
		
		// 2. Csv 엔티티 생성시, 원본명/저장명 모두 저장
		Csv csvLog = Csv.builder()
		    .fileName(originalFileName)   // 원본명
		    .savedFileName(safeFileName)  // 저장명 (필드 추가 필요!)
		    .filePath(fileUploadDir)
		    .fileSize(file.getSize())
		    .member(member)
		    .build();
		csvLog = csvRepo.save(csvLog);
		storeFileToDisk(file, csvLog);

		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

			// (1) "\t(탭)" 구분자로 파싱할 CSVReader 설정
			CSVParser parser = new CSVParserBuilder().withSeparator('\t').build();
			CSVReader csv = new CSVReaderBuilder(reader).withCSVParser(parser).build();

			// (2) 헤더 한 줄 읽어서, 컬럼 인덱스 매핑
			String[] header = csv.readNext();
			if (header == null)
				throw new InvalidCsvFormatException(
						"[오류] : [CsvSaveService] CSV 파일 header 없음 (fileName=" + csvLog.getFileName() + ")");

			String[] requiredColumns = { "location_id", "epc_product", "epc_code", "epc_lot", "event_type",
					"business_step", "event_time" };

			// (3) 필수 컬럼 체크
			for (String col : requiredColumns) {
				if (Arrays.stream(header).noneMatch(col::equals))
					// hearder돌면서 requiredColumns 비교해서 하나라도 같은 것이 있으면 false
					throw new InvalidCsvFormatException("[오류] : [CsvSaveService] 필수 컬럼(" + col + ")이 없습니다.");
			}

			// (4) 컬럼명 <-> 인덱스 매핑
			Map<String, Integer> colIdx = new HashMap<>();
			for (int i = 0; i < header.length; i++)
				colIdx.put(header[i], i);

			// (5) 날짜 포맷터 준비
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

			// 1. DB에 이미 있는 location_id
			Set<Long> existLocationIds = locationRepo.findAllPK();
			Set<Long> insertedLocations = new HashSet<>(existLocationIds);

			// 2. DB에 이미 있는 Product 복합키 조합
			Set<String> insertedProducts = new HashSet<>();

			// ProductKey → productId 맵 생성
			Map<String, Long> productKeyToIdMap = new HashMap<>();
			List<Object[]> rows = productRepo.findAllProductKeyIdMap();
			for (Object[] row : rows) {
			    String key = row[0] + "|" + row[1] + "|" + row[2];
			    Long productId = (Long) row[3];
			    productKeyToIdMap.put(key, productId);
			    insertedProducts.add(key); // 중복 방지용
			}

			// 3. DB에 이미 있는 EPC 코드
			Set<String> existEpcCodes = epcRepo.findAllPK(); // ★ 이 메서드가 Set<String>을 반환
			Set<String> insertedEPCs = new HashSet<>(existEpcCodes);

			// [8] chunk 단위로 파일을 읽으며, 파싱/저장/검증 진행
			List<String[]> chunk = new ArrayList<>(chunkSize);
			String[] row;
			int rowNum = 1; // 헤더는 1줄이므로 1부터 시작

			while ((row = csv.readNext()) != null) {
				chunk.add(row);
				rowNum++;

				// === chunkSize 도달 시 청크 저장 ===
				if (chunk.size() >= chunkSize) {
                    // [수정된 로직] 외래 키 종속성 문제를 해결하기 위해 청크 처리를 2단계로 분리합니다.
                    // 1단계: Location, Product 같이 다른 엔티티에 종속되지 않는 데이터를 먼저 파싱하고 저장합니다.
                    // 2단계: 저장된 Product의 ID를 포함하여 맵을 갱신한 후, 이 ID를 참조하는 Epc, EventHistory를 파싱하고 저장합니다.
					processChunkInStages(chunk, colIdx, csvLog, dtf, ymdFormatter, insertedLocations, insertedProducts, insertedEPCs, productKeyToIdMap, errorRows, rowNum - chunk.size() + 1);
					chunk.clear();
				}
			}
			// [9] 마지막 남은 chunk 저장 처리
			if (!chunk.isEmpty()) {
                // [수정된 로직] 마지막 남은 청크도 동일한 2단계 방식으로 처리합니다.
				processChunkInStages(chunk, colIdx, csvLog, dtf, ymdFormatter, insertedLocations, insertedProducts, insertedEPCs, productKeyToIdMap, errorRows, rowNum - chunk.size() + 1);
				chunk.clear();
			}

			log.info("[END] 전체 CSV 업로드 완료 - 총 row 수: {}", rowNum - 1);

			log.info("[전송] : [CsvSaveService] WebSocket 메시지 전송");

			// [11] 오류 리포트 출력 및 예외 처리
			if (!errorRows.isEmpty()) {
				StringBuilder report = new StringBuilder("[CSV 저장 전체 오류 요약]\n");
				errorRows.forEach((type, errorRowList) -> {
                    // [수정된 로직] 'rows' 변수가 스코프 내에 없으므로 'errorRowList'로 변경합니다.
					report.append("오류[").append(type).append("]: ").append(errorRowList.size()).append("건 rows: ").append(errorRowList)
							.append("\n");
				});
//				throw new RuntimeException(report.toString());
			}

		} catch (Exception e) {
			log.error("[오류] : [CsvSaveService] CSV 처리 중 예외 발생 - 원인: {}", e.getMessage(), e);
			throw new RuntimeException(e);
		}

		// CSV 저장 완료 후 AiData 생성
		return csvLog.getFileId();
	}

	
	// ■■■■■■■■■■■■■■■■■■■■■■■■■■■ [청크 단위 CSV 파싱 메서드] ■■■■■■■■■■■■■■■■■■■■■■■■
	// 하나의 chunk(List<String[]>)를 받아서 도메인 객체로 변환하고 중복 여부를 검사하는 메서드
	// 1. 필수 항목 누락 체크 및 오류 집계
	// 2. location_id, epc_product, epc_code의 중복 여부를 insertedXxx Set으로 관리
	// 3. 각 row에 대해 Location, Product, Epc, EventHistory 객체를 생성하고 리스트에 담음
	// 4. 날짜, boolean, double 등 타입 파싱은 tryParse 메서드에서 오류 감지 및 로그 기록 처리

	private void processChunkInStages(List<String[]> chunk, Map<String, Integer> colIdx, Csv csvLog,
                                  DateTimeFormatter dtf, DateTimeFormatter ymdFormatter, Set<Long> insertedLocations,
                                  Set<String> insertedProducts, Set<String> insertedEPCs,
                                  Map<String, Long> productKeyToIdMap, Map<String, List<Integer>> errorRows, int startRowNum) {

		// [수정된 로직] 1단계: 선행 엔티티(Location, Product) 파싱 및 리스트 준비
		List<Location> locationsToSave = new ArrayList<>();
		List<Product> productsToSave = new ArrayList<>();

		for (int i = 0; i < chunk.size(); i++) {
			String[] row = chunk.get(i);
			int currentRow = startRowNum + i;

            Long locId = parseLongSafe(getValue(colIdx, row, "location_id"));
            if (locId == null) {
                errorRows.computeIfAbsent("location_id 파싱 오류", k -> new ArrayList<>()).add(currentRow);
                continue;
            }

			// Location 파싱
			if (!insertedLocations.contains(locId)) {
				locationsToSave.add(
					Location.builder()
						.locationId(locId)
						.scanLocation(getValue(colIdx, row, "scan_location"))
						.latitude(parseDoubleSafe(getValue(colIdx, row, "latitude")))
						.longitude(parseDoubleSafe(getValue(colIdx, row, "longitude")))
						.operatorId(parseLongSafe(getValue(colIdx, row, "operator_id")))
						.deviceId(parseLongSafe(getValue(colIdx, row, "device_id")))
						.build());
                insertedLocations.add(locId); // 중복 추가 방지를 위해 미리 세트에 추가
			}
			
			// Product 파싱
			String prodId = getValue(colIdx, row, "epc_product");
			String epcCompany = getValue(colIdx, row, "epc_company");
			String productName = getValue(colIdx, row, "product_name");
			String productKey = prodId + "|" + epcCompany + "|" + productName;
			
			if (prodId == null || prodId.isBlank()){
				errorRows.computeIfAbsent("epc_product 파싱 오류", k -> new ArrayList<>()).add(currentRow);
				continue;
			}

			if (!insertedProducts.contains(productKey)) {
				productsToSave.add(Product.builder()
					.epcProduct(prodId)
					.epcCompany(epcCompany)
					.productName(productName)
					.build());
                insertedProducts.add(productKey); // 중복 추가 방지를 위해 미리 세트에 추가
			}
		}

		try {
			// [수정된 로직] 1단계-저장: 선행 엔티티를 DB에 먼저 저장합니다.
			if (!locationsToSave.isEmpty()) csvSaveBatchService.saveLocations(locationsToSave);
			if (!productsToSave.isEmpty()) csvSaveBatchService.saveProducts(productsToSave);

			// [수정된 로직] productKey-productId 맵을 최신화합니다.
			// 새로 저장된 Product의 DB 생성 ID를 가져오기 위함입니다.
			if (!productsToSave.isEmpty()) {
				List<Object[]> latestRows = productRepo.findAllProductKeyIdMap();
				for (Object[] line : latestRows) {
					String key = line[0] + "|" + line[1] + "|" + line[2];
					Long productId = (Long) line[3];
					productKeyToIdMap.put(key, productId);
				}
			}

			// [수정된 로직] 2단계: 종속 엔티티(Epc, EventHistory) 파싱 및 리스트 준비
			List<Epc> epcsToSave = new ArrayList<>();
			List<EventHistory> eventsToSave = new ArrayList<>();

			for (int i = 0; i < chunk.size(); i++) {
				String[] row = chunk.get(i);
				int currentRow = startRowNum + i;
				
                String epcCode = getValue(colIdx, row, "epc_code");
				if (epcCode == null || epcCode.isBlank()) {
					errorRows.computeIfAbsent("epc_code 파싱 오류", k -> new ArrayList<>()).add(currentRow);
					continue;
				}

				// Epc 파싱
				if (!insertedEPCs.contains(epcCode)) {
                    String prodId = getValue(colIdx, row, "epc_product");
                    String epcCompany = getValue(colIdx, row, "epc_company");
                    String productName = getValue(colIdx, row, "product_name");
                    String productKey = prodId + "|" + epcCompany + "|" + productName;
					
					// 최신화된 맵에서 productId를 조회합니다. 이 시점에서는 항상 유효한 ID를 찾을 수 있습니다.
					Long productId = productKeyToIdMap.get(productKey);
					if (productId == null) { // 만약 Product 키가 잘못되어 맵에 없다면 오류로 기록
						errorRows.computeIfAbsent("Epc에 해당하는 Product ID 조회 실패", k-> new ArrayList<>()).add(currentRow);
						continue;
					}

					Long locId = parseLongSafe(getValue(colIdx, row, "location_id"));
					if (locId == null) continue; // 위에서 이미 처리했지만 안전장치

					epcsToSave.add(Epc.builder()
						.epcCode(epcCode)
						.epcHeader(getValue(colIdx, row, "epc_header"))
						.epcLot(getValue(colIdx, row, "epc_lot"))
						.epcSerial(getValue(colIdx, row, "epc_serial"))
						.location(Location.builder().locationId(locId).build())
						.product(Product.builder().productId(productId).build()) // 안전하게 조회된 ID 사용
						.manufactureDate(tryParseDateTime(getValue(colIdx, row, "manufacture_date"), dtf, errorRows, currentRow, "manufacture_date"))
						.expiryDate(tryParseDate(getValue(colIdx, row, "expiry_date"), ymdFormatter, errorRows, currentRow, "expiry_date"))
						.build());
                    insertedEPCs.add(epcCode); // 중복 추가 방지를 위해 미리 세트에 추가
				}

				// EventHistory 파싱
				Long locId = parseLongSafe(getValue(colIdx, row, "location_id"));
				if (locId == null) continue;

				eventsToSave.add(EventHistory.builder()
						.epc(Epc.builder().epcCode(epcCode).build())
						.location(Location.builder().locationId(locId).build())
						.hubType(getValue(colIdx, row, "hub_type"))
						.eventType(getValue(colIdx, row, "event_type"))
						.businessOriginal(getValue(colIdx, row, "business_step"))
						.businessStep(normalizeBusinessStep(getValue(colIdx, row, "business_step")))
						.eventTime(tryParseDateTime(getValue(colIdx, row, "event_time"), dtf, errorRows, currentRow, "event_time"))
						.csv(csvLog)
						.build());
			}

			// [수정된 로직] 2단계-저장: 종속 엔티티들을 DB에 저장합니다.
			if (!epcsToSave.isEmpty()) csvSaveBatchService.saveEpcs(epcsToSave);
			if (!eventsToSave.isEmpty()) csvSaveBatchService.saveEvent(eventsToSave);
            
			log.info("[성공] : [CsvSaveService] 청크 처리 완료 (row: {} ~ {})", startRowNum, startRowNum + chunk.size() - 1);
		
		} catch (Exception e) {
			int startRow = startRowNum;
			int endRow = startRowNum + chunk.size() - 1;
			List<Integer> failRows = new ArrayList<>();
			for (int i = startRow; i <= endRow; i++) failRows.add(i);
			errorRows.computeIfAbsent("DB 저장 실패", k -> new ArrayList<>()).addAll(failRows);
			log.error("[오류] : [CsvSaveService] 청크 저장 실패 (row: {} ~ {}): {}", startRow, endRow, e.getMessage());
		}
	}



	
	// 파일 저장
	public void storeFileToDisk(MultipartFile file, Csv csvLog) {
	    try {
	        // 1. 저장할 디렉토리 경로 객체 생성
	        Path directoryPath = Paths.get(csvLog.getFilePath());

	        // 2. 폴더가 없으면 생성
	        if (!Files.exists(directoryPath)) {
	            Files.createDirectories(directoryPath);
	        }

	        // 3. 전체 경로 = 디렉토리 + 파일명
	        Path fullPath = Paths.get(csvLog.getFilePath(), csvLog.getSavedFileName());

	        // 4. 파일 저장
	        Files.copy(file.getInputStream(), fullPath, StandardCopyOption.REPLACE_EXISTING);

	        log.info("[성공] 파일 저장 완료: " + fullPath.toAbsolutePath());

	    } catch (IOException e) {
	        System.err.println("[오류] 파일 저장 실패: " + e.getMessage());
	        throw new CsvFileSaveToDiskException("파일 저장 중 오류 발생");
	    }
	}


//	 ■■■■■■■■■■■■■■■■■■■■■■■■■■■ [ 헬퍼 메서드 ] ■■■■■■■■■■■■■■■■■■■■■■■■
//	 tryParseDateTime: 문자열을 LocalDateTime으로 안전하게 파싱
//	   - 실패 시 null 반환 및 errorRows에 "필드명 파싱 오류"로 row 번호 저장
//	
//	 tryParseDate: 문자열을 LocalDate로 안전하게 파싱
//	   - 위와 동일한 방식으로 오류 누적 

//	 컬럼명으로부터 값 추출 (index map 사용, index 범위 체크 포함)
	private String getValue(Map<String, Integer> colIdx, String[] row, String col) {
		Integer idx = colIdx.get(col);
		return (idx != null && idx < row.length) ? row[idx] : null;
	}

	// 문자열을 Long 타입으로 안전하게 변환 (빈 문자열 → null)
	private Long parseLongSafe(String s) {
		try {
			return (isNullOrEmpty(s)) ? null : Long.parseLong(s.trim());
		} catch (Exception e) {
			return null;
		}
	}

	// 문자열을 Double 타입으로 안전하게 변환 (빈 문자열 → 0.0)
	private Double parseDoubleSafe(String s) {
		try {
			return (isNullOrEmpty(s)) ? 0.0 : Double.parseDouble(s.trim());
		} catch (Exception e) {
			return 0.0;
		}
	}

	// 문자열이 null이거나 빈 문자열인지 확인
	private boolean isNullOrEmpty(String s) {
		return s == null || s.trim().isEmpty();
	}

	// 문자열을 LocalDateTime으로 파싱
	private LocalDateTime tryParseDateTime(String value, DateTimeFormatter formatter,
			Map<String, List<Integer>> errorRows, int rowNum, String fieldName) {
		try {
			return isNullOrEmpty(value) ? null : LocalDateTime.parse(value.trim(), formatter);
		} catch (Exception e) {
			errorRows.computeIfAbsent(fieldName + " 파싱 오류", k -> new ArrayList<>()).add(rowNum);
			return null;
		}
	}

	// 문자열을 LocalDate로 파싱
	private LocalDate tryParseDate(String value, DateTimeFormatter formatter, Map<String, List<Integer>> errorRows,
			int rowNum, String fieldName) {
		try {
			return isNullOrEmpty(value) ? null : LocalDate.parse(value.trim(), formatter);
		} catch (Exception e) {
			errorRows.computeIfAbsent(fieldName + " 파싱 오류", k -> new ArrayList<>()).add(rowNum);
			return null;
		}
	}

	private String normalizeBusinessStep(String input) {
		if (input == null)
			return null;
		input = input.trim().toLowerCase();
		if (input.contains("factory"))
			return "Factory";
		if (input.contains("wms"))
			return "WMS";
		if (input.contains("logistics_hub") || input.contains("logi") || input.contains("hub"))
			return "LogiHub";
		if (input.startsWith("w_stock"))
			return "Wholesaler";
		if (input.startsWith("r_stock"))
			return "Reseller";
		if (input.contains("pos"))
			return "POS";
		return null;
	}
	
	private boolean safeEquals(String a, String b) {
		if (a == null) return b == null;
		return a.trim().equalsIgnoreCase(b != null ? b.trim() : "");
	}

}