package edu.pnu.dto.dataShere;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

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
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ExportDataToAiDTO {
	private String epcCode;
    private List<ExportEventHistoryDTO> events;

    public static ExportDataToAiDTO from(String epcCode, List<EventHistory> events) {
        return ExportDataToAiDTO.builder()
            .epcCode(epcCode)
            .events(events.stream().map(ExportEventHistoryDTO::fromEntity).toList())
            .build();
    }
}
