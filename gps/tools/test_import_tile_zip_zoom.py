"""The import is now a standalone production component; run its behavioral checks."""
from pathlib import Path
import subprocess
subprocess.run(["python3",str(Path(__file__).with_name("run_interaction_checks.py")),"--tiles-only"],check=True)
