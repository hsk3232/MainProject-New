package edu.pnu.dto.dataShare;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder

public class ImportDatafromAiDTO {
    private Long fileId;
    @JsonProperty("eventHistory") 
    private List<ImportAiDataDTO> eventHistory;


}
