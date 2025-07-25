package edu.pnu.service.statistics;

import java.util.Map;

import org.springframework.stereotype.Component;

import edu.pnu.Repo.AiDataRepository;
import edu.pnu.Repo.EventHistoryRepository;
import edu.pnu.Repo.KPIAnalysisRepository;
import edu.pnu.domain.Csv;
import edu.pnu.domain.KPIAnalysis;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class KPIAnalysisComponet implements StatisticsInterface {
	
	private final EventHistoryRepository eventHistoryRepo;
	private final AiDataRepository aiDataRepo;
	private final KPIAnalysisRepository kpiAnalysisRepo;

	@Override
	public String getProcessorName() {
		return "KPI 분석";
	}

	@Override
	public void process(Long fileId) {
		 // 1. KPI 집계 한 번에 조회
        Map<String, Object> result = eventHistoryRepo.getKpiAggregates(fileId);

       
        // 3. KPIAnalysis 엔티티 저장
        KPIAnalysis kpi = KPIAnalysis.builder()
            .totalTripCount(((Number) result.get("totalTripCount")).longValue())
            .uniqueProductCount(((Number) result.get("uniqueProductCount")).intValue())
            .codeCount(((Number) result.get("codeCount")).longValue())
            .anomalyCount(((Number) result.get("anomalyCount")).longValue())
            // salesRate, dispatchRate, inventoryRate 등은 아래에서 별도 계산
            .build();

        // (추가: salesRate, dispatchRate, inventoryRate, avgLeadTime)
        // 값이 null인 경우 0 처리
        Long salesCount = getLong(result, "salesCount");
        Long warehouseIn = getLong(result, "warehouseIn");
        Long warehouseOut = getLong(result, "warehouseOut");
        Double avgLeadTime = getDouble(result, "avgLeadTime");

        // 계산식(0 divide by 0 방지)
        double salesRate = (kpi.getCodeCount() > 0 && salesCount != null) ? (salesCount * 100.0 / kpi.getCodeCount()) : 0.0;
        double dispatchRate = (warehouseIn != null && warehouseIn > 0 && warehouseOut != null) ? (warehouseOut * 100.0 / warehouseIn) : 0.0;
        double inventoryRate = 0.0; // 별도 쿼리 필요시 추가
        double anomalyRate = (kpi.getTotalTripCount() > 0) ? (kpi.getAnomalyCount() * 1.0 / kpi.getTotalTripCount()) : 0.0;

        kpi.setSalesRate(salesRate);
        kpi.setDispatchRate(dispatchRate);
        kpi.setInventoryRate(inventoryRate);
        kpi.setAvgLeadTime(avgLeadTime != null ? avgLeadTime : 0.0);
        kpi.setAnomalyRate(anomalyRate);
        kpi.setCsv(Csv.builder().fileId(fileId).build());

        kpiAnalysisRepo.save(kpi);
        log.info("[KPI] KPIAnalysis 저장 완료: {}", kpi);
    }

    @Override
    public int getOrder() {
        return 3;
    }

    // Map에서 안전하게 Long, Double 추출
    private Long getLong(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? ((Number) val).longValue() : 0L;
    }
    private Double getDouble(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? ((Number) val).doubleValue() : 0.0;
    }
}