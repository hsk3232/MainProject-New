package edu.pnu.dto;

import java.util.List;

import edu.pnu.domain.EventHistory;
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
public class NodeDTO {
	private String hubType;
	private String scanLocation;
	private String businessStep;
	private List<Double> coord;
	
	public static NodeDTO fromEntity(Location l, EventHistory e) {
		return NodeDTO.builder()
				.hubType(e.getHubType())
				.scanLocation(l.getScanLocation())
				.businessStep(e.getBusinessStep())
				.coord(List.of(l.getLongitude(), l.getLatitude()))
				.build();
	}
}
