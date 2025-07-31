package edu.pnu.dto;

import java.time.LocalDateTime;
import java.util.List;

import edu.pnu.domain.EventHistory;
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
public class FilterDTO {
	private List<String> scanLocations;
	private List<LocalDateTime> eventTimeRange;
	private List<String> businessSteps;
	private List<String> productNames;
	private List<String> eventTypes;
	private List<String> anomalyTypes;
	
	
}
