package edu.pnu.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
public class Product {
	@Id // 고유한 값임으로 AI 필요 없음
	private String epcProduct; // 상품코드
	private String epcCompany; // 제조사 코드
	private String productName; // 상품명
	

}
