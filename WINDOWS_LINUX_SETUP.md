# 🖱️ Mobile Mouse Desktop App - Windows & Linux Setup Guide

## ✅ Supported Platforms

- ✅ **Windows 10/11** - Fully supported, works out of the box
- ✅ **Linux (Ubuntu/Debian)** - Fully supported with xdotool or Robot class
- ✅ **Linux (Fedora/RHEL)** - Fully supported with xdotool or Robot class
- ✅ **Linux (Arch)** - Fully supported with xdotool or Robot class

---

## 🪟 Windows Users

### Requirements
- Java 11 or higher
- Windows 10 or Windows 11
- Internet connection for mobile-to-desktop communication

### Installation & Setup

#### Step 1: Check Java Installation
```bash
java -version
```
Should show Java 11 or higher.

#### Step 2: Clone/Navigate to Project
```bash
cd path/to/mobile-mouse-desktop-app
```

#### Step 3: Run the Application
```bash
./gradlew run
```

**Note:** On Windows, use `gradlew.bat run` if `gradlew` command doesn't work.

#### Step 4: Note Your IP Address
When the app starts, it will show you system information. Note your computer's IP address.

#### Step 5: Connect from Mobile
1. Open the Flutter app on your mobile device
2. Connect to: `<YOUR_COMPUTER_IP>:5000`
3. Grant any necessary permissions
4. Tilt your phone and watch the cursor move!

### Expected Output on Startup
```
╔═══════════════════════════════════════════════════════════╗
║     Mobile Mouse Controller - Desktop Application           ║
╚═══════════════════════════════════════════════════════════╝

📊 System Information:
  OS: Windows 10 10.0
  Java Version: 11.0.x
  User: YourUsername

🔧 Platform-Specific Setup:

  ✅ WINDOWS DETECTED
  The application is ready to use on Windows!
  • Cursor control: ✓ Fully supported
  • Mouse clicks: ✓ Fully supported
  • No additional setup needed

  📱 Ready to connect from mobile device:
     1. Note your computer's IP address
     2. Open Flutter app on mobile
     3. Connect to <YOUR_IP>:5000
     4. Enjoy your mobile mouse!

╔═══════════════════════════════════════════════════════════╗
║ Starting application...                                    ║
╚═══════════════════════════════════════════════════════════╝
```

### Finding Your IP Address on Windows
```bash
# Open Command Prompt and run:
ipconfig

# Look for IPv4 Address (usually starts with 192.168 or 10.0)
```

### Troubleshooting on Windows

**Issue: Port 5000 already in use**
```bash
# Check what's using port 5000
netstat -ano | findstr :5000

# Kill the process (replace PID with the actual process ID)
taskkill /PID <PID> /F
```

**Issue: Cursor not moving**
1. Check console for error messages
2. Verify mobile device is on same network
3. Check firewall settings - allow Java through firewall
4. Restart the application

**Issue: Compilation error**
```bash
./gradlew clean build
./gradlew run
```

---

## 🐧 Linux Users

### Requirements
- Java 11 or higher
- Linux desktop (Ubuntu/Debian, Fedora, Arch, etc.)
- xdotool (recommended but optional)

### Installation & Setup

#### Step 1: Check Java Installation
```bash
java -version
```
Should show Java 11 or higher.

#### Step 2: Install xdotool (Recommended)

**Ubuntu/Debian:**
```bash
sudo apt-get update
sudo apt-get install xdotool
```

**Fedora/RHEL:**
```bash
sudo dnf install xdotool
```

**Arch/Manjaro:**
```bash
sudo pacman -S xdotool
```

#### Step 3: Verify xdotool Installation
```bash
which xdotool
xdotool --version
```

#### Step 4: Navigate to Project
```bash
cd path/to/mobile-mouse-desktop-app
```

#### Step 5: Run the Application
```bash
./gradlew run
```

#### Step 6: Note Your IP Address
When the app starts, check the system information for your IP address.

#### Step 7: Connect from Mobile
Same as Windows - open Flutter app and connect to `<YOUR_IP>:5000`

### Expected Output on Startup

