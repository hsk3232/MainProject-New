package edu.pnu.events;

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
public class EventHistorySavedEvent {
    private Long fileId;
}
