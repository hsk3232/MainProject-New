package edu.pnu.util;

//final class: 상속을 허용하지 않는 유틸리티 클래스
public final class StringUtils {

 // private 생성자: 이 클래스가 인스턴스화되는 것을 방지합니다.
 private StringUtils() {}

 /**
  * 입력된 문자열을 정규화합니다. (null -> "", 양쪽 공백 제거, 소문자 변환)
  * @param input 원본 문자열
  * @return 정규화된 문자열
  */
 public static String normalize(String input) {
     return input == null ? "" : input.trim().toLowerCase();
 }
}