**With xdotool installed:**
```
╔═══════════════════════════════════════════════════════════╗
║     Mobile Mouse Controller - Desktop Application           ║
╚═══════════════════════════════════════════════════════════╝

📊 System Information:
  OS: Linux 5.10.0-x-generic
  Java Version: 11.0.x
  User: yourname
  Display: :0
  Session Type: x11

🔧 Platform-Specific Setup:

  🐧 LINUX DETECTED
  Session Type: X11

  ✅ xdotool: INSTALLED
  • Cursor control: ✓ Optimal (no sudo needed)
  • Mouse clicks: ✓ Optimal
  • Status: Ready to use!

  💡 Troubleshooting:
     • Check DISPLAY variable: echo $DISPLAY
     • Test xdotool: xdotool mousemove 100 100

╔═══════════════════════════════════════════════════════════╗
║ Starting application...                                    ║
╚═══════════════════════════════════════════════════════════╝
```

**Without xdotool (fallback):**
```
  🐧 LINUX DETECTED
  Session Type: X11

  ❌ xdotool: NOT INSTALLED
  • Recommended: Install xdotool for best experience
  • Command: sudo apt-get install xdotool
  • After install, restart the application

  ⚠️  Fallback: Using Robot class (may require sudo)
  • If cursor doesn't move, run: sudo ./gradlew run
```

### Finding Your IP Address on Linux
```bash
# Method 1: Using hostname
hostname -I

# Method 2: Using ifconfig
ifconfig | grep inet

# Method 3: Using ip command
ip addr show
```

### Troubleshooting on Linux

**Issue: xdotool not found**
```bash
# Install xdotool
sudo apt-get install xdotool  # Ubuntu/Debian
sudo dnf install xdotool      # Fedora
sudo pacman -S xdotool        # Arch
```

**Issue: Cursor not moving (even with xdotool)**
```bash
# Check DISPLAY variable
echo $DISPLAY

# Test xdotool directly
xdotool mousemove 100 100
xdotool click 1

# If above commands work but app doesn't, check session type
echo $XDG_SESSION_TYPE
```

**Issue: Port 5000 already in use**
```bash
# Find what's using port 5000
sudo netstat -tlnp | grep 5000

# Or with ss command
sudo ss -tlnp | grep 5000

# Kill the process
sudo kill <PID>
```

**Issue: Permission denied for X11**
```bash
# If running under different user/sudo
# Ensure DISPLAY is set correctly
export DISPLAY=:0

# Then run app
./gradlew run
```

**Issue: Wayland session limitations**
```bash
# Check session type
echo $XDG_SESSION_TYPE

# If it says 'wayland', for best results:
# 1. Switch to X11 session (if available)
# 2. Or use xdotool (has better Wayland support)
```

**Issue: Compilation error**
```bash
./gradlew clean build
./gradlew run
```

---

## 🔄 Platform-Specific Features

### Windows
| Feature | Status | Notes |
|---------|--------|-------|
| Cursor Movement | ✅ | Via Robot class |
| Mouse Clicks | ✅ | Left & Right |
| Multi-monitor | ✅ | Auto-detected |
| Administrator | ❌ | Not needed |
| Setup Time | ⏱️ | ~2 minutes |

### Linux with xdotool
| Feature | Status | Notes |
|---------|--------|-------|
| Cursor Movement | ✅ | Via xdotool (no sudo!) |
| Mouse Clicks | ✅ | Left & Right |
| Multi-monitor | ✅ | Auto-detected |
| Administrator | ❌ | Not needed |
| Setup Time | ⏱️ | ~5 minutes |

### Linux without xdotool (Fallback)
| Feature | Status | Notes |
|---------|--------|-------|
| Cursor Movement | ⚠️ | Via Robot (may need sudo) |
| Mouse Clicks | ⚠️ | Via Robot (may need sudo) |
| Multi-monitor | ✅ | Auto-detected |
| Administrator | ⚠️ | May need sudo |
| Setup Time | ⏱️ | ~2 minutes |

---

## 📊 Comparison: Windows vs Linux

