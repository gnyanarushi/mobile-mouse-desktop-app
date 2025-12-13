#!/bin/bash

# Test if Java Robot class can move cursor on Linux
# This script helps diagnose cursor movement issues

echo "======================================"
echo "Java Robot Cursor Movement Test"
echo "======================================"
echo ""

# Check system info
echo "System Information:"
echo "  OS: $(uname -s)"
echo "  Distribution: $(lsb_release -d 2>/dev/null | cut -f2 || echo "Unknown")"
echo "  Display Server: ${XDG_SESSION_TYPE:-Unknown}"
echo "  Display: ${DISPLAY:-Not set}"
echo "  User: $USER"
echo "  Groups: $(groups)"
echo ""

# Check if running with sudo
if [ "$EUID" -eq 0 ]; then
   echo "✓ Running with root privileges"
else
   echo "⚠ NOT running with root privileges"
   echo "  If test fails, try: sudo $0"
fi
echo ""

# Check input group membership
if groups | grep -q '\binput\b'; then
    echo "✓ User is in 'input' group"
else
    echo "⚠ User is NOT in 'input' group"
    echo "  To fix: sudo usermod -a -G input $USER"
    echo "  Then logout and login again"
fi
echo ""

# Create and run Java test
TEST_FILE="/tmp/RobotTest_$$.java"
cat > "$TEST_FILE" << 'JAVACODE'
import java.awt.*;

public class RobotTest_$$ {
    public static void main(String[] args) {
        try {
            System.out.println("Creating Robot instance...");
            Robot robot = new Robot();

            // Get current position
            Point before = MouseInfo.getPointerInfo().getLocation();
            System.out.println("Current cursor position: (" + before.x + ", " + before.y + ")");

            // Try to move cursor to a test position
            int testX = 500;
            int testY = 500;
            System.out.println("Attempting to move cursor to: (" + testX + ", " + testY + ")");

            robot.mouseMove(testX, testY);
            Thread.sleep(100);

            // Check if it actually moved
            Point after = MouseInfo.getPointerInfo().getLocation();
            System.out.println("Cursor position after move: (" + after.x + ", " + after.y + ")");

            // Restore original position
            robot.mouseMove(before.x, before.y);
            Thread.sleep(50);

            System.out.println("");
            if (after.x == testX && after.y == testY) {
                System.out.println("✓✓✓ SUCCESS! Robot can move cursor! ✓✓✓");
                System.out.println("Your application should work correctly.");
                System.exit(0);
            } else {
                System.out.println("✗✗✗ FAILED! Robot cannot move cursor! ✗✗✗");
                System.out.println("Expected: (" + testX + ", " + testY + ")");
                System.out.println("Got: (" + after.x + ", " + after.y + ")");
                System.out.println("");
                System.out.println("SOLUTIONS:");
                System.out.println("1. Run with sudo: sudo " + System.getProperty("user.dir") + "/test_robot.sh");
                System.out.println("2. Add user to input group: sudo usermod -a -G input " + System.getProperty("user.name"));
                System.out.println("3. If using Wayland, switch to X11 session");
                System.exit(1);
            }

        } catch (AWTException e) {
            System.err.println("Failed to create Robot: " + e.getMessage());
            System.err.println("Your system may not support Robot class");
            System.exit(2);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(3);
        }
    }
}
JAVACODE

# Replace placeholder with actual PID
sed -i "s/\\$\\$/$$/" "$TEST_FILE"

echo "Compiling test..."
javac "$TEST_FILE"

if [ $? -ne 0 ]; then
    echo "✗ Compilation failed. Make sure Java is installed."
    rm -f "$TEST_FILE"
    exit 1
fi

echo "Running test..."
echo "======================================"
echo ""

java -cp /tmp "RobotTest_$$"
TEST_RESULT=$?

echo ""
echo "======================================"

# Cleanup
rm -f "$TEST_FILE" "/tmp/RobotTest_$$.class"

if [ $TEST_RESULT -eq 0 ]; then
    echo ""
    echo "🎉 Your system is ready to run the Mobile Mouse application!"
    echo "Run with: ./gradlew run"
else
    echo ""
    echo "⚠ Robot class cannot move cursor on your system"
    echo "Follow the solutions above to fix the issue"
fi

exit $TEST_RESULT

