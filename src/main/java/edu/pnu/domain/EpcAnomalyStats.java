package edu.pnu.domain;

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
@Table(name="epcanomalystats")

// AI 모듈에서 넘어온 EPC 코드별 이상치 통계

public class EpcAnomalyStats {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="epc_code")
	private Epc epc;
	
	@Builder.Default
	private int totalEvents = 0;
	
	@Builder.Default
	private int jumpCount = 0;
	
	@Builder.Default
	private int evtOrderErrCount = 0;
	
	@Builder.Default
	private int epcFakeCount = 0;
	
	@Builder.Default
	private int epcDupCount = 0;
	
	@Builder.Default
	private int locErrCount = 0;
}
