package edu.pnu.controller;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.pnu.domain.Role;
import edu.pnu.dto.LoginDTO;
import edu.pnu.dto.MemberJoinDTO;
import edu.pnu.service.member.LoginService;
import edu.pnu.service.member.MemberJoinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public")
public class MemberJoinController {

	private final MemberJoinService memberJoinsvc;
	
	@PostMapping("/join")
	public void postJoin(@RequestBody MemberJoinDTO dto) {
		try {
			log.info("[진입] : [MemberJoinController] 회원가입 진입");
		
		memberJoinsvc.postJoin(dto);
		log.info("[성공] : [MemberJoinController] 회원가입 성공");
		}
		catch(Exception e) {
			log.info("[실패] : [MemberJoinController] 회원가입 실패 " + e.getMessage());
			
		}
	}
	
	@PostMapping("/join/idsearch")
	public boolean postIdSearch(@RequestBody LoginDTO dto) {
		boolean result = memberJoinsvc.postIdSearch(dto.getUserId());
		if(result) {
			log.info("[경고/오류] : [JoinController][이미 사용 중인 아이디입니다.]" + "\n");
	        return true;
		}
		log.info("[성공] : [JoinController] 사용할 수 있는 아이디 검색^^ \n");
			return false;
	}
	
	
	private final LoginService loginSvc;
	
	@GetMapping("/unauth")
	public ResponseEntity<?> getUnauthPage(Principal principal) {
		log.info("[진입] : [LoginController] 미승인 가입자 페이지 연결 진입");
		
		 Role r = loginSvc.getUnauthPage(principal.getName());

		    if (r == Role.ROLE_UNAUTH) {
		    	log.info("[성공] : [LoginController] 미승인 가입자");
		        return ResponseEntity.ok().body("미승인 가입자입니다.");
		    } else {
		    	log.info("[차단] : [LoginController] 승인된 사용자가 요청함");
		        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("잘못된 접근입니다.");
		    }
	}
	
}
