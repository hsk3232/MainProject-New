package edu.pnu.service.statistics;

import java.util.List;

import org.springframework.stereotype.Service;

import edu.pnu.domain.KPIAnalysis;
import edu.pnu.dto.ByProductDTO;
import edu.pnu.dto.InventoryDTO;
import edu.pnu.dto.KPIExportDTO;
import edu.pnu.repo.AiDataRepository;
import edu.pnu.repo.EventHistoryRepository;
import edu.pnu.repo.KPIAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsFindService {
	
	private final KPIAnalysisRepository kpiRepo;
	private final EventHistoryRepository eventHistoryRepo;
	private final AiDataRepository aiDataRepo; 
	
	public KPIExportDTO getKPIAnlaysis(Long fileId) {
		log.info("[진입] : [StatisticsFindService] KPI 전송 서비스");
		KPIAnalysis k = kpiRepo.findByCsv_FileId(fileId);
		KPIExportDTO dto = KPIExportDTO.fromEntity(k);
		return dto;
	}
	
	public List<InventoryDTO> getInventory (Long fileId){
		log.info("[진입] : [StatisticsFindService] Inventory 전송 서비스");
		return eventHistoryRepo.calculateInventoryByBusinessStep(fileId);
	}
	
	public List<ByProductDTO> getByProduct (Long fileId){
		log.info("[진입] : [StatisticsFindService] ByProduct 전송 서비스");
		return aiDataRepo.countByProduct(fileId);
	}
}
