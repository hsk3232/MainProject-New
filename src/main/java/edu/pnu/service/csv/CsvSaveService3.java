//package edu.pnu.service.csv;
//
//import java.io.BufferedReader;
//import java.io.InputStreamReader;
//import java.nio.charset.StandardCharsets;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import com.opencsv.CSVParser;
//import com.opencsv.CSVParserBuilder;
//import com.opencsv.CSVReader;
//import com.opencsv.CSVReaderBuilder;
//
//import edu.pnu.Repo.AiDataRepository;
//import edu.pnu.Repo.CsvRepository;
//import edu.pnu.Repo.EpcRepository;
//import edu.pnu.Repo.EventHistoryRepository;
//import edu.pnu.Repo.LocationRepository;
//import edu.pnu.Repo.MemberRepository;
//import edu.pnu.Repo.ProductRepository;
//import edu.pnu.config.CustomUserDetails;
//import edu.pnu.domain.AiData;
//import edu.pnu.domain.Csv;
//import edu.pnu.domain.Epc;
//import edu.pnu.domain.EventHistory;
//import edu.pnu.domain.Location;
//import edu.pnu.domain.Member;
//import edu.pnu.domain.Product;
//import edu.pnu.exception.BadRequestException;
//import edu.pnu.exception.CsvFileNotFoundException;
//import edu.pnu.exception.FileUploadException;
//import edu.pnu.exception.InvalidCsvFormatException;
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class CsvSaveService3 {
//
//	// ---- 의존성 주입 ----
//	private final ProductRepository productRepo;
//	private final EpcRepository epcRepo;
//	private final LocationRepository locationRepo;
//	private final EventHistoryRepository eventHistoryRepo;
//	private final CsvRepository csvRepo;
//	private final MemberRepository memberRepo;
//	private final AiDataRepository aiDataRepo;
//
//	private final CsvSaveBatchService csvSaveBatchService; // JdbcTemplate batch insert
//
//	private final WebSocketService webSocketService;
//
//	private final int chunkSize = 1000; // 한 번에 읽어 처리할 row 수 (청크 단위)
//
//	// ■■■■■■■■■■■■■■■■■■■■■■■■■■■ [하이브리드 메서드] ■■■■■■■■■■■■■■■■■■■■■■■■
//	// CSV 파일을 chunk 단위로 버퍼링해서 효율적으로 파싱 및 저장하는 메인 메서드
//
//	
//	
//	
//	
//	@Transactional
//	public Long postCsv(MultipartFile file, CustomUserDetails user) {
//
//		log.info("[시작] : [CsvSaveService] CSV 파일 업로드 요청 처리 시작 - file: {}", file.getOriginalFilename());
//
//		Map<String, List<Integer>> errorRows = new HashMap<>(); // ★ 오류 누적 집계용
//
//		// [1] 유저 및 파일 검증 (Null 체크, 형식 체크, 로그인 체크 등)
//		if (user == null)
//			throw new BadRequestException("[오류] : [CsvSaveService] 로그인 정보 없음");
//
//		String userId = user.getUserId();
//		Member member = memberRepo.findByUserId(userId).orElseThrow(() -> new RuntimeException("회원 정보 없음"));
//
//		if (file == null || file.isEmpty())
//			throw new CsvFileNotFoundException("[오류] : [CsvSaveService] 파일을 찾지 못했음");
//
//		if (!file.getOriginalFilename().endsWith(".csv"))
//			throw new FileUploadException("[오류] : [CsvSaveService] CSV 파일 아님");
//
//		// [2] 업로드 파일 정보 저장 (Csv 로그 엔티티)
//		Csv csvLog = Csv.builder().fileName(file.getOriginalFilename()).filePath("c:/MainProject/save_csv")
//				.fileSize(file.getSize()).member(member).build();
//
//		csvLog = csvRepo.save(csvLog); // 엔티티에 저장 완료
//
//		try (BufferedReader reader = new BufferedReader(
//				new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
//
//			// (1) "\t(탭)" 구분자로 파싱할 CSVReader 설정
//			CSVParser parser = new CSVParserBuilder().withSeparator('\t').build();
//			CSVReader csv = new CSVReaderBuilder(reader).withCSVParser(parser).build();
//
//			// (2) 헤더 한 줄 읽어서, 컬럼 인덱스 매핑
//			String[] header = csv.readNext();
//			if (header == null)
//				throw new InvalidCsvFormatException(
//						"[오류] : [CsvSaveService] CSV 파일 header 없음 (fileName=" + csvLog.getFileName() + ")");
//
//			String[] requiredColumns = { "location_id", "epc_product", "epc_code", "epc_lot", "event_type",
//					"business_step", "event_time" };
//
//			// (3) 필수 컬럼 체크
//			for (String col : requiredColumns) {
//				if (Arrays.stream(header).noneMatch(col::equals))
//					// hearder돌면서 requiredColumns 비교해서 하나라도 같은 것이 있으면 false
//					throw new InvalidCsvFormatException("[오류] : [CsvSaveService] 필수 컬럼(" + col + ")이 없습니다.");
//			}
//
//			// (4) 컬럼명 <-> 인덱스 매핑
//			Map<String, Integer> colIdx = new HashMap<>();
//			for (int i = 0; i < header.length; i++)
//				colIdx.put(header[i], i);
//
//			// (5) 날짜 포맷터 준비
//			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//			DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
//
//			// 1. DB에 이미 있는 PK만 미리 모두 Set으로 뽑아둠
//			Set<Long> existLocationIds = locationRepo.findAllPK(); // location_id만
//			Set<Long> existProductIds = productRepo.findAllPK();
//			Set<String> existEpcCodes = epcRepo.findAllPK();
//
//			Set<Long> insertedLocations = new HashSet<>(existLocationIds);
//			Set<Long> insertedProducts = new HashSet<>(existProductIds);
//			Set<String> insertedEPCs = new HashSet<>(existEpcCodes);
//
//			// [8] chunk 단위로 파일을 읽으며, 파싱/저장/검증 진행
//			List<String[]> chunk = new ArrayList<>(chunkSize);
//			String[] row;
//			int rowNum = 1; // 헤더는 1줄이므로 1부터 시작
//
//			while ((row = csv.readNext()) != null) {
//				chunk.add(row);
//				rowNum++;
//
//				// === chunkSize 도달 시 청크 저장 ===
//				if (chunk.size() >= chunkSize) {
//					// [실제 저장용] - 매 청크마다 새로 할당(스코프 안)
//					List<Location> locations = new ArrayList<>();
//					List<Product> products = new ArrayList<>();
//					List<Epc> epcs = new ArrayList<>();
//					List<EventHistory> events = new ArrayList<>();
//
//					parseAndStoreChunk(chunk, colIdx, csvLog, dtf, ymdFormatter, insertedLocations, insertedProducts,
//							insertedEPCs, locations, products, epcs, events, errorRows, rowNum - chunk.size() + 1);
//
//					try {
//
//						// chunkSize에 도달할 때마다 batch insert
//						csvSaveBatchService.saveLocations(locations);
//						csvSaveBatchService.saveProducts(products);
//						csvSaveBatchService.saveEpcs(epcs);
//						csvSaveBatchService.saveEvent(events);
//						
////						// 1. JPA로 저장하는 방법
////						List<AiData> aiDataList = events.stream()
////						        .map(event -> AiData.builder().eventHistory(event).build())
////						        .toList();
////						csvSaveBatchService.saveAiDataBatch(aiDataList);
////
////						// 2. JDBC로 저장하는 방법
//
////						List<AiData> aiDataList = events.stream()
////								.map(event -> AiData.builder().eventHistory(event).build())
////								.toList();
////
////						csvSaveBatchService.saveAiDataBatch(aiDataList); // JdbcTemplate 방식으로 위임
//
//						// ★ 추가: 저장 완료된 데이터를 중복 방지 세트에 추가
//						// ★ 수정: 람다식 대신 일반 for문 사용
//						for (Location loc : locations) {
//							insertedLocations.add(loc.getLocationId());
//						}
//						for (Product prod : products) {
//							insertedProducts.add(prod.getEpcProduct());
//						}
//						for (Epc epc : epcs) {
//							insertedEPCs.add(epc.getEpcCode());
//						}
//
//						log.info("[성공] : [CsvSaveService] 청크 처리 완료 (row: {} ~ {})", rowNum - chunkSize + 1, rowNum);
//
//					} catch (Exception e) {
//						// ★ 저장 실패 시, 해당 chunk의 row 번호를 모두 errorRows에 기록 ★
//						int startRow = rowNum - chunkSize + 1;
//						int endRow = rowNum;
//						List<Integer> failRows = new ArrayList<>();
//						for (int i = startRow; i <= endRow; i++)
//							failRows.add(i);
//						errorRows.computeIfAbsent("DB 저장 실패", k -> new ArrayList<>()).addAll(failRows);
//						log.error("[오류] : [CsvSaveService] 청크 저장 실패 (row: {} ~ {}): {}", startRow, endRow,
//								e.getMessage());
//					}
//
//					chunk.clear();
//				}
//			}
//			// [9] 마지막 남은 chunk 저장 처리
//			// === while문 종료 후, chunk가 남았으면 저장 ===
//			if (!chunk.isEmpty()) {
//				List<Location> locations = new ArrayList<>();
//				List<Product> products = new ArrayList<>();
//				List<Epc> epcs = new ArrayList<>();
//				List<EventHistory> events = new ArrayList<>();
//				parseAndStoreChunk(chunk, colIdx, csvLog, dtf, ymdFormatter, insertedLocations, insertedProducts,
//						insertedEPCs, locations, products, epcs, events, errorRows, rowNum - chunk.size() + 1);
//				// 파싱해서 리스트로 변환 및 중복 방지 세트 갱신
//
//				try {
//					// 저장 전 리스트 크기 확인
//					log.info("저장 전 리스트 크기 확인 - locations: {}, products: {}, epcs: {}, events: {}", locations.size(),
//							products.size(), epcs.size(), events.size());
//					csvSaveBatchService.saveLocations(locations);
//					csvSaveBatchService.saveProducts(products);
//					csvSaveBatchService.saveEpcs(epcs);
//					csvSaveBatchService.saveEvent(events);
//
//					// ★ 추가: 저장 완료된 데이터를 중복 방지 세트에 추가
//					// ★ 수정: 람다식 대신 일반 for문 사용
//					for (Location loc : locations) {
//						insertedLocations.add(loc.getLocationId());
//					}
//					for (Product prod : products) {
//						insertedProducts.add(prod.getEpcProduct());
//					}
//					for (Epc epc : epcs) {
//						insertedEPCs.add(epc.getEpcCode());
//					}
//
//					log.info("[성공] : [CsvSaveService] 마지막 청크 처리 완료 (row: {} ~ {})", rowNum - chunk.size() + 1, rowNum);
//
//				} catch (Exception e) {
//					// ★ 저장 실패 시, 해당 chunk의 row 번호를 모두 errorRows에 기록 ★
//					int startRow = rowNum - chunkSize + 1;
//					int endRow = rowNum;
//					List<Integer> failRows = new ArrayList<>();
//					for (int i = startRow; i <= endRow; i++)
//						failRows.add(i);
//					errorRows.computeIfAbsent("DB 저장 실패", k -> new ArrayList<>()).addAll(failRows);
//					log.error("[오류] 청크 저장 실패 (row: {} ~ {}): {}", startRow, endRow, e.getMessage());
//				}
//
//				locations.clear();
//				products.clear();
//				epcs.clear();
//				events.clear();
//				chunk.clear();
//			}
//
//			log.info("[END] 전체 CSV 업로드 완료 - 총 row 수: {}", rowNum - 1);
//			webSocketService.sendMessage(user.getUserId(), "DB 저장 완료");
//			webSocketService.sendMessage(user.getUserId(), "AI 전송 중");
//			log.info("[전송] : [CsvSaveService] WebSocket 메시지 전송");
//
//			// [11] 오류 리포트 출력 및 예외 처리
//			if (!errorRows.isEmpty()) {
//				StringBuilder report = new StringBuilder("[CSV 저장 전체 오류 요약]\n");
//				errorRows.forEach((type, rows) -> {
//					report.append("오류[").append(type).append("]: ").append(rows.size()).append("건 rows: ").append(rows)
//							.append("\n");
//				});
////				throw new RuntimeException(report.toString());
//			}
//
//		} catch (Exception e) {
//			log.error("[오류] : [CsvSaveService] CSV 처리 중 예외 발생 - 원인: {}", e.getMessage(), e);
//			throw new RuntimeException(e);
//		}
//
//		// CSV 저장 완료 후 AiData 생성
//		return csvLog.getFileId();
//	}
//
//	// ■■■■■■■■■■■■■■■■■■■■■■■■■■■ [청크 단위 CSV 파싱 메서드] ■■■■■■■■■■■■■■■■■■■■■■■■
//	// 하나의 chunk(List<String[]>)를 받아서 도메인 객체로 변환하고 중복 여부를 검사하는 메서드
//	// 1. 필수 항목 누락 체크 및 오류 집계
//	// 2. location_id, epc_product, epc_code의 중복 여부를 insertedXxx Set으로 관리
//	// 3. 각 row에 대해 Location, Product, Epc, EventHistory 객체를 생성하고 리스트에 담음
//	// 4. 날짜, boolean, double 등 타입 파싱은 tryParse 메서드에서 오류 감지 및 로그 기록 처리
//	private void parseAndStoreChunk(List<String[]> chunk, Map<String, Integer> colIdx, Csv csvLog,
//			DateTimeFormatter dtf, DateTimeFormatter ymdFormatter, Set<Long> insertedLocations,
//			Set<Long> insertedProducts, Set<String> insertedEPCs, List<Location> locations, List<Product> products,
//			List<Epc> epcs, List<EventHistory> events, Map<String, List<Integer>> errorRows, int startRowNum) {
//
//		for (int i = 0; i < chunk.size(); i++) {
//			String[] row = chunk.get(i);
//			int currentRow = startRowNum + i;
//			try {
//				Long locId = parseLongSafe(getValue(colIdx, row, "location_id"));
//				Long prodId = parseLongSafe(getValue(colIdx, row, "epc_product"));
//				String epcCode = getValue(colIdx, row, "epc_code");
//
//				// === [추가] 동기 방식과 동일: 필수값 파싱 오류 별도 집계
//				if (locId == null) {
//					errorRows.computeIfAbsent("location_id 파싱 오류", k -> new ArrayList<>()).add(currentRow);
//					continue;
//				}
//				if (prodId == null) {
//					errorRows.computeIfAbsent("epc_product 파싱 오류", k -> new ArrayList<>()).add(currentRow);
//					continue;
//				}
//				if (epcCode == null || epcCode.isBlank()) {
//					errorRows.computeIfAbsent("epc_code 파싱 오류", k -> new ArrayList<>()).add(currentRow);
//					continue;
//				}
//
//				// === [중복 INSERT 방지: DB+파일 모두] ===
//				if (!insertedLocations.contains(locId)) {
//					locations.add(
//							Location.builder().locationId(locId).scanLocation(getValue(colIdx, row, "scan_location"))
//									.latitude(parseDoubleSafe(getValue(colIdx, row, "latitude")))
//									.longitude(parseDoubleSafe(getValue(colIdx, row, "longitude"))).build());
//				}
//				insertedLocations.add(locId);
//
//				if (!insertedProducts.contains(prodId)) {
//					products.add(Product.builder().epcProduct(prodId).productName(getValue(colIdx, row, "product_name"))
//							.build());
//				}
//				insertedProducts.add(prodId);
//
//				if (!insertedEPCs.contains(epcCode)) {
//					epcs.add(Epc.builder().epcCode(epcCode).epcHeader(getValue(colIdx, row, "epc_header"))
//							.epcCompany(parseLongSafe(getValue(colIdx, row, "epc_company")))
//							.epcLot(parseLongSafe(getValue(colIdx, row, "epc_lot")))
//							.epcSerial(getValue(colIdx, row, "epc_serial"))
//							.location(Location.builder().locationId(locId).build())
//							.product(Product.builder().epcProduct(prodId).build()).build());
//				}
//				insertedEPCs.add(epcCode);
//
//				// === 날짜 등 필드 파싱 오류 별도 집계 ===
//				EventHistory.EventHistoryBuilder evBuilder = EventHistory.builder()
//						.epc(Epc.builder().epcCode(epcCode).build())
//						.location(Location.builder().locationId(locId).build())
//						.hubType(getValue(colIdx, row, "hub_type")).eventType(getValue(colIdx, row, "event_type"))
//						.businessOriginal(getValue(colIdx, row, "business_step"))
//						.businessStep(normalizeBusinessStep(getValue(colIdx, row, "business_step"))).csv(csvLog);
//
//				// 날짜 파싱
//				evBuilder.eventTime(tryParseDateTime(getValue(colIdx, row, "event_time"), dtf, errorRows, currentRow,
//						"event_time"));
//				evBuilder.manufactureDate(tryParseDateTime(getValue(colIdx, row, "manufacture_date"), dtf, errorRows,
//						currentRow, "manufacture_date"));
//				evBuilder.expiryDate(tryParseDate(getValue(colIdx, row, "expiry_date"), ymdFormatter, errorRows,
//						currentRow, "expiry_date"));
//
//				events.add(evBuilder.build());
//
//			} catch (Exception e) {
//				errorRows.computeIfAbsent("파싱 오류", k -> new ArrayList<>()).add(currentRow);
//				log.error("[오류] : [CsvSaveService] void parseAndStoreChunk 저장 오류 " + e.getMessage());
//			}
//		}
//	}
//
////	private void createInitialAiData(Long fileId) {
////		log.info("AIData 자동 생성 시작 - fileId: {}", fileId);
////
////		List<EventHistory> events = eventHistoryRepo.findByCsv_FileId(fileId);
////		if (events.isEmpty()) {
////			log.warn("[오류] : [CsvSaveService] EventHistory가 없음- fileId: {}", fileId);
////			return;
////		}
////		List<AiData> aiDataList = events.stream().map(event -> AiData.builder().eventHistory(event).build()) // @Builder.Default로
////				.toList();
////
////		csvSaveBatchService.saveAiDataBatch(aiDataList); // JdbcTemplate 방식으로 위임
////		log.info("AiData 초기 생성 완료: {}건", aiDataList.size());
////	}
//
////	 ■■■■■■■■■■■■■■■■■■■■■■■■■■■ [ 헬퍼 메서드 ] ■■■■■■■■■■■■■■■■■■■■■■■■
////	 tryParseDateTime: 문자열을 LocalDateTime으로 안전하게 파싱
////	   - 실패 시 null 반환 및 errorRows에 "필드명 파싱 오류"로 row 번호 저장
////	
////	 tryParseDate: 문자열을 LocalDate로 안전하게 파싱
////	   - 위와 동일한 방식으로 오류 누적 
//
////	 컬럼명으로부터 값 추출 (index map 사용, index 범위 체크 포함)
//	private String getValue(Map<String, Integer> colIdx, String[] row, String col) {
//		Integer idx = colIdx.get(col);
//		return (idx != null && idx < row.length) ? row[idx] : null;
//	}
//
//	// 문자열을 Long 타입으로 안전하게 변환 (빈 문자열 → null)
//	private Long parseLongSafe(String s) {
//		try {
//			return (isNullOrEmpty(s)) ? null : Long.parseLong(s.trim());
//		} catch (Exception e) {
//			return null;
//		}
//	}
//
//	// 문자열을 Double 타입으로 안전하게 변환 (빈 문자열 → 0.0)
//	private Double parseDoubleSafe(String s) {
//		try {
//			return (isNullOrEmpty(s)) ? 0.0 : Double.parseDouble(s.trim());
//		} catch (Exception e) {
//			return 0.0;
//		}
//	}
//
//	// 문자열이 null이거나 빈 문자열인지 확인
//	private boolean isNullOrEmpty(String s) {
//		return s == null || s.trim().isEmpty();
//	}
//
//	// 문자열을 LocalDateTime으로 파싱
//	private LocalDateTime tryParseDateTime(String value, DateTimeFormatter formatter,
//			Map<String, List<Integer>> errorRows, int rowNum, String fieldName) {
//		try {
//			return isNullOrEmpty(value) ? null : LocalDateTime.parse(value.trim(), formatter);
//		} catch (Exception e) {
//			errorRows.computeIfAbsent(fieldName + " 파싱 오류", k -> new ArrayList<>()).add(rowNum);
//			return null;
//		}
//	}
//
//	// 문자열을 LocalDate로 파싱
//	private LocalDate tryParseDate(String value, DateTimeFormatter formatter, Map<String, List<Integer>> errorRows,
//			int rowNum, String fieldName) {
//		try {
//			return isNullOrEmpty(value) ? null : LocalDate.parse(value.trim(), formatter);
//		} catch (Exception e) {
//			errorRows.computeIfAbsent(fieldName + " 파싱 오류", k -> new ArrayList<>()).add(rowNum);
//			return null;
//		}
//	}
//
//	private String normalizeBusinessStep(String input) {
//		if (input == null)
//			return null;
//		input = input.trim().toLowerCase();
//		if (input.contains("factory"))
//			return "Factory";
//		if (input.contains("wms"))
//			return "WMS";
//		if (input.contains("logistics_hub") || input.contains("logi") || input.contains("hub"))
//			return "LogiHub";
//		if (input.startsWith("w_stock"))
//			return "Wholesaler";
//		if (input.startsWith("r_stock"))
//			return "Reseller";
//		if (input.contains("pos"))
//			return "POS";
//		return null;
//	}
//
//}