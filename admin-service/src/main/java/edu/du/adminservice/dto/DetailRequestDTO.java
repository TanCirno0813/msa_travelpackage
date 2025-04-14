package edu.du.adminservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetailRequestDTO {
    private Long id;
    private String description;    // 상세 설명
    private String imageUrl;      // 이미지들 (콤마 구분 문자열 등)
    private String date;           // 일정 요약
}
