# Build from the known working ddeed8ca baseline

The source tree uses `ddeed8ca` (official Vector 2.2 / 3112), which the user
reported works with their banking apps, plus the singleton lock-retention
and safe-uninstall fixes described in `late-root-activation.md`, with their
regression checks, and the manager-only Compose RectList crash fix documented
in `manager-rectlist-crash.md`. Native injection and app-scoping behavior
are unchanged from `ddeed8ca`.

History still contains the resource-cache bind commit `82112f99` and rendezvous
commit `eaffc816`: merging the fixes branch into master did not remove them.
They were subsequently undone by explicit revert commits `be83f850` and
`2616816f`, respectively, without rewriting published history. Seeing the
original commits in `git log` is expected; their changes are no longer active.

After both reverts, the complete Git tree was verified byte-for-byte against
`ddeed8ca` with only the patches from our lifecycle fix `a0167238` and manager
fix `1fa3681f` applied. Both trees were
`f69b31b9a2d5a5243e455f5b025148020ce8c6e7`. The subsequent change to this document
only clarifies the history; it does not change runtime code.

Banking compatibility still requires testing this locally signed build on the
phone. The reported good baseline had no enabled Xposed modules, while an
earlier failing setup had five. Keep the same modules and settings when making
a comparison. No claim that a particular later commit caused detection has
been established.

Use the bundled manager via KernelSU's Vector Execute action. An installed
official manager has a different signing certificate and cannot be updated by
this locally signed APK without first uninstalling that manager.

For an existing installation, update the module in place so configuration is
preserved. The old installed uninstaller cannot gain this fix retroactively;
uninstalling it before upgrading can still leave a daemon holding deleted files.
A full hardware reboot before re-rooting clears an existing orphan. A soft
reboot may leave it alive.

Both GitHub Actions workflows are manual-only in this fork. Pushes, tags, pull
requests, and merge queues do not start jobs. Run either workflow from GitHub's
Actions page when needed. Run `python3 -m unittest discover -s tests -v` locally
to check uninstall behavior and the manual-only workflow policy.
