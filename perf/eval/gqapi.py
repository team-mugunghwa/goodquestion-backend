"""평가 스크립트 공용 - 로그인/HTTP/멀티파트. 표준 라이브러리만 쓴다.

로그인 응답이 버전에 따라 {accessToken} 또는 {tokens:{accessToken}} 이라
둘 다 받는다. 실패는 예외로 올린다 - 평가는 인증이 안 되면 의미가 없다.
"""

import json
import time
import urllib.error
import urllib.request
import uuid

DEMO_EMAIL = "demo@goodquestion.kr"
DEMO_PASSWORD = "demo1234!"
DEMO_CHILD_ID = "aaaaaaaa-aaaa-aaaa-aaaa-000000000001"


class Api:
    def __init__(self, base):
        self.base = base.rstrip("/")
        self.token = None

    def login(self, email=DEMO_EMAIL, password=DEMO_PASSWORD):
        status, body, _ = self.request(
            "POST", "/api/auth/login", {"email": email, "password": password}, auth=False)
        if status != 200:
            raise RuntimeError(f"로그인 실패 {status}: {body}")
        data = json.loads(body)
        self.token = (data.get("tokens") or {}).get("accessToken") or data.get("accessToken")
        if not self.token:
            raise RuntimeError(f"토큰을 찾지 못함: {body[:200]}")
        return self.token

    def request(self, method, path, payload=None, auth=True, timeout=60):
        """반환: (status, body_text, latency_ms)"""
        headers = {"Content-Type": "application/json"}
        if auth and self.token:
            headers["Authorization"] = f"Bearer {self.token}"
        data = json.dumps(payload).encode() if payload is not None else None
        req = urllib.request.Request(
            self.base + path, data=data, headers=headers, method=method)
        t0 = time.perf_counter()
        try:
            with urllib.request.urlopen(req, timeout=timeout) as res:
                return res.status, res.read().decode(), (time.perf_counter() - t0) * 1000
        except urllib.error.HTTPError as e:
            return e.code, e.read().decode(), (time.perf_counter() - t0) * 1000

    def post_multipart(self, path, field, filename, content, content_type, timeout=120):
        """멀티파트 업로드 (STT). 반환: (status, body_text, latency_ms)"""
        boundary = uuid.uuid4().hex
        body = (
            f"--{boundary}\r\n"
            f'Content-Disposition: form-data; name="{field}"; filename="{filename}"\r\n'
            f"Content-Type: {content_type}\r\n\r\n"
        ).encode() + content + f"\r\n--{boundary}--\r\n".encode()
        headers = {
            "Content-Type": f"multipart/form-data; boundary={boundary}",
            "Authorization": f"Bearer {self.token}",
        }
        req = urllib.request.Request(self.base + path, data=body, headers=headers, method="POST")
        t0 = time.perf_counter()
        try:
            with urllib.request.urlopen(req, timeout=timeout) as res:
                return res.status, res.read().decode(), (time.perf_counter() - t0) * 1000
        except urllib.error.HTTPError as e:
            return e.code, e.read().decode(), (time.perf_counter() - t0) * 1000


def percentile(values, p):
    """단순 최근접 순위 백분위. 표본이 작으므로 보간하지 않는다."""
    if not values:
        return None
    ordered = sorted(values)
    index = min(len(ordered) - 1, max(0, round(p / 100 * len(ordered)) - 1))
    return round(ordered[index], 1)
