package edu.pnu.service.datashare;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.pnu.Repo.EventHistoryRepository;
import edu.pnu.dto.dataShere.ImportAiDataDTO;
import edu.pnu.dto.dataShere.ImportDatafromAiDTO;
import edu.pnu.exception.NoDataFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataApplyService   {
	private final EventHistoryRepository eventHistoryRepo;
	
	@PersistenceContext
    private EntityManager em;
	
	// ■■■■■■■■■■■■■■■■ 결과 반영 (DB 업데이트) ■■■■■■■■■■■■■■■■
		@Transactional
		public void applyAnomalyResult(ImportDatafromAiDTO importData) {
			log.info("[진입] : [DataShareService] AI로부터 받은 이상치 결과 DB 반영 시작");
			
			if (importData.getEventHistory() == null || importData.getEventHistory().isEmpty()) {
			    throw new NoDataFoundException("[DataShareService] 수신 데이터 비어있음");
			}
			
			if (importData.getEventHistory() != null) {
				List<ImportAiDataDTO> dtoList = importData.getEventHistory();
				
				System.out.println(dtoList.get(1).getEventId());
				
				log.info("[진행] : [DataShareService] AiData 업데이트 대상 건수 = {}", dtoList.size());

				// [1-1] eventId 목록 추출
				List<Long> eventIds = dtoList.stream().map(ImportAiDataDTO::getEventId).toList();

				// 여기서 bulk update으로 한번에 저장
		        int updatedCount = eventHistoryRepo.bulkUpdatseAnomaly(eventIds);
		        em.clear(); // 영속성 컨텍스트 비우기

		        log.info("[완료] anomaly 업데이트 적용 ({}건), 영속성 컨텍스트 clear!", updatedCount);


			
			}
		}
}
