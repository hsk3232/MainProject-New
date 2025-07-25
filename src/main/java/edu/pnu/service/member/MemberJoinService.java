package edu.pnu.service.member;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import edu.pnu.Repo.MemberRepository;
import edu.pnu.domain.Member;
import edu.pnu.dto.MemberJoinDTO;
import edu.pnu.exception.BadRequestException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberJoinService {
	private final MemberRepository memberRepo;
	private final PasswordEncoder passwordEncoder;
	
	// 1. 회원가입
	public void postJoin(MemberJoinDTO dto) {
		
		// Global Exception : 아이디 중복 체크 1번 더 
        if (memberRepo.existsByUserId(dto.getUserId())) {
            throw new BadRequestException("[오류] : [MemberJoinController] 동일 id 검색됨" + dto.getUserId());
        }
		
        // 회원가입 정보 저장 및 암호 해시
		Member m = dto.toEntity();
		m.setPassword(passwordEncoder.encode(dto.getPassword()));
		
		memberRepo.save(m);
	}

	
	// 2. id 중복 여부 확인 -> api 따로 있기 때문에 필요함.
	public boolean postIdSearch(String userId){
		boolean exist = memberRepo.existsByUserId(userId);
		System.out.println(exist);
		if(exist) {
			System.out.println("[오류] : [MemberJoinController] 동일 id 검색됨");
		}
		return exist;
	}
	
}
