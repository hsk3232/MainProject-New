package edu.pnu.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.pnu.dto.AnalyzedTripDTO;
import edu.pnu.dto.ByProductDTO;
import edu.pnu.dto.FilterDTO;
import edu.pnu.dto.FromToDTO;
import edu.pnu.dto.InventoryDTO;
import edu.pnu.dto.NodeDTO;
import edu.pnu.service.DashboardService;
import edu.pnu.service.statistics.StatisticsFindService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/manager")
public class DashboardController {
	
	private final DashboardService dashboardService;
	private final StatisticsFindService statisticsFindService;
	
	@GetMapping("/nodes")
	public List<NodeDTO> getNodeList(){
		log.info("[진입] : [NodeController] Node 정보 전달 진입");
		
		List<NodeDTO> dto = dashboardService.getNodeList();
		log.info("[성공] : [NodeController] Node 정보 전달 성공");
		return dto;
	}
	
	@GetMapping("/anomalies")
	public Map<String, Object> getAnomaliesList(
	        @RequestParam(defaultValue = "50") int limit,
	        @RequestParam(required = false) Long cursor,
	        @RequestParam Long fileId) {
		
		 // 커서 방식 서비스 호출 (fileId 파라미터 없음)
	    List<AnalyzedTripDTO> dtoList = dashboardService.getAnomaliesList(limit, cursor, fileId);

	    Map<String, Object> response = new HashMap<>();
	    response.put("data", dtoList);

	    // 커서: roadId가 제일 마지막 것
	    Long nextCursor = (!dtoList.isEmpty()) ? dtoList.get(dtoList.size() - 1).getRoadId() : null;
	    response.put("nextCursor", nextCursor);
	    response.put("pageSize", limit);
	    response.put("hasNext", dtoList.size() == limit);

	    return response;
		
	}
	
	@GetMapping("/allanomalies")
	public Map<String, Object> getAllAnomaliesList(@RequestParam Long fileId) {
		

	    // 커서 방식 서비스 호출 (fileId 파라미터 없음)
	    List<AnalyzedTripDTO> dtoList = dashboardService.getAllAnomaliesList(fileId);

	    Map<String, Object> response = new HashMap<>();
	    response.put("data", dtoList);

	    return response;
	}
	


	@GetMapping("/trips")
	public Map<String, Object> getAnomaliesList(
	        @RequestParam(defaultValue = "50") int limit,
	        @RequestParam(required = false) Long cursor,
	        @RequestParam(required = false) String epcLot,
	        @RequestParam(required = false) String epcCode,
	        @RequestParam Long fileId) {
		

	    // 커서 방식 서비스 호출 (fileId 파라미터 없음)
	    List<AnalyzedTripDTO> dtoList = dashboardService.getTripList(limit, cursor, epcCode, epcLot, fileId);

	    Map<String, Object> response = new HashMap<>();
	    response.put("data", dtoList);

	    // 커서: roadId가 제일 마지막 것
	    Long nextCursor = (!dtoList.isEmpty()) ? dtoList.get(dtoList.size() - 1).getRoadId() : null;
	    response.put("nextCursor", nextCursor);
	    response.put("pageSize", limit);
	    response.put("hasNext", dtoList.size() == limit);

	    return response;
	}
	
	@GetMapping("/trips/filter")
	public FilterDTO getFilterList (@RequestParam Long fileId){
		FilterDTO filterList = dashboardService.getFilterList(fileId);
		return filterList;
	}
	
	
	@GetMapping("/trips/from")
	public FromToDTO getToLocation (@RequestParam Long fileId, @RequestParam String scanLocation){
		FromToDTO ToList = dashboardService.getToLocation(fileId, scanLocation);
		return ToList;
	}
	
	
	@GetMapping("/inventory")
	public  ResponseEntity<Map<String, Object>> getInventoryDistribution(
			@RequestParam("fileId") Long fileId) {
        
        // 1. 서비스로부터 List<InventoryDTO>를 받습니다.
        List<InventoryDTO> inventoryList = statisticsFindService.getInventory(fileId);
        
        // 2. 최종 JSON 형식에 맞게 리스트를 "inventoryDistribution" 키를 가진 Map으로 감쌉니다.
        Map<String, Object> response = new HashMap<>();
        response.put("inventoryDistribution", inventoryList);
        
        // 3. Map을 반환하면, Spring Boot가 자동으로 원하시는 JSON 형식으로 변환해줍니다.
        return ResponseEntity.ok(response);
    }
	
	
	@GetMapping("/byproduct")
	public ResponseEntity<Map<String, Object>> getByProduct(
            @RequestParam("fileId") Long fileId) {
		List<ByProductDTO> byProductList = statisticsFindService.getByProduct(fileId);
		Map<String, Object> response = new HashMap<>();
        response.put("byProductList", byProductList);
        return ResponseEntity.ok(response);
	}
	
	
	
}
