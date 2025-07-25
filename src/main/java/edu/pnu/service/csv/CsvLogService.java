package edu.pnu.service.csv;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import edu.pnu.Repo.CsvRepository;
import edu.pnu.domain.Csv;
import edu.pnu.dto.CsvFileListResponseDTO;
import edu.pnu.exception.CsvFileNotFoundException;
import edu.pnu.exception.CsvFilePathNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CsvLogService {
	
	private final CsvRepository csvRepo;

	
    public Resource loadCsvResource(Long fileLogId) {
    	 Csv csv = csvRepo.findById(fileLogId)
    			 .orElseThrow(() -> new CsvFileNotFoundException("[오류] : [CsvLogService] 조회된 파일이 없음 (id=" + fileLogId + ")"));

         try {
             Path filePath = Paths.get(csv.getFilePath());
             Resource resource = new UrlResource(filePath.toUri());
             if (resource.exists() && resource.isReadable()) {
                 return resource;
             } else {
                 throw new CsvFilePathNotFoundException("[오류] : [CsvLogService] 파일을 읽을 수 없음 (id= " + fileLogId + ")");
             }
         } catch (MalformedURLException e) {
        	 throw new CsvFilePathNotFoundException(
        			    "[오류] : [CsvLogService] 잘못된 파일 경로 (filePath= " + csv.getFilePath() + ")");
         }
    }
    
    
    // 업로드된 file 목록 조회, 커서 페이징 사용
    public List<CsvFileListResponseDTO> getFileListByCursor(Long cursor, int size, String search) {
        Pageable pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "fileId"));
        List<Csv> csvList;

        if (search != null && !search.isBlank()) {
            if (cursor == null) {
            	
                csvList = csvRepo.findByFileNameContainingOrderByFileIdDesc(search, pageable);
            } else {
                csvList = csvRepo.findByFileIdLessThanAndFileNameContainingOrderByFileIdDesc(cursor, search, pageable);
            }
        } else {
            if (cursor == null) {
                csvList = csvRepo.findAllByOrderByFileIdDesc(pageable);
            } else {
                csvList = csvRepo.findByFileIdLessThanOrderByFileIdDesc(cursor, pageable);
            }
        }

        if (csvList.isEmpty()) {
            throw new CsvFileNotFoundException("[오류] : [CsvLogService] 조회된 파일이 없음 (검색어= " + search + ")");
        }

        // DTO 변환
        return csvList.stream()
                      .map(CsvFileListResponseDTO::fromEntity)
                      .toList();
    }
    
    
    public String getFileName(Long fileLogId) {
    	Optional<Csv> csvOpt = csvRepo.findById(fileLogId);
    	if (!csvOpt.isPresent()) {
            throw new CsvFileNotFoundException("[오류] : [CsvLogService] 조회된 파일이 없음 (id= " + fileLogId + ")");
        }
        // 실제 엔티티의 fileName 필드 반환
        return csvOpt.get().getFileName();
    }
}
