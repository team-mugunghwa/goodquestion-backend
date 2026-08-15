package com.mugunghwa.goodquestion.ai.stt;

import org.springframework.web.multipart.MultipartFile;

/** STT 외부 API 추상화. 벤더 교체를 위해 인터페이스로 분리. */
public interface SttClient {

    /** @return 변환 텍스트와 신뢰도. 신뢰도를 못 주는 벤더는 confidence를 null로 둔다 */
    SttResult transcribe(MultipartFile audio);
}
