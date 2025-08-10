package edu.pnu.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Builder
@EqualsAndHashCode(of = "locationId")
public class Location {
	@Id
	private Long locationId;
	
	private String scanLocation;
	private double latitude;
	private double longitude;
	
	private Long operatorId;
	private Long deviceId;
}
