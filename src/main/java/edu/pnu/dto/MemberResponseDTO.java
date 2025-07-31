package edu.pnu.dto;

import edu.pnu.domain.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberResponseDTO {
	private String userName;
	private String userId;
	private String status;
	private String email;
 // 비밀번호 등 민감 정보는 포함하지 않음
	
	
 public static MemberResponseDTO fromEntity(Member member) {
	    return MemberResponseDTO.builder()
	    	.userName(member.getUserName())
	        .userId(member.getUserId())
	        .status(member.getStatus())
	        .email(member.getEmail())
	        .build();
	}
}
