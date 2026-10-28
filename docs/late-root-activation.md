# Repeated soft reboots leave Vector inactive

On a Find X8 Ultra running ColorOS 17 with late-load KernelSU and NyaZygisk,
the official 2.2 / 3114 build (`b7cf93e1`) remained inactive after an emulated
soft reboot. This build already contains the rendezvous and death-recipient
fix in `eaffc816`; updating to that commit alone does not address this failure.

The 2026-10-28 14:03 capture showed an existing `vectord` (PID 25545) surviving
the restart while a second daemon (PID 16143) initialized. The second daemon
crashed with SIGSEGV on a Binder thread. Its tombstone reports a likely stack
overflow and 512 frames repeatedly forwarding `SystemServerService.onTransact`
through Binder. The survivor no longer held a descriptor for `/data/adb/lspd/lock`
or the corresponding exclusive entry in `/proc/locks`.

The optimized release mapping explains the missing lock: R8 removes both
`FileSystem.fileLock` and `FileSystem.lockChannel`. They are private fields used
only to retain ownership after startup. Once their stores disappear, garbage
collection can close the channel and release its process-wide lock. The next
daemon then passes `tryLock()` despite the original still running.

Preserve both lifetime roots with explicit R8 member rules. The release ZIP now
depends on `:zygisk:checkDaemonLockRetentionRelease`, which inspects the actual
optimized daemon DEX for both static fields and assignments to them. A JVM test
of the unoptimized source would miss this failure.

After installing the patched module, perform a full device reboot before
re-rooting. An update cannot recover a lock already discarded by a running
older daemon. Validate that one daemon remains, its lock descriptor and exclusive
kernel lock survive normal use, Vector activates, and the same daemon handles a
subsequent soft reboot. Check Coogle's framework connection and companion state
after that. Keep logcat running throughout; host artifact checks do not prove
the behavior of the device's root and injection stack.

## Uninstall followed by a soft reboot and reinstall

A later capture at 17:04 showed a second route to the same duplicate-daemon
failure. The patched daemon (PID 10967) still held its exclusive lock, but FD 66
pointed to `lock (deleted)`, inode 83671. The newly installed path was inode
83154. Its open configuration database and log files were also deleted. The user
confirmed the sequence: uninstall, soft reboot, install Vector again.

The original `uninstall.sh` only removed `/data/adb/lspd`. KernelSU's emulated
soft reboot can leave the old daemon alive, so deleting that directory unlinks
its lock without stopping it. A new daemon can then acquire an independent lock
on the replacement file. Lock retention alone cannot prevent this. The second
daemon (PID 6878) crashed in repeated Binder forwarding; the survivor's CLI
socket returned connection refused.

Uninstall now signals every `vectord` process, waits for normal termination,
escalates to SIGKILL if necessary, and verifies that no daemon remains before
removing data. If a process cannot be stopped, the script reports failure and
retains the directory. This preserves the singleton lock until its owner exits.
The earlier R8 lock-retention fix remains necessary for normal operation.

Run `python3 -m unittest discover -s tests -p test_uninstall.py -v` from the
repository root. The five shell fixtures cover no daemon, multiple survivors,
ignored TERM, failure to stop, and a process disappearing while signalled. The
original one-line script fails four of those tests. The tests substitute signal
and removal commands so they cannot kill host processes or remove phone data.

This lifecycle fix concerns Vector activation. It does not establish the cause
of banking apps' root warnings. To isolate that question, compare the same
KernelSU/NeoZygisk/HMA configuration with Vector enabled and disabled, restarting
the framework between states and confirming that the intended modules actually
loaded. Disabling a module without restarting does not remove existing hooks.
