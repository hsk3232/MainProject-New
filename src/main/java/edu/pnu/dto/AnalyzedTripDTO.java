package edu.pnu.dto;

import java.util.List;

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

	private String epcCode;
	private String productName;
	private String epcLot;
	private String eventType;

	private String anomaly;
	private String anomalyDescription;
	
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
}
