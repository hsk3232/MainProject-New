package edu.pnu.repo.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import edu.pnu.domain.AiData;
import edu.pnu.domain.EventHistory;
import edu.pnu.dto.FilterDTO;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;


public class EventHistorySpecification {

	public static Specification<EventHistory> withFilters(FilterDTO filters) {
        // Specification은 람다 표현식으로 구현됩니다.
        return (root, query, criteriaBuilder) -> {
            
            // 모든 동적 조건들을 담을 리스트를 생성합니다.
            List<Predicate> predicates = new ArrayList<>();

            // 1. scanLocations 필터
            if (filters.getScanLocations() != null && !filters.getScanLocations().isEmpty()) {
                predicates.add(root.get("location").get("scanLocation").in(filters.getScanLocations()));
            }

            // 2. eventTimeRange 필터 (시작 시간과 끝 시간)
            if (filters.getEventTimeRange() != null && filters.getEventTimeRange().size() == 2) {
                if (filters.getEventTimeRange().get(0) != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("eventTime"), filters.getEventTimeRange().get(0)));
                }
                if (filters.getEventTimeRange().get(1) != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("eventTime"), filters.getEventTimeRange().get(1)));
                }
            }

            // 3. businessSteps 필터
            if (filters.getBusinessSteps() != null && !filters.getBusinessSteps().isEmpty()) {
                predicates.add(root.get("businessStep").in(filters.getBusinessSteps()));
            }
            
            // 4. productNames 필터 (JOIN 필요)
            if (filters.getProductNames() != null && !filters.getProductNames().isEmpty()) {
                predicates.add(root.join("epc").join("product").get("productName").in(filters.getProductNames()));
            }

            // 5. eventTypes 필터
            if (filters.getEventTypes() != null && !filters.getEventTypes().isEmpty()) {
                predicates.add(root.get("eventType").in(filters.getEventTypes()));
            }

            // 6. anomalyTypes 필터 (Subquery를 이용한 EXISTS 조건)
            if (filters.getAnomalyTypes() != null && !filters.getAnomalyTypes().isEmpty()) {
                Subquery<Integer> subquery = query.subquery(Integer.class);
                Root<AiData> aiDataRoot = subquery.from(AiData.class);
                subquery.select(criteriaBuilder.literal(1)) // 1을 선택
                       .where(
                           // 서브쿼리의 AiData가 메인쿼리의 EventHistory와 연결되는지 확인
                           criteriaBuilder.equal(aiDataRoot.get("eventHistory"), root),
                           // 서브쿼리의 AiData의 anomalyType이 조건에 맞는지 확인
                           aiDataRoot.get("anomalyType").in(filters.getAnomalyTypes())
                       );
                predicates.add(criteriaBuilder.exists(subquery));
            }
            
            // 모든 조건들을 'AND'로 묶어 최종 Predicate를 반환합니다.
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
