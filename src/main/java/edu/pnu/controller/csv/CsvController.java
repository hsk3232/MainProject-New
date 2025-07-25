package edu.pnu.controller.csv;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import edu.pnu.config.CustomUserDetails;
import edu.pnu.dto.CsvFileListResponseDTO;
import edu.pnu.service.csv.CsvLogService;
import edu.pnu.service.csv.CsvSaveService;
import edu.pnu.service.datashare.DataShareService;
import edu.pnu.websocket.WebSocketService;
import jakarta.servlet.annotation.MultipartConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@MultipartConfig(maxFileSize = 1024 * 1024 * 200, // 100mb
		maxRequestSize = 1024 * 1024 * 500 // 500mb
)
@RequestMapping("/api/manager")
public class CsvController {

	private final CsvLogService csvLogService;
	private final CsvSaveService csvSaveService;
	private final DataShareService dataShareService;
	private final WebSocketService webSocketService;

	
	// Front -> Back csv 전달 및 저장
	@PostMapping("/upload")
	public ResponseEntity<Map<String, String>> postCsv(@RequestParam("file") MultipartFile file,
			@AuthenticationPrincipal CustomUserDetails user) {

		String userId = user.getUserId();

		// 1. 즉시 사용자에게 시작 알림
		webSocketService.sendMessage(userId, "파일 업로드 시작");

		// 2. CSV 저장 (EventHistory까지만)
		Long fileId = csvSaveService.postCsv(file, user);

		// 3. 즉시 응답 반환
		webSocketService.sendMessage(userId, "CSV 저장 완료");

		CompletableFuture.runAsync(() -> {
	        try {
	            csvSaveService.processAiAndStatistics(fileId, userId);
	        } catch (Exception e) {
	            // 비동기 에러는 WebSocket으로만 처리
	            webSocketService.sendMessage(userId, "처리 중 오류 발생: " + e.getMessage());
	            log.error("[오류] 비동기 처리 실패: {}", e.getMessage(), e);
	        }
		});

		return ResponseEntity.ok(Map.of("message", "업로드 시작됨. 파일 ID: " + fileId + ". 진행상황은 실시간으로 알림됩니다."));
     
	}


	// 업로드된 file 목록 조회
		@GetMapping("/upload/filelist")
		public Map<String, Object> getFileListByCursor(@RequestParam(required = false) Long cursor,
				@RequestParam(defaultValue = "50") int size, @RequestParam(required = false) String search) {

			List<CsvFileListResponseDTO> data = csvLogService.getFileListByCursor(cursor, size, search);

			// nextCursor(다음 커서값) 계산
			Long nextCursor = (data.size() == size) ? data.get(data.size() - 1).getFileId() : null;

			// 응답으로 보낼 데이터를 담기 위한 Map(딕셔너리) 생성
			Map<String, Object> response = new HashMap<>();

			// data라는 이름으로 실제 데이터 리스트(파일 목록) 저장
			response.put("data", data);
			// 방금 계산한 커서값(또는 null)을 Map에 저장
			response.put("nextCursor", nextCursor);

			// 완성된 Map을 응답으로 리턴
			return response;
		}



	@GetMapping("/download/{fileLogId}")
	public ResponseEntity<Resource> downloadCsv(@PathVariable Long fileLogId,
			@AuthenticationPrincipal CustomUserDetails user) {
		// 필요 시: user와 파일의 접근 권한 체크도 서비스에서 처리할 수 있음
		
		Resource resource = csvLogService.loadCsvResource(fileLogId);
		String filename = csvLogService.getFileName(fileLogId);

		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
				.body(resource);
	}

	
	@PostMapping("/resend/{fileId}")
	public String resendToAi(@PathVariable Long fileId) {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		try {
	        Future<Void> future = executor.submit(() -> {
	            dataShareService.sendDataAndSaveResult(fileId);
	            return null;
	        });
	        future.get(5, TimeUnit.SECONDS); // 5초 타임아웃
	        return "AI 모듈로 재전송 성공!";
	    } catch (TimeoutException e) {
	        throw new ResponseStatusException(
	            HttpStatus.REQUEST_TIMEOUT,
	            "AI 모듈로 재전송 시간이 너무 오래 걸려 중단되었습니다. 다시 시도해주세요."
	        );
	    } catch (Exception e) {
	        throw new ResponseStatusException(
	            HttpStatus.INTERNAL_SERVER_ERROR,
	            "AI 모듈로 재전송 중 알 수 없는 오류가 발생했습니다."
	        );
	    } finally {
	        executor.shutdownNow();
	    }
	}
	
	


}