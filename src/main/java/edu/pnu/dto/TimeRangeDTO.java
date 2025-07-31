package edu.pnu.dto;

import java.time.LocalDateTime;



public record TimeRangeDTO(LocalDateTime minTime, LocalDateTime maxTime) {

}
