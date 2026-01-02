# Connect Flutter to the Desktop App — Prompt & How-to

This file is a copy-ready prompt you can paste into your Flutter project documentation or use directly in the Flutter app to connect to the Java desktop server in this repository.

Goal
- Show the desktop screen in the Flutter app (live) and send mouse/keyboard input from the Flutter app to the desktop over the same LAN.

Checklist (quick)
- [ ] Desktop app running (Gradle run or JAR) and TCP server listening on port 5000.
- [ ] Desktop firewall allows inbound TCP 5000 and the chosen WebSocket port (e.g. 8080).
- [ ] Flutter device and desktop are on the same LAN and you have the desktop LAN IP.
- [ ] Use TCP control JSON to start WebSocket streaming, then open WebSocket to receive JPEG frames.
- [ ] Send keyboard and motion JSONs over the same TCP connection.

1) Network / prerequisites
- Ensure the mobile device and the desktop are on the same Wi-Fi or LAN. Use the desktop's LAN IP (e.g. 192.168.1.42).
- Open firewall/allow apps for:
  - TCP 5000 (control + motion + keyboard)
  - TCP <WS_PORT> (e.g. 8080) for WebSocket image streaming

2) Control JSONs (send over the TCP control socket to desktop:5000)
- Start WebSocket streaming server on the desktop (desktop will start a WS server and wait for clients):
  {"websocket":{"cmd":"start","port":8080,"fps":12,"maxWidth":1280,"quality":0.7}}

- Stop WebSocket server:
  {"websocket":{"cmd":"stop"}}

- Start UDP streaming (if you prefer UDP):
  {"stream":{"cmd":"start","port":6000,"fps":12,"maxWidth":1280,"quality":0.7}}

- Keyboard actions (the server already supports these over TCP):
  - Type text:
    {"keyboard":{"cmd":"type","text":"Hello from Flutter"}}
  - Tap a key (KeyEvent VK_* numeric code, e.g., 65 = 'A'):
    {"keyboard":{"cmd":"tap","keyCode":65}}
  - Press and release:
    {"keyboard":{"cmd":"press","keyCode":17}}
    {"keyboard":{"cmd":"release","keyCode":17}}

- Motion / mouse from Flutter (existing protocol):
  {"gyroX":0.12,"gyroY":-0.04,"leftClick":false,"rightClick":false}

3) Flutter: connect TCP socket and send control JSON
- Use `dart:io` Socket to open a TCP connection to desktop:5000 and send newline-delimited JSON lines.

Example TCP control helper (Dart):

import 'dart:io';
import 'dart:convert';

Future<Socket> connectControl(String host, int port) async {
  final socket = await Socket.connect(host, port);
  socket.setOption(SocketOption.tcpNoDelay, true);
  return socket;
}

Future<void> sendControl(Socket socket, Map<String, dynamic> json) async {
  socket.add(utf8.encode(jsonEncode(json) + '\n'));
  await socket.flush();
}

Usage example:

final sock = await connectControl('192.168.1.42', 5000);
await sendControl(sock, {"websocket": {"cmd": "start", "port": 8080, "fps": 12}});

4) Flutter: connect to WebSocket and display JPEG frames
- After you start the WS server via the TCP control message, open a WebSocket to ws://<DESKTOP_IP>:<WS_PORT>.
- The WebSocket server sends each frame as a binary message containing a full JPEG image. Show each binary as an Image.memory widget.

Minimal Flutter widget to receive and display frames:

import 'dart:io';
import 'dart:typed_data';
import 'package:flutter/material.dart';

class DesktopViewer extends StatefulWidget {
  final String wsUrl; // e.g. 'ws://192.168.1.42:8080'
  DesktopViewer({required this.wsUrl});
  @override
  _DesktopViewerState createState() => _DesktopViewerState();
}

class _DesktopViewerState extends State<DesktopViewer> {
  WebSocket? _socket;
  Uint8List? _lastFrame;

  @override
  void initState() {
    super.initState();
    _connect();
  }

  Future<void> _connect() async {
    try {
      _socket = await WebSocket.connect(widget.wsUrl);
      _socket!.listen((data) {
        if (data is List<int>) {
          setState(() {
            _lastFrame = Uint8List.fromList(data);
          });
        }
      }, onDone: () async {
        // socket closed
      }, onError: (err) {
        // handle error
      });
    } catch (e) {
      print('WS connect failed: $e');
    }
  }

  @override
  void dispose() {
    _socket?.close();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Center(
      child: _lastFrame != null
        ? Image.memory(_lastFrame!, gaplessPlayback: true, fit: BoxFit.contain)
        : CircularProgressIndicator(),
    );
  }
}

Notes:
- Use `Image.memory` which accepts a Uint8List of JPEG bytes and renders it. This keeps the client simple.
- If frames are large and rendering is slow, reduce `maxWidth` or `quality` when you start the WS stream.

5) Flutter: sending keyboard text and key events
- Use the same TCP socket (connected to port 5000). Send keyboard JSON messages (newline-delimited) like the examples in section 2.

Example to type text:
await sendControl(sock, {"keyboard": {"cmd": "type", "text": "Hello\n"}});

6) Motion and click controls
- The motion JSON format your desktop expects is (send over TCP):
  {"gyroX":0.12,"gyroY":-0.04,"leftClick":false,"rightClick":false}
- Send motion messages frequently (e.g. 20-60Hz depending on your sampling) but be aware of network load and processing on the desktop.

7) Troubleshooting
- No frames: verify the WS server is actually started. Send the start control JSON and check desktop console logs.
- WebSocket errors: confirm you used ws:// not http:// and correct IP/port. Check firewall rules.
- Large frames / dropped messages: lower `maxWidth` and `quality` in control JSON. Example: {"websocket":{"cmd":"start","port":8080,"fps":10,"maxWidth":800,"quality":0.4}}
- If you still prefer UDP streaming (already implemented), implement reassembly logic in Flutter to reconstruct fragments. WebSocket avoids fragmentation and is easier.

8) Quick test sequence (end-to-end)
1. Run desktop app: `./gradlew run` (or launch the jar). Confirm console prints "TCP Server running on port 5000".
2. From Flutter, open TCP socket to desktop:5000 and send: {"websocket":{"cmd":"start","port":8080,"fps":12}}
3. From Flutter, open WebSocket to ws://192.168.1.42:8080 and render binary messages using `Image.memory`.
4. Send keyboard JSON over the TCP socket and observe typing on the desktop.

9) Security & improvements
- Consider adding a simple token in the control JSON and only start WS if the token matches.
- Rate-limit motion messages or sample at a lower rate and smooth on the client to reduce network usage.

If you want, I can:
- Add a complete Flutter example app repository with controls for mouse/keyboard and the viewer screen.
- Add small server-side acknowledgements (JSON responses on TCP) so the Flutter client can confirm the WS server started.

-- End of prompt --

