package edu.pnu.service.statistics;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import edu.pnu.Repo.AiDataRepository;
import edu.pnu.Repo.AssetProductRepository;
import edu.pnu.Repo.EventHistoryRepository;
import edu.pnu.domain.AiData;
import edu.pnu.domain.AssetProduct;
import edu.pnu.domain.Epc;
import edu.pnu.domain.EventHistory;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class FindAnomalyComponet implements StatisticsInterface {

	private final EventHistoryRepository eventHistoryRepo;
	private final AssetProductRepository assetProductRepo;
	private final AiDataRepository aiDataRepo;

	@Override
	public String getProcessorName() {
		return "이상 종류 판별";
	}

	@Override
	public void process(Long fileId) {
		log.info("[시작] Product 이상 분석 완료 코드 분류 작업 시작 - fileId: {}", fileId);

		// [1] 자산 제품 목록 조회
		List<AssetProduct> assetProducts = assetProductRepo.findAll();

		// [1-1] 완전 일치 조합 (3개 다 일치) 저장용 Set
		Set<Triples> fullMatchSet = assetProducts.stream()
				.map(p -> new Triples(
						safe(p.getEpcProduct()),
						safe(p.getEpcCompany()),
						safe(p.getProductName())))
				.collect(Collectors.toSet());

		// [1-2] 개별 필드 기준 일치 체크용 Set (safe 적용!)
		Set<String> knownProducts = assetProducts.stream().map(p -> safe(p.getEpcProduct())).collect(Collectors.toSet());
		Set<String> knownCompanies = assetProducts.stream().map(p -> safe(p.getEpcCompany())).collect(Collectors.toSet());
		Set<String> knownNames = assetProducts.stream().map(p -> safe(p.getProductName())).collect(Collectors.toSet());

		// [2] anomaly == true인 EventHistory 조회
		List<EventHistory> events = eventHistoryRepo.findAllByCsv_FileIdWithEpcAndProduct(fileId).stream()
				.filter(EventHistory::isAnomaly)
				.toList();

		List<AiData> result = new ArrayList<>();

		// [3] 이벤트별 판단
		for (EventHistory event : events) {
			Epc epc = event.getEpc();

			String epcProduct = safe(epc.getProduct().getEpcProduct());
			String epcCompany = safe(epc.getProduct().getEpcCompany());
			String productName = safe(epc.getProduct().getProductName());

			Triples current = new Triples(epcProduct, epcCompany, productName);

			// [3-1] 완전 일치 → anomaly인데도 불구하고 정상임 → error로 기록
			if (fullMatchSet.contains(current)) {
				AiData errorData = AiData.builder()
						.eventHistory(event)
						.anomalyType("error")
						.build();
				result.add(errorData);
				continue;
			}

			// [3-2] 개별 필드 하나라도 일치하면 → tamper
			boolean productMatch = knownProducts.contains(epcProduct);
			boolean companyMatch = knownCompanies.contains(epcCompany);
			boolean nameMatch = knownNames.contains(productName);

			String anomalyType;
			if (productMatch || companyMatch || nameMatch) {
				anomalyType = "tamper";
			} else {
				anomalyType = "fake";
			}

			AiData data = AiData.builder()
					.eventHistory(event)
					.anomalyType(anomalyType)
					.build();

			result.add(data);
		}

		// [4] 저장
		aiDataRepo.saveAll(result);
		log.info("[완료] Product 이상 분석 완료 - 저장된 AiData 수: {}", result.size());
	
	
		
	
	}

	@Override
	public int getOrder() {
		return 2;
	}

	//비교용 value class - equals/hashCode 포함
	@Getter
	@AllArgsConstructor
	@EqualsAndHashCode
	private static class Triples {
		private String epcProduct;
		private String epcCompany;
		private String productName;
	}

	// 공백, null, 대소문자 차이 제거용 정제 함수
	private String safe(String input) {
	    return input == null ? "" : input.trim().toLowerCase();
	}

}
