package edu.pnu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter 
@Setter 
@ToString 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class UserStatusUpdateDTO {
    private String userId;
    private String status;  // "active", "rejected", "inactive", "del"
}