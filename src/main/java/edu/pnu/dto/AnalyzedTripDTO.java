package edu.pnu.dto;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import edu.pnu.domain.AnalyzedTrip;
import edu.pnu.domain.Location;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyzedTripDTO {
	private TripPoint from;
	private TripPoint to;
	
	private Long fileId;
	private String epcCode;
	private String productName;
	private String epcLot;
	private String eventType;
	private Long roadId;

	private List<String> anomalyTypeList;

	
	@Getter
	@Setter
	@ToString
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class TripPoint {  // -> from과 to 안에 들어갈 내용을 
        private String scanLocation;
        private List<Double> coord; // [longitude, latitude]
        private Long eventTime;
        private String businessStep;
    } 
	
	public static AnalyzedTripDTO fromEntity(AnalyzedTrip a) {
		 Location fromLoc = a.getFromLocation();
		    Location toLoc = a.getToLocation();

		    TripPoint from = TripPoint.builder()
		        .scanLocation(fromLoc != null ? fromLoc.getScanLocation() : null)
		        .coord(fromLoc != null ? List.of(fromLoc.getLongitude(), fromLoc.getLatitude()) : null)
		        .eventTime(a.getFromEventTime() != null ? a.getFromEventTime().toEpochSecond(ZoneOffset.UTC) : null)
		        .businessStep(a.getFromBusinessStep())
		        .build();

		    TripPoint to = TripPoint.builder()
		        .scanLocation(toLoc != null ? toLoc.getScanLocation() : null)
		        .coord(toLoc != null ? List.of(toLoc.getLongitude(), toLoc.getLatitude()) : null)
		        .eventTime(a.getToEventTime() != null ? a.getToEventTime().toEpochSecond(ZoneOffset.UTC) : null)
		        .businessStep(a.getToBusinessStep())
		        .build();

		    return AnalyzedTripDTO.builder()
		        .from(from)
		        .to(to)
		        .epcCode(a.getEpc().getEpcCode())
		        .productName(a.getEpc().getProduct().getProductName())
		        .epcLot(a.getEpc().getEpcLot())
		        .eventType(a.getToEventType()) // 출고 or 도착 기준
		        .roadId(a.getRoadId())
		        .anomalyTypeList(new ArrayList<>()) // 후처리 시 채움
		        .build();
	}
}
