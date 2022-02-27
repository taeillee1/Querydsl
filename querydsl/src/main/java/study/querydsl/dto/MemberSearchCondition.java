package study.querydsl.dto;

import lombok.Data;

@Data
public class MemberSearchCondition {
    //회원명, 팀명, 나이(ageGoe, ageLoe) 조건으로 검색하게하기위한것

    private String username;
    private String teamName;
    private Integer ageGoe;
    private Integer ageLoe;
}
