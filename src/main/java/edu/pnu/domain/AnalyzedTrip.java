package edu.pnu.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
@Table(name="analyzedtrip")
public class AnalyzedTrip {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roadId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="epc_code")
    private Epc epc;
    
    private String fromScanLocation;
    private String toScanLocation;
    
    private Long fromLocationId;
    private Long toLocationId;
    
    private String fromBusinessStep;
    private String toBusinessStep;
    private String fromEventType;
    private String toEventType;
    private String fromHubType;
    private String toHubType;
    
    private LocalDateTime fromEventTime;
    private LocalDateTime toEventTime;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private Csv csv;
    
}