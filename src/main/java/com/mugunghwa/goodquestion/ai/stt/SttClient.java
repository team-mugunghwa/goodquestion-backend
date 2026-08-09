package com.mugunghwa.goodquestion.ai.stt;

import org.springframework.web.multipart.MultipartFile;

/** STT 외부 API 추상화. 벤더 교체를 위해 인터페이스로 분리. */
public interface SttClient {

    /** @return 변환된 텍스트 (실패·무음이면 null 또는 빈 문자열) */
    String transcribe(MultipartFile audio);
}
