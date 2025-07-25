package edu.pnu.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.pnu.dto.NodeDTO;
import edu.pnu.service.NodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/manager")
public class NodeController {
	
	private final NodeService nodeService;
	
	@GetMapping("/node")
	public List<NodeDTO> getNodeList(){
		log.info("[진입] : [NodeController] Node 정보 전달 진입");
		
		List<NodeDTO> dto = nodeService.getNodeList();
		log.info("[성공] : [NodeController] Node 정보 전달 성공");
		return dto;
	}
}
