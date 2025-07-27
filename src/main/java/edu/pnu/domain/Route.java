package edu.pnu.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Builder
public class Route {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roadId;
    private String fromScanLocation;
    private String toScanLocation;
    private Long fromLocationId;
    private Long toLocationId;
    private String fromBusinessStep;
    private String toBusinessStep;
    private String fromEventType;
    private String toEventType;
}
