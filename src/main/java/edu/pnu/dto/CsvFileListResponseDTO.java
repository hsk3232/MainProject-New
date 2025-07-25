package edu.pnu.dto;

import java.time.LocalDateTime;

import edu.pnu.domain.Csv;
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
public class CsvFileListResponseDTO {
	private Long fileId;
	private String fileName;
	private String userId;
	private Long fileSize;
	private LocalDateTime createdAt;
	
	public static CsvFileListResponseDTO fromEntity(Csv c) {
		return CsvFileListResponseDTO.builder()
				.fileId(c.getFileId())
				.fileName(c.getFileName())
				.userId(c.getMember().getUserId())
				.fileSize(c.getFileSize())
				.createdAt(c.getCreatedAt())
				.build();
		
	}
}
