package edu.pnu.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.pnu.dto.AnalyzedTripDTO;
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
//	public Map<String, Object> getAnomaliesList(
//	        @RequestParam(defaultValue = "0") int page,
//	        @RequestParam(defaultValue = "50") int size,
//	        @RequestParam(required = false) String epcLot,  @RequestParam(required = false) String epcCode) {
//
//		 Page<AnalyzedTripDTO> dtoPage = dashboardService.getAnomaliesList(page, size, epcLot, epcCode);
//
//		    Map<String, Object> response = new HashMap<>();
//		    response.put("data", dtoPage.getContent());
//		    response.put("currentPage", dtoPage.getNumber());
//		    response.put("totalPages", dtoPage.getTotalPages());
//		    response.put("totalElements", dtoPage.getTotalElements());
//		    response.put("pageSize", dtoPage.getSize());
//
//		    // 커서처럼 nextCursor를 내려주고 싶다면 (마지막 페이지가 아니고, 데이터가 존재할 때만)
//		    Long nextCursor = (!dtoPage.isLast() && !dtoPage.isEmpty())
//		        ? dtoPage.getContent().get(dtoPage.getContent().size() - 1).getRoadId()
//		        : null;
//		    response.put("nextCursor", nextCursor);
//
//		// 완성된 Map을 응답으로 리턴
//		return response;
//	}
	
	@GetMapping("/anomalies")
	public Map<String, Object> getAnomaliesList(
	        @RequestParam(defaultValue = "50") int limit,
	        @RequestParam(required = false) Long cursor,
	        @RequestParam(required = false) String epcLot,
	        @RequestParam(required = false) String epcCode) {

	    // 커서 방식 서비스 호출 (fileId 파라미터 없음)
	    List<AnalyzedTripDTO> dtoList = dashboardService.getAnomaliesCursor(limit, cursor, epcCode, epcLot);

	    Map<String, Object> response = new HashMap<>();
	    response.put("data", dtoList);

	    // 커서: roadId가 제일 마지막 것
	    Long nextCursor = (!dtoList.isEmpty()) ? dtoList.get(dtoList.size() - 1).getRoadId() : null;
	    response.put("nextCursor", nextCursor);
	    response.put("pageSize", limit);
	    response.put("hasNext", dtoList.size() == limit);

	    return response;
	}
	
//	@GetMapping("/allanomalies")
	
}
