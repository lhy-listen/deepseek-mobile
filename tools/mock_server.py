"""Tiny OpenAI-compatible mock server used to verify the app end to end.

Run:  python tools/mock_server.py
Then point the app's base URL at http://10.0.2.2:8765 (emulator) or
http://<your-lan-ip>:8765 (real device).
"""
from __future__ import annotations

import json
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, fmt, *args):  # quieter logs
        print("[mock]", fmt % args)

    def _json(self, status: int, payload: dict) -> None:
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if self.path.rstrip("/").endswith("/models"):
            self._json(200, {
                "object": "list",
                "data": [
                    {"id": "deepseek-chat", "object": "model"},
                    {"id": "deepseek-reasoner", "object": "model"},
                ],
            })
            return
        self._json(404, {"error": {"message": "not found"}})

    def do_POST(self):
        length = int(self.headers.get("content-length") or 0)
        raw = self.rfile.read(length) if length else b"{}"
        try:
            payload = json.loads(raw or b"{}")
        except Exception:
            payload = {}

        model = str(payload.get("model", ""))
        messages = payload.get("messages", [])
        has_image = any(
            isinstance(m.get("content"), list)
            and any(part.get("type") == "image_url" for part in m["content"])
            for m in messages if isinstance(m, dict)
        )
        last_text = ""
        for m in messages:
            if m.get("role") == "user":
                content = m.get("content")
                last_text = content if isinstance(content, str) else " ".join(
                    p.get("text", "") for p in content if isinstance(p, dict)
                )

        if has_image and not payload.get("stream"):
            self._json(200, {
                "id": "mock-vision",
                "object": "chat.completion",
                "choices": [{
                    "index": 0,
                    "message": {
                        "role": "assistant",
                        "content": "（模拟视觉描述）图中是一台 OPPO Find X8s 的「设备详情」页面，"
                                   "型号 PKT110，Android 16，右上角时间 00:22、电量 47%。",
                    },
                    "finish_reason": "stop",
                }],
            })
            return

        if not payload.get("stream"):
            self._json(200, {
                "id": "mock",
                "object": "chat.completion",
                "choices": [{
                    "index": 0,
                    "message": {"role": "assistant", "content": f"模拟回复：收到「{last_text[:40]}」"},
                    "finish_reason": "stop",
                }],
                "usage": {"prompt_tokens": 20, "completion_tokens": 10, "total_tokens": 30},
            })
            return

        self.send_response(200)
        self.send_header("Content-Type", "text/event-stream; charset=utf-8")
        self.send_header("Cache-Control", "no-cache")
        self.send_header("Connection", "keep-alive")
        self.end_headers()

        def send(data: dict) -> None:
            self.wfile.write(("data: " + json.dumps(data, ensure_ascii=False) + "\n\n").encode("utf-8"))
            self.wfile.flush()

        if "reasoner" in model:
            send({"choices": [{"index": 0, "delta": {"reasoning_content": "先确认用户的问题，再组织回答。"}, "finish_reason": None}]})
            time.sleep(0.2)

        words = ["这是", "一条", "来自", "本地", " mock ", "服务", "的流式", "回复。\n\n", "```kotlin\n", "fun main() {\n", "    println(\"hi\")\n", "}\n", "```\n"]
        for word in words:
            send({"choices": [{"index": 0, "delta": {"content": word}, "finish_reason": None}]})
            time.sleep(0.08)

        send({
            "choices": [{"index": 0, "delta": {}, "finish_reason": "stop"}],
            "usage": {"prompt_tokens": 42, "completion_tokens": 24, "total_tokens": 66},
        })
        self.wfile.write(b"data: [DONE]\n\n")
        self.wfile.flush()


if __name__ == "__main__":
    server = ThreadingHTTPServer(("0.0.0.0", 8765), Handler)
    print("mock server on http://0.0.0.0:8765")
    server.serve_forever()
