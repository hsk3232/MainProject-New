package edu.pnu.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import edu.pnu.domain.Location;
import edu.pnu.domain.Member;
import edu.pnu.dto.UserFactoryUpdateDTO;
import edu.pnu.dto.UserResponseDTO;
import edu.pnu.dto.UserStatusUpdateDTO;
import edu.pnu.exception.NoDataFoundException;
import edu.pnu.repo.LocationRepository;
import edu.pnu.repo.MemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {
	private final MemberRepository memberRepo;
	private final LocationRepository locationRepo;


    // 사용자 목록 조회
	@Transactional
    public List<UserResponseDTO> getUserList() {
        List<Member> members = memberRepo.findAll();
        return members.stream()
            .map(this::toUserResponseDTO)
            .collect(Collectors.toList());
    }

    private UserResponseDTO toUserResponseDTO(Member m) {

        return UserResponseDTO.builder()
            .role(m.getRole().name()) // Role enum to String
            .locationId(m.getLocation() != null ? m.getLocation().getLocationId() : null)
            .userId(m.getUserId())
            .userName(m.getUserName())
            .email(m.getEmail())
            .status(m.getStatus()) // String or Enum; 맞춰서 사용하세요.
            .createdAt(m.getCreatedAt())
            .build();
    }

    // 상태 변경
    @Transactional
    public void updateUserStatus(UserStatusUpdateDTO dto) {
    	Member m = memberRepo.findByUserId(dto.getUserId())
    			.orElseThrow(() -> new IllegalArgumentException("[오류] 회원 정보가 없습니다: "));
        if (m == null) {
            throw new NoDataFoundException("사용자를 찾을 수 없습니다: " + dto.getUserId());
        }

        m.setStatus(dto.getStatus());
        memberRepo.save(m);
    }
    
    @Transactional
    public void updateUserFactory(UserFactoryUpdateDTO dto) {
        Member member = memberRepo.findByUserId(dto.getUserId())
                .orElseThrow(() -> new NoDataFoundException("[오류] 회원을 찾을 수 없습니다. userId=" + dto.getUserId()));

        Location location = locationRepo.findById(dto.getLocationId())
                .orElseThrow(() -> new NoDataFoundException("[오류] 위치를 찾을 수 없습니다. locationId=" + dto.getLocationId()));

        member.setLocation(location);
        memberRepo.save(member);
    }
}
