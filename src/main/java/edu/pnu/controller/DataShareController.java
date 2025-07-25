package edu.pnu.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.pnu.service.datashare.DataShareService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager")
public class DataShareController {
    private final DataShareService dataShareService;

    // 비동기 분석 트리거
    @PostMapping("export-and-analyze-async")
    public String exportAndAnalyzeAsync() {
    	log.info("[진입] : [DataShareController] 비동기 import 진입");

        dataShareService.autoSendLatestFile();  // 바로 리턴!
        log.info("[성공] : [DataShareController] 데이터 전달 성공");
        return "비동기로 분석 요청을 보냄";
    }
	

}
