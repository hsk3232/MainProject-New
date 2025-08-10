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

import edu.pnu.domain.Csv;
import edu.pnu.dto.CsvFileListResponseDTO;
import edu.pnu.exception.CsvFileNotFoundException;
import edu.pnu.exception.CsvFilePathNotFoundException;
import edu.pnu.exception.UnauthorizedException;
import edu.pnu.repo.CsvRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvLogService {
	
	private final CsvRepository csvRepo;

	//DownLoad 위한 메서드
    public Resource loadCsvResource(Long fileId) {
    	log.info("[진입] : [CsvLogService] ");
    	
    	 Csv csv = csvRepo.findById(fileId)
    			 .orElseThrow(() -> new CsvFileNotFoundException("[오류] : [CsvLogService] 조회된 파일이 없음 (id=" + fileId + ")"));

         try {
        	 Path filePath = Paths.get(csv.getFilePath(), csv.getSavedFileName());
             Resource resource = new UrlResource(filePath.toUri());
             if (resource.exists() && resource.isReadable()) {
                 return resource;
             } else {
                 throw new CsvFilePathNotFoundException("[오류] : [CsvLogService] 파일을 읽을 수 없음 (id= " + fileId + ")");
             }
         } catch (MalformedURLException e) {
        	 throw new CsvFilePathNotFoundException(
        			    "[오류] : [CsvLogService] 잘못된 파일 경로 (filePath= " + csv.getFilePath() + ")");
         }
        
    }
    
    
    // 업로드된 file 목록 조회, 커서 페이징 사용
    public List<CsvFileListResponseDTO> getFileListByCursor(Long cursor, int size, String search, Long locationId) {
        Pageable pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "fileId"));
        List<Csv> csvList;

        if (search != null && !search.isBlank()) {
            if (cursor == null) {
                csvList = csvRepo.findByMember_Location_LocationIdAndFileNameContainingOrderByFileIdDesc(
                    locationId, search, pageable);
            } else {
                csvList = csvRepo.findByMember_Location_LocationIdAndFileIdLessThanAndFileNameContainingOrderByFileIdDesc(
                    locationId, cursor, search, pageable);
            }
        } else {
            if (cursor == null) {
                csvList = csvRepo.findByMember_Location_LocationIdOrderByFileIdDesc(locationId, pageable);
            } else {
                csvList = csvRepo.findByMember_Location_LocationIdAndFileIdLessThanOrderByFileIdDesc(
                    locationId, cursor, pageable);
            }
        }

        if (csvList.isEmpty()) {
            throw new CsvFileNotFoundException("[오류] : [CsvLogService] 조회된 파일이 없음 (검색어= " + search + ")");
        }

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
