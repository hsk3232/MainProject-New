package edu.pnu.service.statistics;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import edu.pnu.domain.AnalyzedTrip;
import edu.pnu.repo.RouteRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LogisticsFlowValidatorService {

    private final Set<TransitionRule> validRules;

    /**
     * 경로 규칙을 표현하는 내부 레코드.
     * event_type을 포함하여 규칙 대조의 정확도를 높였습니다.
     */
    private record TransitionRule(
        String fromBusinessStep, String toBusinessStep,
        String fromEventType, String toEventType,
        Long fromLocationId, Long toLocationId) {}

    /**
     * 생성자: 서비스가 생성될 때 DB에서 모든 유효 경로 규칙을 한 번만 로드하여 메모리에 저장합니다.
     * @param routeRepo '정상경로란다.csv'에 매핑된 Repository
     */
    public LogisticsFlowValidatorService(RouteRepository routeRepo) {
        // '정상경로란다.csv'의 모든 데이터를 TransitionRule 형태로 변환하여 Set에 저장합니다.
        this.validRules = routeRepo.findAll().stream()
                .map(route -> new TransitionRule(
                        route.getFromBusinessStep(),
                        route.getToBusinessStep(),
                        route.getFromEventType(),
                        route.getToEventType(),
                        route.getFromLocationId().getLocationId(),
                        route.getToLocationId().getLocationId()))
                .collect(Collectors.toSet());
        log.info("[LogisticsFlowValidatorService] DB로부터 {}개의 상세 물류 규칙을 로드했습니다.", validRules.size());
    }


    public Set<Long> findViolations(List<AnalyzedTrip> trips) {
        Set<Long> violations = new HashSet<>();
        if (trips == null || trips.isEmpty()) {
            return violations;
        }

        // --- 검증 규칙 1: 첫 출발지 검사 ---
        AnalyzedTrip firstTrip = trips.get(0);
        if (!"Factory".equals(firstTrip.getFromBusinessStep()) || !"Aggregation".equals(firstTrip.getFromEventType())) {
            // 제품의 첫 시작은 반드시 'Factory'의 'Aggregation'이어야 함
            violations.add(firstTrip.getRoadId());
        }

        // --- 검증 규칙 2 & 3: 전체 경로 순회 검사 ---
        for (int i = 0; i < trips.size(); i++) {
            AnalyzedTrip currentTrip = trips.get(i);

            // 2. 개별 경로 유효성 검사: 현재 경로가 그 자체로 유효한 규칙인지 확인
            TransitionRule actualRule = new TransitionRule(
                currentTrip.getFromBusinessStep(), currentTrip.getToBusinessStep(),
                currentTrip.getFromEventType(), currentTrip.getToEventType(),
                currentTrip.getFromLocation().getLocationId(), 
                currentTrip.getToLocation().getLocationId()
            );

            if (!validRules.contains(actualRule)) {
                violations.add(currentTrip.getRoadId());
            }

            // 3. 이전 경로와의 '연결성' 검사 (두 번째 경로부터)
            if (i > 0) {
                AnalyzedTrip previousTrip = trips.get(i - 1);

                // 이전 경로의 '도착지' 정보가 현재 경로의 '출발지' 정보와 완벽하게 일치하는지 확인
                boolean isConnected = Objects.equals(previousTrip.getToBusinessStep(), currentTrip.getFromBusinessStep()) &&
                                      Objects.equals(previousTrip.getToEventType(), currentTrip.getFromEventType()) &&
                                      Objects.equals(previousTrip.getToLocation().getLocationId(), currentTrip.getFromLocation().getLocationId());

                if (!isConnected) {
                    // 경로의 연결이 끊어졌으므로 현재 경로를 비정상으로 판단
                    violations.add(currentTrip.getRoadId());
                }
            }
        }

        return violations;
    }
}