package edu.pnu.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Builder
@Table(name="eventhistory")
public class EventHistory {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long eventId;
	
	//N:1 productId와 연결됨
	@ManyToOne(fetch = FetchType.LAZY) // FK
	//@JoinColumn(name = 현재 테이블에 생길 새로운 칼럼명, referenced = 참조할 테이블의 PK)
    @JoinColumn(name = "epc_code") //MySQL 칼럼 기준
	// EventHistory 테이블에 생길 새로운 칼럼명, 참조할 테이블의 PK
	private Epc epc;
	
	
	//N:1 locationId와 연결됨
	@ManyToOne(fetch = FetchType.LAZY) // FK
    @JoinColumn(name = "location_id") //MySQL 칼럼 기준
	// EventHistory 테이블에 생길 새로운 칼럼명, 참조할 테이블의 PK
	private Location location;
	
	private String hubType;
	private String businessStep;
	private String businessOriginal;
	private String eventType;
	private LocalDateTime eventTime;

	
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private Csv csv;
    
    @Builder.Default
    private boolean anomaly = false;
    
   
    
    
    
}
