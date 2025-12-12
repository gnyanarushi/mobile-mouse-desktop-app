# Mobile Mouse — Desktop Application

This repository contains the desktop/server portion of "Mobile Mouse" — a small Java desktop application that accepts motion/control data from a mobile client and moves the mouse or emulates input on the host machine.

This README documents what the application does, how to build and run it, the main components, and troubleshooting tips.

## Features

- Listens for incoming TCP connections from a mobile client.
- Receives motion and control data and processes it into mouse movement actions.
- Simple, dependency-free Java application using Gradle for build and execution.

## Project structure

- `src/main/java/com/mousecontrol`
  - `Main.java` — application entry point; starts the server.
  - `communication/TCPServer.java` — TCP server accepting client connections and receiving data.
  - `controller/MouseController.java` — translates processed data into system mouse actions.
  - `models/MotionData.java` — model class representing incoming motion/control payloads.
  - `processor/MovementProcessor.java` — converts raw motion data into actionable movement deltas.
- `build.gradle.kts` — Gradle build script.

## Requirements

- Java 8+ (JDK 8 or newer recommended)
- Gradle wrapper is included (`./gradlew`) so you don't need Gradle preinstalled.

## Build and run

From the project root you can build and run the application using the bundled Gradle wrapper.

Build:

```bash
./gradlew clean build
```

Run (from project root):

```bash
./gradlew run
```

Alternatively, run the compiled JAR from `build/libs` if you prefer to produce a jar with a custom task.

## Configuration

- The server currently uses a TCP port configured in code (see `TCPServer.java`).
- If you need to change the listening port or other runtime parameters, modify the constants in `TCPServer.java` or add a small configuration loader.

## How it works (high level)

1. `Main` starts the `TCPServer`.
2. A mobile client connects and streams motion events (e.g., orientation, accelerometer, touch) as a simple serialized payload.
3. `TCPServer` parses incoming bytes into `MotionData` objects and forwards them to `MovementProcessor`.
4. `MovementProcessor` converts motion events into mouse deltas and passes them to `MouseController`.
5. `MouseController` uses the host OS input APIs (via Java Robot or native integration) to move the cursor or perform clicks.

## Usage

1. Start the desktop app on the machine you want to control (see run instructions above).
2. On the mobile client (not included in this repo), configure it to connect to the desktop machine's IP address and the port the server is listening on.
3. Use the mobile client to move/gesture; the desktop app will interpret and apply the movements.

## Logs and debugging

- Console output shows connection events and basic errors.
- For more detailed debugging, add logging statements in `TCPServer`, `MovementProcessor`, and `MouseController`.

## Common issues

- "Connection refused": ensure the desktop app is running and the port is open in any firewall.
- Bad/unknown payloads: confirm the mobile client and server expect the same data format.
- Cursor doesn't move: verify the `MouseController` implementation and that the JVM has permission to control the mouse on your OS.

## Tests

There are no automated tests included by default. Consider adding unit tests for `MovementProcessor` and integration tests that simulate incoming `MotionData` payloads.

## Contributing

Contributions are welcome. Suggested improvements:

- Add a configuration file or CLI options for port and sensitivity settings.
- Add unit tests and CI integration.
- Provide a packaged JAR or native installer.

Please open issues or pull requests with clear descriptions and a small, focused change set.

## License

Add a license file if you intend to open-source the project. If this is private, treat accordingly.

---

If you'd like, I can also:
- Add a small sample mobile payload generator for local testing.
- Add a `--port` command-line option or a simple `application.properties` loader.
- Create unit tests for `MovementProcessor`.

Tell me which follow-up you'd like and I'll implement it next.
