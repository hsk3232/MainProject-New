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
@Table(name = "kpianalysis")
public class KPIAnalysis {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long KpiId;

	// 필터 조건에 맞는 전체 이동(Trip) 건수 (EventHistory의 row 수, 이동 event의 카디널리티)
	private Long totalTripCount; // ex) 854320

	// 필터 조건 내 고유한 상품(productName)의 총 종류 수
	// (ex: 각 공장별 생산 상품 종류, epc_product 기준)
	private int uniqueProductCount; // ex) 128

	// 총 생산량 (생산된 epc 코드의 수)
	private Long codeCount; // ex) 900000

	// 필터 조건 내에서 발견된 총 이상 징후 발생 건수n
	private Long anomalyCount; // ex) 125

	// 이상 발생 비율 (anomalyCount / (totalTripCount*5))
	// 프론트에서 100 곱해 %로 표시
	private double anomalyRate; // ex) 0.0146

	// 전체 입고량 대비 실제 판매(pos_Sell)된 상품의 비율(%)
	// pos_Sell && epc_code 카디널리티 / factory && epc_code 카디널리티
	private double salesRate; // ex) 92.5

	// 생산량 대비 실제 출고된 상품의 비율(%)
	// 창고_out && epc_code 카디널리티 / 창고_in && epc_code 카디널리티
	private double dispatchRate; // ex) 95.1

	// 전체 보관 가능 용량 대비 현재 보관 중인 재고의 비율(%)
	// 각 단계 out / 각 단계 in
	private double inventoryRate; // ex) 78.2

	// 상품이 생산 시작부터 최종 판매까지 걸리는 평균 소요 시간 (day 단위)
	private double avgLeadTime; // ex) 12.5

	// N:1 productId와 연결됨
	@ManyToOne(fetch = FetchType.LAZY) // FK
	// @JoinColumn(name = 현재 테이블에 생길 새로운 칼럼명, referenced = 참조할 테이블의 PK)
	@JoinColumn(name = "file_id") // MySQL 칼럼 기준
	// EventHistory 테이블에 생길 새로운 칼럼명, 참조할 테이블의 PK
	private Csv csv;
}
