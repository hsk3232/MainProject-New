package edu.pnu.service.statistics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EpcSerialValidatorService {
	
	public static class SerialRange {
		int start;
		int end;

		public SerialRange(int start, int end) {
			this.start = start;
			this.end = end;
		}

		public boolean contains(int value) {
			return value >= start && value <= end;
		}
	}

	private final Map<String, Map<String, SerialRange>> factoryLotSerialRanges = new HashMap<>();

    public EpcSerialValidatorService() {
        factoryLotSerialRanges.put("화성", initFactoryLotsWithResets(50001, 26, 2000, 16));
        factoryLotSerialRanges.put("인천", initFactoryLotsWithResets(10001, 51, 2000, 16));
        factoryLotSerialRanges.put("구미", initFactoryLotsWithResets(150001, 11, 2000, 16));
        factoryLotSerialRanges.put("양산", initFactoryLotsWithResets(100001, 32, 2000, 16));
    }

    private Map<String, SerialRange> initFactoryLotsWithResets(int sl, int bs, int cpl, int ri) {
        Map<String, SerialRange> lm = new LinkedHashMap<>();
        int ss = 1;
        for (int i = 0; i < bs; i++) {
            int lot = sl + i;
            int se;
            if (i % ri == 0) { se = ss; } 
            else if ((i + 1) % ri == 0) { se = ss + 1998; } 
            else { se = ss + cpl - 1; }
            lm.put(String.valueOf(lot), new SerialRange(ss, se));
            if ((i + 1) % ri == 0) { ss = 1; } 
            else { ss = se + 1; }
        }
        return lm;
    }
    
    // 'tamper' 판별 시 사용될, 과거에 사용된 적 있는 모든 Lot 번호 목록
    public Set<String> getAllKnownLots() {
        Set<String> lots = new HashSet<>();
        factoryLotSerialRanges.values().forEach(lotMap -> lots.addAll(lotMap.keySet()));
        return lots;
    }

    // [성능 개선] 메모리 소모 없이 시리얼 번호가 유효한 범위에 있는지 '가능성'만 검사
    public boolean isPotentiallyValidSerial(int serialNumber) {
        if (serialNumber <= 0) return false;
        for (Map<String, SerialRange> lotMap : factoryLotSerialRanges.values()) {
            for (SerialRange range : lotMap.values()) {
                if (range.contains(serialNumber)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    // 'error'/'tamper' 판별 시, 정확한 공장과 lot을 지정하여 유효성을 검사
    public boolean isValid(String factory, String lot, int serialNumber) {
        if (factory == null || lot == null) return false;
        Map<String, SerialRange> lotRanges = factoryLotSerialRanges.get(factory);
        if (lotRanges == null) {
            log.warn("시리얼 검증 실패: 정의되지 않은 factory입니다. factory={}", factory);
            return false;
        }
        SerialRange range = lotRanges.get(lot);
        return range != null && range.contains(serialNumber);
    }
    
    // 최초 생산 공장을 찾기 위한 헬퍼 메서드
    public String extractFactoryFromName(String hubType) {
        if (hubType == null) return null;
        if (hubType.contains("HWS") || hubType.contains("화성")) return "화성";
        if (hubType.contains("ICN") || hubType.contains("인천")) return "인천";
        if (hubType.contains("GUM") || hubType.contains("구미")) return "구미";
        if (hubType.contains("YAS") || hubType.contains("양산")) return "양산";
        return null;
    }
}
