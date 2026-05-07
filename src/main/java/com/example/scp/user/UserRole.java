package com.example.scp.user;

import lombok.Getter;

@Getter
public enum UserRole {
    ADMIN("ROLE_ADMIN"),
    USER("ROLE_USER");

    UserRole(String value){
        this.value=value;
    }

    private String value;
}

//현재는 권한으로 특정 기능을 제어하지 않지만 
//추후 ADMIN 권한(관리자 권한)을 지닌 사용자가 다른 사람이 작성한 질문이나 답변을 수정 가능하도록 만들기!