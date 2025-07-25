package edu.pnu.dto.dataShere;

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
@Builder
@ToString

public class ImportAiDataDTO {
	
	// 각 이벤트별 상세
    private Long eventId;
}