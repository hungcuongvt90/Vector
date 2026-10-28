"""Uninstall must never unlink a live daemon's singleton lock or database."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest


SCRIPT = Path(__file__).resolve().parents[1] / "zygisk/module/uninstall.sh"
FIXTURE = r'''
pidof() {
    [ "$1" = vectord ] || return 2
    [ -s "$TEST_STATE" ] || return 1
    cat "$TEST_STATE"
}
kill() {
    printf '%s\n' "signal:$*" >> "$TEST_EVENTS"
    case "$TEST_SCENARIO:$1" in
        normal:-TERM|gone:-TERM|stubborn:-KILL) : > "$TEST_STATE" ;;
    esac
    [ "$TEST_SCENARIO" != gone ]
}
sleep() { printf '%s\n' wait >> "$TEST_EVENTS"; }
rm() {
    if [ -s "$TEST_STATE" ]; then
        printf '%s\n' unsafe-delete >> "$TEST_EVENTS"
        return 90
    fi
    [ "$*" = '-rf /data/adb/lspd' ] || return 91
    printf '%s\n' delete >> "$TEST_EVENTS"
}
. "$1"
'''


class UninstallTest(unittest.TestCase):
    def run_uninstall(self, scenario, pids="101 202"):
        with tempfile.TemporaryDirectory() as directory:
            state = Path(directory) / "state"
            events = Path(directory) / "events"
            state.write_text(pids)
            events.touch()
            result = subprocess.run(
                ["bash", "-c", FIXTURE, "fixture", str(SCRIPT)],
                env=dict(os.environ, TEST_STATE=str(state), TEST_EVENTS=str(events),
                         TEST_SCENARIO=scenario),
                capture_output=True, text=True, timeout=5,
            )
            return result, events.read_text().splitlines()

    def test_no_running_daemon_can_remove_data(self):
        result, events = self.run_uninstall("normal", "")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(events, ["delete"])

    def test_all_surviving_daemons_exit_before_data_is_removed(self):
        result, events = self.run_uninstall("normal")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(events, ["signal:-TERM 101 202", "delete"])

    def test_ignored_term_is_followed_by_kill_before_delete(self):
        result, events = self.run_uninstall("stubborn")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("signal:-KILL 101 202", events)
        self.assertEqual(events[-1], "delete")
        self.assertNotIn("unsafe-delete", events)

    def test_failed_stop_retains_data_and_reports_failure(self):
        result, events = self.run_uninstall("unstoppable")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("retaining its data", result.stderr)
        self.assertNotIn("delete", events)
        self.assertNotIn("unsafe-delete", events)

    def test_process_exiting_during_signal_is_not_an_uninstall_error(self):
        result, events = self.run_uninstall("gone")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(events[-1], "delete")


if __name__ == "__main__":
    unittest.main()
