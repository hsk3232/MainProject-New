package edu.pnu.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.pnu.dto.CsvFileListResponseDTO;
import edu.pnu.dto.NodeDTO;
import edu.pnu.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/manager")
public class DashboardController {
	
	private final DashboardService dashboardService;
	
	@GetMapping("/node")
	public List<NodeDTO> getNodeList(){
		log.info("[진입] : [NodeController] Node 정보 전달 진입");
		
		List<NodeDTO> dto = dashboardService.getNodeList();
		log.info("[성공] : [NodeController] Node 정보 전달 성공");
		return dto;
	}
	
//	@GetMapping("/anomalies")
//	public Map<String, Object> getFileListByCursor(@RequestParam(required = false) Long cursor,
//			@RequestParam(defaultValue = "50") int size, @RequestParam(required = false) String search) {
//
//		List<CsvFileListResponseDTO> data = csvLogService.getFileListByCursor(cursor, size, search);
//
//		// nextCursor(다음 커서값) 계산
//		Long nextCursor = (data.size() == size) ? data.get(data.size() - 1).getFileId() : null;
//
//		// 응답으로 보낼 데이터를 담기 위한 Map(딕셔너리) 생성
//		Map<String, Object> response = new HashMap<>();
//
//		// data라는 이름으로 실제 데이터 리스트(파일 목록) 저장
//		response.put("data", data);
//		// 방금 계산한 커서값(또는 null)을 Map에 저장
//		response.put("nextCursor", nextCursor);
//
//		// 완성된 Map을 응답으로 리턴
//		return response;
//	}
}
