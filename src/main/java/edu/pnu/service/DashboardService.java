package edu.pnu.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import edu.pnu.domain.AiData;
import edu.pnu.domain.EventHistory;
import edu.pnu.dto.AnalyzedTripDTO;
import edu.pnu.dto.FilterDTO;
import edu.pnu.dto.FromToDTO;
import edu.pnu.dto.NodeDTO;
import edu.pnu.dto.ReportCoverDTO;
import edu.pnu.dto.TimeRangeDTO;
import edu.pnu.exception.NodeNotFoundException;
import edu.pnu.repo.AiDataRepository;
import edu.pnu.repo.AnalyzedTripRepository;
import edu.pnu.repo.CsvRepository;
import edu.pnu.repo.EventHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

	private final AiDataRepository aiDataRepo;

	private final EventHistoryRepository eventHistoryRepo;
	private final AnalyzedTripRepository analyzedTripRepo;
	private final CsvRepository csvRepo;
	
	

	// [node]
	public List<NodeDTO> getNodeList() {
	    List<NodeDTO> list = eventHistoryRepo.findLatestEventHistoryPerLocation();

	    if (list.isEmpty()) {
	        log.error("[오류] :[NodeService] Node 정보가 비어 있음");
	        throw new NodeNotFoundException("노드 데이터가 존재하지 않습니다.");
	    }
	    return list;
	}

	// [anomalies] 
	public List<AnalyzedTripDTO> getAnomaliesList(int limit, Long cursor, Long fileId) {
		log.info("[진입] :[DashboardService][getAnomaliesList] AnalyzedTripDTO 생성 진입 ");
		
	    Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "roadId"));
	 // 1. Trip DTO 쿼리로 추출
	    List<AnalyzedTripDTO> dtoList = analyzedTripRepo.getFindAnomaliesWithCursor(fileId, cursor, pageable);

	    // 2. roadId만 추출
	 // 2. fileId로 모든 관련 AiData 조회
	 // 2. fileId로 모든 관련 AiData 조회
	    List<AiData> aiDataForFile = aiDataRepo.findDistinctAnomalyTypesByFileId(fileId);

	    // 3. (EPC코드 + 이벤트시간)을 키로 사용하는 Map 생성
	    Map<String, List<String>> eventToAnomalyTypes = aiDataForFile.stream()
	        .collect(Collectors.groupingBy(
	            // 키 생성: EPC코드와 이벤트시간을 조합
	            ai -> {
	                EventHistory eh = ai.getEventHistory();
	                return eh.getEpc().getEpcCode() + "|" + eh.getEventTime().toString();
	            },
	            // 값 생성: anomalyType을 리스트로 수집
	            Collectors.mapping(AiData::getAnomalyType, Collectors.toList())
	        ));

	    // 4. Trip DTO에 anomalyTypeList 세팅
	    dtoList.forEach(dto -> {
	        // DTO의 도착점(to) 정보와 epoch second를 LocalDateTime으로 변환하여 키를 생성
	        LocalDateTime toEventTime = LocalDateTime.ofEpochSecond(dto.getTo().getEventTime(), 0, ZoneOffset.UTC);
	        String lookupKey = dto.getEpcCode() + "|" + toEventTime.toString();
	        
	        List<String> types = eventToAnomalyTypes.get(lookupKey);
	        if (types != null) {
	            dto.setAnomalyTypeList(types);
	        }
	    });

	    return dtoList;
	}
	
	
	// [all_anomalies]
	public List<AnalyzedTripDTO> getAllAnomaliesList (Long fileId) {
		log.info("[진입] :[DashboardService][getAllAnomaliesList] AnalyzedTripDTO 생성 진입 ");
		
	  
	 // 1. Trip DTO 쿼리로 추출
	    List<AnalyzedTripDTO> dtoList = analyzedTripRepo.getAllAnomaliesList(fileId);

	    // 2. roadId만 추출
	    List<AiData> aiDataForFile = aiDataRepo.findDistinctAnomalyTypesByFileId(fileId);

	    // 3. (EPC코드 + 이벤트시간)을 키로 사용하는 Map 생성
	    Map<String, List<String>> eventToAnomalyTypes = aiDataForFile.stream()
	        .collect(Collectors.groupingBy(
	            // 키 생성: EPC코드와 이벤트시간을 조합
	            ai -> {
	                EventHistory eh = ai.getEventHistory();
	                return eh.getEpc().getEpcCode() + "|" + eh.getEventTime().toString();
	            },
	            // 값 생성: anomalyType을 리스트로 수집
	            Collectors.mapping(AiData::getAnomalyType, Collectors.toList())
	        ));

	    // 4. Trip DTO에 anomalyTypeList 세팅
	    dtoList.forEach(dto -> {
	        // DTO의 도착점(to) 정보와 epoch second를 LocalDateTime으로 변환하여 키를 생성
	        LocalDateTime toEventTime = LocalDateTime.ofEpochSecond(dto.getTo().getEventTime(), 0, ZoneOffset.UTC);
	        String lookupKey = dto.getEpcCode() + "|" + toEventTime.toString();
	        
	        List<String> types = eventToAnomalyTypes.get(lookupKey);
	        if (types != null) {
	            dto.setAnomalyTypeList(types);
	        }
	    });

	    return dtoList;
	}
	
	
	// [trip]
	public List<AnalyzedTripDTO> getTripList(int limit, Long cursor, String epcCode, String epcLot, Long fileId) {
		log.info("[진입] :[DashboardService][getAnomaliesList] AnalyzedTripDTO 생성 진입 ");
		
	    Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "roadId"));
	 // 1. Trip DTO 쿼리로 추출
	    List<AnalyzedTripDTO> dtoList = analyzedTripRepo.getFindTripWithCursorAndSearch(
	            cursor, epcCode, epcLot, pageable, fileId);

	    // 2. roadId만 추출
	    List<AiData> aiDataForFile = aiDataRepo.findDistinctAnomalyTypesByFileId(fileId);

	    // 3. (EPC코드 + 이벤트시간)을 키로 사용하는 Map 생성
	    Map<String, List<String>> eventToAnomalyTypes = aiDataForFile.stream()
	        .collect(Collectors.groupingBy(ai -> {
                EventHistory eh = ai.getEventHistory();
                return eh.getEpc().getEpcCode() + "|" + eh.getEventTime().toString();
            },
            // 값 생성: anomalyType을 리스트로 수집
            Collectors.mapping(AiData::getAnomalyType, Collectors.toList())
        ));

	    // 4. Trip DTO에 anomalyTypeList 세팅
	    dtoList.forEach(dto -> {
	        // DTO의 도착점(to) 정보와 epoch second를 LocalDateTime으로 변환하여 키를 생성
	        LocalDateTime toEventTime = LocalDateTime.ofEpochSecond(dto.getTo().getEventTime(), 0, ZoneOffset.UTC);
	        String lookupKey = dto.getEpcCode() + "|" + toEventTime.toString();
	        
	        List<String> types = eventToAnomalyTypes.get(lookupKey);
	        if (types != null) {
	            dto.setAnomalyTypeList(types);
	        }
	    });

	    return dtoList;
	}
	
	
	// [trips/filter]
	 public FilterDTO getFilterList (Long fileId) {
		 log.info("[진입] :[DashboardService][getFilterList] FilterDTO 생성 진입 ");
	     
		 // 1. 각 Repository를 통해 필터링에 필요한 목록들을 병렬로 조회합니다.
	        List<String> scanLocations = eventHistoryRepo.findDistinctScanLocationsByFileId(fileId);
	        TimeRangeDTO timeRange = eventHistoryRepo.findEventTimeRangeByFileId(fileId);
	        List<String> businessSteps = eventHistoryRepo.findDistinctBusinessStepsByFileId(fileId);
	        List<String> productNames = eventHistoryRepo.findDistinctProductNamesByFileId(fileId);
	        List<String> eventTypes = eventHistoryRepo.findDistinctEventTypesByFileId(fileId);
	        List<String> anomalyTypes = aiDataRepo.findDistincByFileId(fileId);

	        // 2. eventTimeRange 필드를 [최소시간, 최대시간] 형태의 리스트로 변환합니다.
	        List<LocalDateTime> eventTimeList = new ArrayList<>();
	        if (timeRange != null && timeRange.minTime() != null) {
	            eventTimeList.add(timeRange.minTime());
	            eventTimeList.add(timeRange.maxTime());
	        }

	        // 3. Builder를 사용하여 조회된 모든 목록을 하나의 FilterDTO 객체로 조합합니다.
	        return FilterDTO.builder()
	                .scanLocations(scanLocations)
	                .eventTimeRange(eventTimeList)
	                .businessSteps(businessSteps)
	                .productNames(productNames)
	                .eventTypes(eventTypes)
	                .anomalyTypes(anomalyTypes)
	                .build();
	    }
	 
	 // [trip/from]
	 public FromToDTO getToLocation (Long fileId, String scanLocation) {
		 log.info("[진입] :[DashboardService][getFilterList] getToLocation 생성 진입 ");
		  List<String> toScanLocation = analyzedTripRepo.findDistinctToScanLocationsByFromScanLocation(fileId, scanLocation);
		  return FromToDTO.builder()
				  .toLocation(toScanLocation)
				  .build();
	 }
	 
	 
	 public ReportCoverDTO getReportCover(Long fileId) {
	        Optional<ReportCoverDTO> optDto = csvRepo.findReportCoverByFileId(fileId);
	        if (optDto.isEmpty()) {
	            throw new RuntimeException("해당 파일 ID에 대한 보고서 정보를 찾을 수 없습니다: " + fileId);
	        }
	        return optDto.get();  // periodStart, periodEnd는 DTO 내에서 getPeriod()로 리스트 반환 가능
	    }       
}
