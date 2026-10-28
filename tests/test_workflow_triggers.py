"""Keep every workflow opt-in, including workflows added in the future."""
from pathlib import Path
import re
import unittest


WORKFLOWS = Path(__file__).resolve().parents[1] / ".github/workflows"


class WorkflowTriggersTest(unittest.TestCase):
    def test_all_workflows_allow_only_manual_dispatch(self):
        workflows = sorted(WORKFLOWS.glob("*.yml")) + sorted(WORKFLOWS.glob("*.yaml"))
        self.assertTrue(workflows, "No workflows found; check the fixture path")
        for workflow in workflows:
            with self.subTest(workflow=workflow.name):
                # Enforce the small, dependency-free trigger format used by this fork.
                # A different YAML spelling must be reviewed rather than silently skipped.
                text = workflow.read_text()
                triggers = re.findall(r"^on:[^\n]*(?:\n[ \t]+[^\n]*|\n[ \t]*)*", text, re.MULTILINE)
                self.assertEqual(len(triggers), 1, "Expected one explicit workflow trigger block")
                lines = [line for line in triggers[0].splitlines()
                         if line.strip() and not line.lstrip().startswith("#")]
                self.assertEqual(lines, ["on:", "  workflow_dispatch:"],
                                 "Workflow must run only on manual dispatch")


if __name__ == "__main__":
    unittest.main()