| Aspect | Windows | Linux (xdotool) | Linux (Robot) |
|--------|---------|-----------------|---|
| **Cursor Movement** | ✅ Works | ✅ Works | ⚠️ May need sudo |
| **No sudo Required** | ✅ Yes | ✅ Yes | ❌ No |
| **Setup Complexity** | Simple | Simple | Simple |
| **Installation Time** | 2 min | 5 min | 2 min |
| **Recommended** | ✅ | ✅ | ⚠️ Fallback |

---

## 🎯 Quick Comparison Table

```
╔────────────────────────────────────────────────────────╗
║         Windows        │        Linux                  ║
╠────────────────────────────────────────────────────────╣
║ Ready to use:      ✅  │ Recommended:     xdotool      ║
║ No setup needed    ✅  │ Install cmd:     apt-get      ║
║ Just run:          ✅  │ Setup time:      5 min        ║
║                        │ Fallback:        Robot class  ║
╚────────────────────────────────────────────────────────╝
```

---

## 🔧 Configuration (Both Platforms)

After the app is running, you can adjust these settings in `Main.java`:

```java
// Cursor movement sensitivity (lower = slower)
processor.setSensitivity(10.0);  // Try: 5.0, 20.0, 50.0

// Smoothing of movement (0.0 = no smoothing)
processor.setSmoothing(0.1);     // Try: 0.0, 0.3, 0.5

// Dead zone (minimum movement threshold)
processor.setDeadZone(0.0);      // Try: 0.01, 0.05, 0.1
```

After changing, rebuild and restart:
```bash
./gradlew run
```

---

## 🎓 Getting Help

### Check Application Logs
The application prints diagnostic information when it starts. Look for:
- OS detection
- Java version
- xdotool status (Linux)
- Platform-specific instructions

### Common Solutions

| Problem | Windows | Linux |
|---------|---------|-------|
| Cursor not moving | Check console logs | Install xdotool |
| Port 5000 in use | netstat + taskkill | netstat + kill |
| Permission denied | Restart app | Run with sudo or add to input group |
| Can't compile | gradlew clean | gradlew clean |

---

## ✨ What's Supported

### ✅ Works Perfectly
- Windows 10 & 11
- Linux with X11 session
- Linux with xdotool
- Multi-monitor setups
- Network communication
- Real-time cursor tracking
- Dashboard visualization

### ⚠️ Limited Support
- Linux with Wayland (use xdotool for better results)
- Linux without xdotool (may need sudo)

### ❌ Not Supported
- macOS (would need separate implementation)
- Very old Java versions (<11)

---

## 🚀 Quick Start Commands

### Windows
```bash
# 1. Check Java
java -version

# 2. Navigate to project
cd path/to/mobile-mouse-desktop-app

# 3. Run
./gradlew run

# 4. Get IP address
ipconfig
```

### Linux
```bash
# 1. Check Java
java -version

# 2. Install xdotool (recommended)
sudo apt-get update
sudo apt-get install xdotool

# 3. Navigate to project
cd path/to/mobile-mouse-desktop-app

# 4. Run
./gradlew run

# 5. Get IP address
hostname -I
```

---

## 📞 Troubleshooting Checklist

### Before Reporting Issues
- [ ] Java 11+ installed
- [ ] xdotool installed (Linux)
- [ ] App compiles without errors (`./gradlew build`)
- [ ] Port 5000 not in use
- [ ] Mobile and desktop on same network
- [ ] Firewall allows port 5000
- [ ] Checked application startup logs

### Still Having Issues?
1. Check the application startup output
2. Refer to platform-specific troubleshooting above
3. Review the detailed documentation files
4. Ensure Java version is 11 or higher
5. Try `./gradlew clean build`

---

## ✅ Success Indicators

When everything is working correctly, you'll see:

**Windows:**
```
✅ WINDOWS DETECTED
✓ Cursor control: Fully supported
✓ Mouse clicks: Fully supported
```

**Linux:**
```
✅ xdotool: INSTALLED
✓ Cursor control: Optimal (no sudo needed)
✓ Mouse clicks: Optimal
```

Then when mobile connects:
```
Cursor movement: ✓ Working
Dashboard updates: ✓ Real-time
Clicks: ✓ Functional
```

---

**You're all set!** Both Windows and Linux are now fully supported! 🎉

