package edu.pnu.service.member;

import org.springframework.stereotype.Service;

import edu.pnu.Repo.MemberRepository;
import edu.pnu.domain.Role;
import edu.pnu.exception.NotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginService {
	private final MemberRepository memberRepo;

	public Role getUnauthPage(String userId) {
		// 권한(Role) 정보를 DB에서 조회
        Role role = memberRepo.findByRole(userId);

        // 결과가 없으면 커스텀 예외 발생 (→ 전역 핸들러가 404로 처리)
        if (role == null) {
            throw new NotFoundException("[오류] : [LoginService] 권한 정보를 찾을 수 없습니다: " + userId);
        }

        return role;
    }
}
