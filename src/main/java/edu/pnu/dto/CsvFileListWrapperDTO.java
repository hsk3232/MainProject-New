package edu.pnu.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class CsvFileListWrapperDTO {
    private List<CsvFileListResponseDTO> data;
    private Long nextCursor;
}