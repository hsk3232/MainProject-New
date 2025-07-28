package edu.pnu.dto;

import lombok.Value;

@Value // Getter, AllArgsConstructor, EqualsAndHashCode, ToString을 한번에 생성
public class Triples {
    String epcProduct;
    String epcCompany;
    String productName;
}
