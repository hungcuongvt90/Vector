#!/system/bin/sh

# A KernelSU soft reboot can leave vectord alive. Removing its lock file while
# it still holds it lets a reinstalled daemon lock a different inode and start
# beside it. Stop every survivor before unlinking the lock, sockets or database.
pids=$(pidof vectord 2>/dev/null)
if [ -n "$pids" ]; then
    kill -TERM $pids 2>/dev/null
    attempts=0
    while [ "$attempts" -lt 5 ]; do
        pids=$(pidof vectord 2>/dev/null)
        [ -n "$pids" ] || break
        sleep 1
        attempts=$((attempts + 1))
    done
    pids=$(pidof vectord 2>/dev/null)
    if [ -n "$pids" ]; then
        kill -KILL $pids 2>/dev/null
        sleep 1
    fi
    if pidof vectord >/dev/null 2>&1; then
        echo 'Vector: daemon is still running; retaining its data directory.' >&2
        exit 1
    fi
fi

rm -rf /data/adb/lspd
