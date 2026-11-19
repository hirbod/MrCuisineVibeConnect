#!/system/bin/sh
# Run as root on the robot (tools/install-robot.sh or install-robot.ps1 does it after installing Toque).
# 1. Allows Toque in Magisk, without the on-screen prompt: that prompt closes by itself after
#    10 s and Magisk then refuses Toque for good (the robot has no way to reach Magisk's settings).
# 2. Makes sure Magisk's busybox is in /data/adb/magisk: Toque needs it for the serial port.
# 3. Re-allows Toque at every boot (see below).
U=$(stat -c %u /data/data/app.toque 2>/dev/null)
[ -n "$U" ] || { echo "Toque is not installed"; exit 1; }
if magisk --sqlite "REPLACE INTO policies (uid,policy,until,logging,notification) VALUES($U,2,0,0,0)"; then
  echo "Toque allowed in Magisk"
fi
if [ ! -x /data/adb/magisk/busybox ]; then
  for b in /data/app/com.topjohnwu.magisk*/lib/arm*/libbusybox.so; do
    [ -f "$b" ] || continue
    mkdir -p /data/adb/magisk
    cp "$b" /data/adb/magisk/busybox && chmod 755 /data/adb/magisk/busybox
    chcon u:object_r:magisk_file:s0 /data/adb/magisk/busybox 2>/dev/null
    echo "Magisk busybox installed"
    break
  done
fi
[ -x /data/adb/magisk/busybox ] || echo "WARNING: no busybox in /data/adb/magisk (install the Magisk app: adb install Magisk-vXX.apk)"
# 3. Magisk drops Toque's policy again during boot on this robot (seen with Magisk 31 on the
#    SKMC 1200), and Toque's next root request then shows that prompt. Re-grant Toque at every
#    boot, once Android has finished starting (Toque itself waits ~60 s of uptime before asking).
mkdir -p /data/adb/service.d
cat > /data/adb/service.d/toque-su.sh <<'SCRIPT'
#!/system/bin/sh
until [ "$(getprop sys.boot_completed)" = "1" ]; do sleep 1; done
sleep 8
uid=$(/data/adb/magisk/busybox stat -c %u /data/data/app.toque 2>/dev/null)
[ -n "$uid" ] || exit 0
magisk --sqlite "DELETE FROM policies WHERE uid=$uid"
magisk --sqlite "INSERT INTO policies (uid,policy,until,logging,notification) VALUES($uid,2,0,0,0)"
SCRIPT
chmod 755 /data/adb/service.d/toque-su.sh && echo "Toque re-allowed in Magisk at every boot"
