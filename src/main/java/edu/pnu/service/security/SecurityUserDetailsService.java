package edu.pnu.service.security;

import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import edu.pnu.Repo.MemberRepository;
import edu.pnu.config.CustomUserDetails;
import edu.pnu.domain.Member;
import lombok.RequiredArgsConstructor;

// --------------(DB에서 유저 찾기)----------//

@Service
@RequiredArgsConstructor
public class SecurityUserDetailsService  implements UserDetailsService {
	
	private final MemberRepository memberRepo;

	@Override
	public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
		System.out.println("\n[진입] : [1][SecurityUserDetailsService] 사용자 검색 진입");
		
		// [1] DB에서 사용자 정보(Member 엔티티) 조회
		Member member = memberRepo.findByUserId(userId)
				.orElseThrow(() -> new UsernameNotFoundException("[오류] : [SecurityUserDetailsService] 사용자 " + userId + " 못찾겠음 \n"));
		// UsernameNotFoundException 이 오류는 Spring Security 표준 예외 제공되는 애를 써도 무방함.
		
		System.out.println("[성공] : [2][SecurityUserDetailsService] 사용자 검색 성공\n");
		
		// [2] CustomUserDetails에 Member 엔티티 정보를 전달
		System.out.println("[전달] : [3][SecurityUserDetailsService]  CustomUserDetails에 Member 엔티티 정보를 전달\n");
        
	
		
		return CustomUserDetails.builder()
                .userId(member.getUserId())
                .password(member.getPassword())
                .authorities(AuthorityUtils.createAuthorityList(member.getRole().toString()))
                .locationId(member.getLocation().getLocationId())
                .build();
				
	}
}
