package com.mugunghwa.goodquestion.global.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 요청한 클라이언트의 IP를 구한다.
 *
 * <p>Railway 같은 PaaS는 프록시 뒤에 있어 getRemoteAddr()가 프록시 주소를 준다.
 * X-Forwarded-For의 첫 항목이 원래 클라이언트다.
 */
public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.isBlank()) {
            return request.getRemoteAddr();
        }
        return forwarded.split(",")[0].trim();
    }
}