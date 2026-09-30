#!/usr/bin/env python3
"""
Smart Storage Cleaner AI - Application Server
Serves static assets and provides mock/real backend telemetry endpoints.
Binds to 0.0.0.0:8080 for live preview in Arena environment.
"""

from __future__ import annotations

import argparse
import json
import mimetypes
import os
import shutil
import sys
from http import HTTPStatus
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

WEB_DIR = Path(__file__).resolve().parent

# Ensure common web extensions have proper MIME types
mimetypes.add_type("application/javascript", ".js")
mimetypes.add_type("text/css", ".css")
mimetypes.add_type("application/json", ".json")
mimetypes.add_type("image/svg+xml", ".svg")
mimetypes.add_type("image/webp", ".webp")


class SmartCleanHTTPHandler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(WEB_DIR), **kwargs)

    def end_headers(self):
        # Enable CORS and prevent caching for development
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.send_header("Cache-Control", "no-cache, no-store, must-revalidate")
        super().end_headers()

    def do_OPTIONS(self):
        self.send_response(HTTPStatus.NO_CONTENT)
        self.end_headers()

    def do_GET(self):
        # Route API requests
        if self.path.startswith("/api/stats"):
            self.send_json_response(self.get_system_stats())
            return
        elif self.path.startswith("/api/health"):
            self.send_json_response({"status": "healthy", "service": "smart-clean-ai", "version": "3.5.0"})
            return

        # Serve static file fallback to index.html if root
        if self.path in ("", "/"):
            self.path = "/index.html"

        return super().do_GET()

    def do_POST(self):
        if self.path.startswith("/api/clean"):
            content_length = int(self.headers.get("Content-Length", 0))
            body = self.rfile.read(content_length).decode("utf-8") if content_length > 0 else "{}"
            try:
                payload = json.loads(body)
            except Exception:
                payload = {}

            # Return successful simulated cleanup response
            response = {
                "success": True,
                "freedBytes": 15247192064,  # ~14.2 GB
                "freedFormatted": "14.2 GB",
                "categoriesCleaned": payload.get("categories", ["cache", "residual", "duplicates", "logs"]),
                "message": "تم تنظيف وتفريغ الذاكرة بنجاح"
            }
            self.send_json_response(response)
            return

        self.send_error(HTTPStatus.NOT_FOUND, "API route not found")

    def send_json_response(self, data: dict, status: int = 200):
        body = json.dumps(data, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def get_system_stats(self) -> dict:
        try:
            total, used, free = shutil.disk_usage("/")
            total_gb = round(total / (1024**3), 1)
            used_gb = round(used / (1024**3), 1)
            free_gb = round(free / (1024**3), 1)
        except Exception:
            total_gb, used_gb, free_gb = 128.0, 104.2, 23.8

        return {
            "storage": {
                "totalGB": total_gb,
                "usedGB": used_gb,
                "freeGB": free_gb,
                "usedPercent": round((used_gb / total_gb) * 100, 1) if total_gb > 0 else 81.0,
            },
            "ram": {
                "totalGB": 8.0,
                "usedGB": 5.9,
                "percent": 74
            },
            "battery": {
                "level": 84,
                "tempC": 36.5,
                "isCharging": False
            },
            "appVersion": "3.5.0-pro"
        }


def run_server(port: int = 8080):
    server_address = ("0.0.0.0", port)
    httpd = ThreadingHTTPServer(server_address, SmartCleanHTTPHandler)
    print(f"🚀 Smart Clean AI Server running at http://0.0.0.0:{port}/")
    print(f"📁 Serving files from: {WEB_DIR}")
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nStopping server...")
        httpd.server_close()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Smart Clean AI Preview Server")
    parser.add_argument("--port", type=int, default=8080, help="Port to bind (default: 8080)")
    args = parser.parse_args()
    run_server(args.port)
