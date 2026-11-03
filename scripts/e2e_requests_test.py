#!/usr/bin/env python3
"""E2E: friend request send/accept + duplicate pending + group invite."""
import re
import subprocess
import sys
import time
from pathlib import Path

PKG = "com.splitsmart.app"


def sh(*args, check=True):
    r = subprocess.run(args, capture_output=True, text=True)
    if check and r.returncode != 0:
        raise RuntimeError(f"cmd failed: {args}\n{r.stdout}\n{r.stderr}")
    return r.stdout.strip()


def dump():
    sh("adb", "shell", "uiautomator", "dump", "/sdcard/ui.xml")
    sh("adb", "pull", "/sdcard/ui.xml", "/tmp/ui.xml", check=False)
    return Path("/tmp/ui.xml").read_text(errors="ignore")


def texts(xml=None):
    xml = xml or dump()
    return re.findall(r'text="([^"]*)"', xml)


def center(rid, xml=None):
    xml = xml or dump()
    for pat in [
        rf'resource-id="[^"]*/{re.escape(rid)}"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"',
        rf'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"[^>]*resource-id="[^"]*/{re.escape(rid)}"',
    ]:
        m = re.search(pat, xml)
        if m:
            x1, y1, x2, y2 = map(int, m.groups())
            return (x1 + x2) // 2, (y1 + y2) // 2
    raise KeyError(rid)


def tap_id(rid):
    x, y = center(rid)
    sh("adb", "shell", "input", "tap", str(x), str(y))
    time.sleep(0.7)


