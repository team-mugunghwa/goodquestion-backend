package com.mugunghwa.goodquestion.global.security;

import java.lang.annotation.*;

/** 컨트롤러 파라미터에 인증된 보호자 ID(UUID)를 주입한다. */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentParentId {
}
