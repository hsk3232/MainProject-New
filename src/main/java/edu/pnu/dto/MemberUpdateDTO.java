package edu.pnu.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberUpdateDTO {
    private String userName;  // optional
    private String email;     // optional
    private String status;   // optional (Boolean to allow null)
}