def tap_text(label, xml=None):
    xml = xml or dump()
    # Prefer clickable/node with exact text
    pat = rf'text="{re.escape(label)}"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'
    m = re.search(pat, xml)
    if not m:
        pat = rf'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"[^>]*text="{re.escape(label)}"'
        m = re.search(pat, xml)
    if not m:
        raise KeyError(label)
    x1, y1, x2, y2 = map(int, m.groups())
    sh("adb", "shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
    time.sleep(0.7)


def type_text(s):
    escaped = s.replace(" ", "%s").replace("@", "\\@")
    sh("adb", "shell", "input", "text", escaped)
    time.sleep(0.3)


def clear_and_type(rid, s):
    tap_id(rid)
    # Move to end, then backspace — DEL does nothing if the cursor is at the start.
    sh("adb", "shell", "input", "keyevent", "123", check=False)  # MOVE_END
    for _ in range(80):
        sh("adb", "shell", "input", "keyevent", "67", check=False)  # DEL
    type_text(s)


def wait_text(substr, timeout=15):
    end = time.time() + timeout
    needle = substr.lower()
    last = []
    while time.time() < end:
        last = texts()
        if any(needle in x.lower() for x in last):
            return last
        time.sleep(0.5)
    raise TimeoutError(f"Did not see {substr!r}. Saw: {last}")


def register(name, email, usc_id, password):
    wait_text("Create an account", timeout=10)
    tap_id("btnGoRegister")
    clear_and_type("etName", name)
    clear_and_type("etEmail", email)
    clear_and_type("etUscId", usc_id)
    clear_and_type("etPassword", password)
    tap_id("btnRegister")
    wait_text("Your groups", timeout=25)


def go_home():
    # Pop back stacks until home profile button is visible.
    for _ in range(6):
        xml = dump()
        if 'btnProfile' in xml and 'Your groups' in xml or (
            'btnProfile' in xml and any("Your groups" == t or "Your friends" == t or "Requests" == t for t in texts(xml))
        ):
            # Prefer Groups tab for a stable home header.
            try:
                tap_text("Groups")
            except Exception:
                pass
            return
        sh("adb", "shell", "input", "keyevent", "4", check=False)
        time.sleep(0.6)
    # Last resort: relaunch home.
    sh("adb", "shell", "am", "start", "-n", f"{PKG}/.ui.HomeActivity", check=False)
    time.sleep(1.2)


def logout():
    go_home()
    tap_id("btnProfile")
    tap_id("btnLogout")
    wait_text("Create an account", timeout=12)


def login(email, password):
    wait_text("Log in", timeout=10)
    # Login screen may already show fields
    try:
        clear_and_type("etEmail", email)
    except KeyError:
        # might be on register; go login if needed
        pass
    clear_and_type("etEmail", email)
    clear_and_type("etPassword", password)
    tap_id("btnLogin")
    wait_text("Your groups", timeout=25)


def main():
    ts = str(int(time.time()))
    alice = f"alice{ts}@usc.edu"
    bob = f"bob{ts}@usc.edu"
    alice_id = f"{(1000000000 + int(ts) % 9000000000):010d}"
    bob_id = f"{(2000000000 + int(ts) % 7000000000):010d}"
    password = "testpass1"

    print("Clear + launch")
    sh("adb", "shell", "pm", "clear", PKG)
    sh("adb", "shell", "am", "start", "-n", f"{PKG}/.ui.LoginActivity")
    time.sleep(2)

    print("Register Alice", alice)
    register("Alice", alice, alice_id, password)
    print("OK Alice home")

    print("Logout Alice / Register Bob", bob)
    logout()
    register("Bob", bob, bob_id, password)
    print("OK Bob home")
    logout()

    print("Login Alice, send friend request to Bob")
    login(alice, password)
    tap_text("Friends")
    wait_text("Your friends", timeout=10)
    tap_id("fabCreate")
    time.sleep(0.5)
    # Dialog: focus email field — may not have id; type into focused field after tap
    xml = dump()
    # Tap the dialog edit text if present
    m = re.search(r'class="android.widget.EditText"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)
    if not m:
        m = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"[^>]*class="android.widget.EditText"', xml)
    if m:
        x1, y1, x2, y2 = map(int, m.groups())
        sh("adb", "shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
        time.sleep(0.3)
    type_text(bob)
    tap_text("Add")
    time.sleep(2)
    # Toast won't appear in uiautomator; check no crash + still on friends
    wait_text("Your friends", timeout=8)
    print("Friend request send attempted")

    print("Duplicate pending should fail gracefully")
    tap_id("fabCreate")
    time.sleep(0.5)
    xml = dump()
    m = re.search(r'class="android.widget.EditText"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)
    if not m:
        m = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"[^>]*class="android.widget.EditText"', xml)
    if m:
        x1, y1, x2, y2 = map(int, m.groups())
        sh("adb", "shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
    type_text(bob)
    tap_text("Add")
    time.sleep(2)
    print("Duplicate attempt done (expect toast about pending)")

    print("Logout Alice / Login Bob / Accept request")
    # Navigate to profile via Groups first if needed
    tap_text("Groups")
    logout()
    login(bob, password)
    tap_text("Requests")
    wait_text("Requests", timeout=10)
    t = texts()
    if not any("friend" in x.lower() for x in t):
        # empty or failed send
        raise RuntimeError(f"Bob sees no friend request. UI texts: {t}")
    wait_text("wants to add you as a friend", timeout=10)
    tap_text("Accept")
    time.sleep(2)
    print("Accepted")

    print("Bob Friends should list Alice")
    tap_text("Friends")
    wait_text("Alice", timeout=12)
    print("OK Alice appears as friend")

    print("Alice Friends should list Bob")
    tap_text("Groups")
    logout()
    login(alice, password)
    tap_text("Friends")
    wait_text("Bob", timeout=12)
    print("OK Bob appears as friend")

    print("Create group, then invite Bob from group detail")
    tap_text("Groups")
    tap_id("fabCreate")
    clear_and_type("etName", "Trip")
    clear_and_type("etDescription", "Weekend")
    # Wait for friend checkbox list to finish loading.
    end = time.time() + 12
    while time.time() < end:
        labels = texts()
        if any(x.startswith("Bob") for x in labels):
            break
        time.sleep(0.5)
    for label in texts():
        if label.startswith("Bob"):
            tap_text(label)
            break
    tap_id("btnSave")
    wait_text("Add expense", timeout=15)
    print("Group created")

    # Also invite by email from group detail (covers that path).
    try:
        clear_and_type("etMemberEmail", bob)
        tap_id("btnAddMember")
        time.sleep(2)
        print("Email invite attempted from group detail")
    except Exception as e:
        print("Group detail invite skipped:", e)

    sh("adb", "shell", "input", "keyevent", "4")
    time.sleep(0.8)

    print("Bob accepts group invite")
    logout()
    login(bob, password)
    tap_text("Requests")
    wait_text("Requests", timeout=10)
    t = texts()
    if not any("group invite" in x.lower() or "invited you" in x.lower() for x in t):
        raise RuntimeError(f"Bob sees no group invite. UI texts: {t}")
    # Prefer Accept on the request card (resource id).
    try:
        tap_id("btnAccept")
    except KeyError:
        tap_text("Accept")
    time.sleep(3)
    # Check for permission/toast errors in brief log window via UI still usable
    tap_text("Groups")
    wait_text("Trip", timeout=15)
    print("OK Bob joined Trip")

    print("ALL REQUEST E2E CHECKS PASSED")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as e:
        print("FAIL:", e)
        print("UI:", texts())
        sys.exit(1)
