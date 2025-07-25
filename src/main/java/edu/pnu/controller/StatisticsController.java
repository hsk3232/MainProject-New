package edu.pnu.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.pnu.dto.KPIExportDTO;
import edu.pnu.exception.NoDataFoundException;
import edu.pnu.service.statistics.StatisticsFindService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager")
public class StatisticsController {
	
	private final StatisticsFindService statisticsFindService;

	@GetMapping("/kpi")
	public List<KPIExportDTO> getKPIAnlaysis(@RequestParam Long fileId) {
		log.info("[진입] : [StatisticsController] Kpi 정보 load 진입");
		List<KPIExportDTO> dto = statisticsFindService.getKPIAnlaysis(fileId);
		if(dto.isEmpty()) {
			throw new NoDataFoundException("[오류] : [StatisticsController] KPI 조회 List가 비었음.");
		}
		log.info("[성공] : [StatisticsController] KPI 정보 Load 성공");
		return dto;
	}
	
}
