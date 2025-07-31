package edu.pnu.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportCoverDTO {
    private String fileName;
    private String userName;
    private Long locationId;
    private LocalDateTime createdAt;
    private LocalDateTime periodStart;  // 시작시간
    private LocalDateTime periodEnd;    // 종료시간

    
    public List<LocalDateTime> getPeriod() {
        return List.of(periodStart, periodEnd);
    }
}
