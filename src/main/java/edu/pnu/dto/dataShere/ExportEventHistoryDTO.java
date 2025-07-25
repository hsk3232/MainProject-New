package edu.pnu.dto.dataShere;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import edu.pnu.domain.EventHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ExportEventHistoryDTO {
    private Long eventId;
    private Long locationId;
    private String businessStep;
    private String eventType;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime eventTime;
//    private String epcProduct;
//    private String productName;
    private Long fileId;

    public static ExportEventHistoryDTO fromEntity(EventHistory h) {
        return ExportEventHistoryDTO.builder()
            .eventId(h.getEventId())
            .locationId(h.getLocation().getLocationId())
            .businessStep(h.getBusinessStep())
            .eventType(h.getEventType())
            .eventTime(h.getEventTime())
//            .epcProduct(h.getEpc().getProduct().getEpcProduct())
//            .productName(h.getEpc().getProduct().getProductName())
            .fileId(h.getCsv().getFileId())
            .build();
    }
}