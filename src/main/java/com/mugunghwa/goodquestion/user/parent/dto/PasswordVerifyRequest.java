package com.mugunghwa.goodquestion.user.parent.dto;

import jakarta.validation.constraints.NotBlank;

/** 보호자 확인 게이트(F-09) 비밀번호 검증 요청. */
public record PasswordVerifyRequest(@NotBlank String password) {}