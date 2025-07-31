package edu.pnu.dto;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

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
public class AnalyzedTripDTO {
    private TripPoint from;
    private TripPoint to;

    private Long fileId;
    private String epcCode;
    private String productName;
    private String epcLot;
    private String eventType;
    private Long roadId;

    private List<String> anomalyTypeList;

    private LocalDateTime eventTime; // <-- 여기에 추가!

    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TripPoint {
        private String scanLocation;
        private List<Double> coord;
        private Long eventTime;
        private String businessStep;
    }

    // JPA Constructor Expression용 생성자
    public AnalyzedTripDTO(
        String fromScanLocation, Double fromLongitude, Double fromLatitude, LocalDateTime fromEventTime, String fromBusinessStep,
        String toScanLocation, Double toLongitude, Double toLatitude, LocalDateTime toEventTime, String toBusinessStep,
        Long fileId, String epcCode, String productName, String epcLot, String eventType, Long roadId,
        LocalDateTime eventTime) {
        this.from = TripPoint.builder()
            .scanLocation(fromScanLocation)
            .coord(fromLongitude != null && fromLatitude != null ? List.of(fromLongitude, fromLatitude) : null)
            .eventTime(fromEventTime != null ? fromEventTime.toEpochSecond(ZoneOffset.UTC) : null)
            .businessStep(fromBusinessStep)
            .build();

        this.to = TripPoint.builder()
            .scanLocation(toScanLocation)
            .coord(toLongitude != null && toLatitude != null ? List.of(toLongitude, toLatitude) : null)
            .eventTime(toEventTime != null ? toEventTime.toEpochSecond(ZoneOffset.UTC) : null)
            .businessStep(toBusinessStep)
            .build();

        this.fileId = fileId;
        this.epcCode = epcCode;
        this.productName = productName;
        this.epcLot = epcLot;
        this.eventType = eventType;
        this.roadId = roadId;
        this.anomalyTypeList = new ArrayList<>();
        this.eventTime = eventTime; // 이제 오류 없음!
    }
}