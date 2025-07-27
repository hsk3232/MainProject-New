package edu.pnu.service.statistics;

import java.util.List;

import org.springframework.stereotype.Service;

import edu.pnu.Repo.KPIAnalysisRepository;
import edu.pnu.domain.KPIAnalysis;
import edu.pnu.dto.KPIExportDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsFindService {
	
	private final KPIAnalysisRepository kpiRepo;
	
	public KPIExportDTO getKPIAnlaysis(Long fileId) {
		KPIAnalysis k = kpiRepo.findByCsv_FileId(fileId);
		KPIExportDTO dto = KPIExportDTO.fromEntity(k);
		return dto;
	}
	
//	public List<>
}
