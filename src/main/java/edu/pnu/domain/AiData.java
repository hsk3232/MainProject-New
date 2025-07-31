package edu.pnu.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "aidata")
public class AiData {
	@Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	Long aiId;
	
	
	 @ManyToOne
     @JoinColumn(name = "event_id")
     private EventHistory eventHistory;

	
	private String anomalyType;
	
	 @ManyToOne
     @JoinColumn(name = "road_id")
     private AnalyzedTrip analyzedTrip;
	 
	 @ManyToOne
	 @JoinColumn(name="file_id")
	 private Csv csv;

	
}
