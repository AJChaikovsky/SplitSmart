#!/usr/bin/env python3
"""Simple adb UI smoke test for SplitSmart."""
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
    sh("adb", "pull", "/sdcard/ui.xml", "/tmp/ui.xml")
    return Path("/tmp/ui.xml").read_text()


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
    time.sleep(0.6)


def tap_xy(x, y):
    sh("adb", "shell", "input", "tap", str(x), str(y))
    time.sleep(0.4)


def type_text(s):
    escaped = s.replace(" ", "%s").replace("@", "\\@")
    sh("adb", "shell", "input", "text", escaped)
    time.sleep(0.3)


def clear_and_type(rid, s):
    tap_id(rid)
    # Select-all-ish: move to end and delete many times
    for _ in range(40):
        sh("adb", "shell", "input", "keyevent", "67", check=False)  # DEL
    type_text(s)


def wait_text(substr, timeout=12):
    end = time.time() + timeout
    needle = substr.lower()
    while time.time() < end:
        t = texts()
        if any(needle in x.lower() for x in t):
            return t
        time.sleep(0.5)
    raise TimeoutError(f"Did not see text containing {substr!r}. Saw: {texts()}")


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
    time.sleep(1.5)

    print("Register Alice", alice)
    tap_id("btnGoRegister")
    clear_and_type("etName", "Alice")
    clear_and_type("etEmail", alice)
    clear_and_type("etUscId", alice_id)
    clear_and_type("etPassword", password)
    tap_id("btnRegister")
    wait_text("Your Groups", timeout=20)
    print("Alice registered OK")

    print("Create group")
    tap_id("fabCreate")
    clear_and_type("etName", "Roommates")
    clear_and_type("etDescription", "Shared apt")
    tap_id("btnSave")
    wait_text("Add expense", timeout=15)
    print("Group created OK")

    print("Logout Alice")
    sh("adb", "shell", "input", "keyevent", "4")  # back to home
    time.sleep(0.8)
    tap_id("btnProfile")
    tap_id("btnLogout")
    wait_text("Create an account")

    print("Register Bob", bob)
    tap_id("btnGoRegister")
    clear_and_type("etName", "Bob")
    clear_and_type("etEmail", bob)
    clear_and_type("etUscId", bob_id)
    clear_and_type("etPassword", password)
    tap_id("btnRegister")
    wait_text("Your Groups", timeout=20)
    print("Bob registered OK")

    print("Logout Bob / login Alice")
    tap_id("btnProfile")
    tap_id("btnLogout")
    wait_text("Create an account")
    clear_and_type("etEmail", alice)
    clear_and_type("etPassword", password)
    tap_id("btnLogin")
    wait_text("Your Groups", timeout=15)
    wait_text("Roommates", timeout=10)
    print("Alice sees group")

    # Open group
    xml = dump()
    # tap first group item roughly by text bounds
    m = re.search(r'text="Roommates"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)
    if not m:
        m = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"[^>]*text="Roommates"', xml)
    x1, y1, x2, y2 = map(int, m.groups())
    tap_xy((x1 + x2) // 2, (y1 + y2) // 2)
    wait_text("Add expense")

    print("Add Bob as member")
    clear_and_type("etMemberEmail", bob)
    tap_id("btnAddMember")
    time.sleep(3)
    t = texts()
    if not any("Bob" in x for x in t):
        print("WARN: Bob name not visible yet:", t)
    else:
        print("Bob added OK")

    print("Create equal expense $30")
    tap_id("btnAddExpense")
    wait_text("Participants")
    clear_and_type("etTitle", "Dinner")
    clear_and_type("etAmount", "30.00")
    # leave default OTHER / EQUAL
    # ensure both participants checked - default all checked
    tap_id("btnSave")
    # confirm dialog
    time.sleep(1)
    xml = dump()
    if any("Confirm shares" in x or "Review shares" in x for x in texts(xml)):
        # tap Save on dialog - usually positive button text Save
        m = re.search(r'text="Save"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', dump())
        if m:
            x1, y1, x2, y2 = map(int, m.groups())
            tap_xy((x1 + x2) // 2, (y1 + y2) // 2)
    time.sleep(3)
    print("After expense screen:", texts()[:30])

    print("Open balances")
    # may still be on group detail
    if not any("Balances" in x or "balances" in x.lower() for x in texts()):
        sh("adb", "shell", "input", "keyevent", "4")
        time.sleep(1)
    tap_id("btnBalances")
    time.sleep(3)
    bal = texts()
    print("Balances:", bal)
    ok = any("15" in x or "$15" in x for x in bal) or any("owe" in x.lower() for x in bal)
    print("PASS balances meaningful" if ok else "WARN balances unexpected")

    print("Open history")
    sh("adb", "shell", "input", "keyevent", "4")
    time.sleep(0.8)
    tap_id("btnHistory")
    time.sleep(2)
    hist = texts()
    print("History:", hist)
    print("PASS history has Dinner" if any("Dinner" in x for x in hist) else "WARN missing Dinner")

    print("Open monthly summary")
    sh("adb", "shell", "input", "keyevent", "4")
    time.sleep(0.8)
    tap_id("btnSummary")
    time.sleep(2)
    summ = texts()
    print("Summary:", summ)

    print("\nE2E finished.")
    print(f"Alice={alice} Bob={bob}")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as e:
        print("FAIL:", e)
        try:
            print("Screen:", texts())
        except Exception:
            pass
        sys.exit(1)
