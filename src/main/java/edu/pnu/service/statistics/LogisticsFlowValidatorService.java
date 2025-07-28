package edu.pnu.service.statistics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import edu.pnu.domain.EventHistory;
import edu.pnu.domain.Location;
import edu.pnu.repo.RouteRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LogisticsFlowValidatorService {

    // --- 의존성 주입 필드 ---
    private final RouteRepository routeRepo;
    
    // --- 클래스 상태 필드 ---
    // DB에서 로드한 모든 유효 규칙을 메모리에 캐싱하는 Set
    private final Set<TransitionRule> validRules;
    
    // --- 정적 필드 ---
    // 공장과 전용 창고의 ID 매핑. 이 규칙은 자주 바뀌지 않으므로 static으로 유지해도 좋습니다.
	private static final Map<Long, Long> factoryToWarehouseMap = new HashMap<>();
	static {
		factoryToWarehouseMap.put(2L, 7L); // 화성공장(2) -> 화성공장창고(7)
        factoryToWarehouseMap.put(1L, 6L);   // 양산공장(1) -> 양산공장창고(6)
        factoryToWarehouseMap.put(3L, 8L);   // 구미공장(3) -> 구미공장창고(8)
        factoryToWarehouseMap.put(4L, 9L);   // 인천공장(4) -> 인천공장창고(9)
	}

    // [개선] 생성자에서 주입받은 Repository를 사용하여 규칙을 초기화합니다.
    // Spring이 이 Service를 생성할 때 단 한 번만 호출됩니다.
    public LogisticsFlowValidatorService(RouteRepository routeRepo) {
        this.routeRepo = routeRepo;
        this.validRules = routeRepo.findAll().stream()
                .map(route -> new TransitionRule(
                        route.getFromBusinessStep(),
                        route.getToBusinessStep(),
                        route.getFromLocationId(),
                        route.getToLocationId()
                        ))
                .collect(Collectors.toSet());
        log.info("[LogisticsFlowValidatorService] DB로부터 {}개의 물류 규칙을 로드했습니다.", validRules.size());
    }

    // 규칙을 표현하는 내부 레코드(Record). 이 서비스 내부에서만 사용됩니다.
    private record TransitionRule(String fromStep, String toStep, Long fromLocId, Long toLocId) {}

    /**
     * 한 EPC의 전체 이력을 받아, 물류 경로 규칙을 위반한 이벤트들의 ID를 반환합니다.
     * @param history 시간순으로 정렬된 한 EPC의 전체 EventHistory 리스트
     * @return 규칙을 위반한 이벤트(들)의 ID Set
     */
	public Set<Long> findViolations(List<EventHistory> history) {
		Set<Long> violations = new HashSet<>();
		if (history == null || history.isEmpty()) return violations;
		
        // 규칙 1: 전체 이력의 가장 첫 이벤트가 'Factory'가 아니면, 그 시작점은 위반(clone)이다.
		EventHistory first = history.get(0);
		if (!"Factory".equals(first.getBusinessStep())) {
		    violations.add(first.getEventId());
		} else {
		    first.setAnomaly(false); // 또는 "error"로 표시할 수 있도록 외부에서 처리되도록 설계
		}
		if (history.size() < 2) return violations;

        // 규칙 2: 모든 이벤트를 순회하며 'from -> to' 전환이 유효한지 검사한다.
		for (int i = 1; i < history.size(); i++) {
			EventHistory from = history.get(i - 1);
			EventHistory to = history.get(i);
			
            // 규칙 3: 어떤 단계에서도 'Factory'로 되돌아갈 수 없다 (경로 역주행).
			if ("Factory".equals(to.getBusinessStep())) {
				violations.add(to.getEventId());
				continue;
			}
			if (!isValidTransition(from, to)) {
				violations.add(to.getEventId());
			}
		}
		return violations;
	}

	// 두 이벤트 사이의 이동이 '규칙집(validRules)'에 명시되어 있는지 판별합니다.
	private boolean isValidTransition(EventHistory from, EventHistory to) {
		Location fromLoc = from.getLocation();
		Location toLoc = to.getLocation();
		if (from.getBusinessStep() == null || to.getBusinessStep() == null || fromLoc == null || toLoc == null) return false;
		
        // 실제 발생한 이동 경로를 '규칙' 객체 형태로 만듭니다.
        TransitionRule actualTransition = new TransitionRule(
            from.getBusinessStep(), 
            to.getBusinessStep(), 
            fromLoc.getLocationId(),
            toLoc.getLocationId()
        );
        
        // 이 실제 이동 경로가, 메모리에 있는 유효한 규칙 목록(validRules)에 포함되는지 확인합니다.
        // 이것이 바로 "규칙집을 대조하는" 핵심 로직입니다.
        return validRules.contains(actualTransition);
	}
}