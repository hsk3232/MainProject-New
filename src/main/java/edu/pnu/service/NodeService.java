package edu.pnu.service;

import java.util.List;

import org.springframework.stereotype.Service;

import edu.pnu.Repo.EventHistoryRepository;
import edu.pnu.Repo.LocationRepository;
import edu.pnu.domain.EventHistory;
import edu.pnu.dto.NodeDTO;
import edu.pnu.exception.NodeNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NodeService {
	
	private final LocationRepository locationRepo;
	private final EventHistoryRepository eventHistoryRepo;
	
	public List<NodeDTO> getNodeList(){
		// event(리스트)를 stream(for-each와 동일)으로 하나씩 꺼내서
		// 각 요소(e)를 NodeDTO로 변환해 fromEntity에 집어 넣고
		// toList로 로 만듦
		List<NodeDTO> list = locationRepo.findAll().stream()
		        .map(l -> {
		            // 각 Location에 연결된 EventHistory들 중
		            // 대표값(예: 가장 최근 EventHistory)을 고른다
		            EventHistory e = eventHistoryRepo
		                .findFirstByLocationOrderByEventTimeDesc(l) // 직접 구현 or JPA 쿼리 메소드
		                .orElse(null);
		            if (e == null) return null; // EventHistory 없는 Location은 무시 or 예외처리
		            return NodeDTO.fromEntity(l, e);
		        })
		        .filter(dto -> dto != null) // EventHistory 없는 Location 제외
		        .toList();
		
		if (list.isEmpty()) {
			log.error("[오류] :[NodeService] Node 정보가 비어 있음");
	        throw new NodeNotFoundException("노드 데이터가 존재하지 않습니다.");
	    }
		 return list;
	}
}
