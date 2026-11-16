"""Tests of the command-line interface."""

import os
import subprocess
import sys
import tomllib
from importlib.metadata import version
from pathlib import Path

import pytest

from gym_membership import __version__
from gym_membership.cli import main

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "data" / "input.csv"
PYPROJECT = ROOT / "pyproject.toml"


def test_report_is_printed_and_saved(tmp_path: Path, capsys: pytest.CaptureFixture[str]) -> None:
    """A valid CSV gives exit code 0, the report on the console and in the file."""
    output = tmp_path / "reports" / "summary.txt"

    assert main([str(DATA), str(output)]) == 0

    printed = capsys.readouterr().out
    report = output.read_text(encoding="utf-8")
    assert printed == report + f"Report saved to {output}\n"
    assert "Leader: Іван Федорчук (45 visits)" in report


def test_date_option_changes_the_selection(tmp_path: Path) -> None:
    """--date sets the reference date of the report."""
    output = tmp_path / "summary.txt"

    assert main([str(DATA), str(output), "--date", "2025-01-01"]) == 0

    assert "Active memberships: 0" in output.read_text(encoding="utf-8")


def test_broken_csv_gives_exit_code_1(tmp_path: Path, capsys: pytest.CaptureFixture[str]) -> None:
    """A broken row is reported on stderr with its line; no report is written."""
    broken = tmp_path / "broken.csv"
    broken.write_text(
        "customer,kind,start,end,visits\nАнна,monthly,2026-10-05,2026-11-05,x\n", encoding="utf-8"
    )
    output = tmp_path / "summary.txt"

    assert main([str(broken), str(output)]) == 1

    assert capsys.readouterr().err == "Error: line 2, field 'visits': not a whole number: 'x'\n"
    assert not output.exists()


def test_missing_input_gives_exit_code_1(
    tmp_path: Path, capsys: pytest.CaptureFixture[str]
) -> None:
    """A missing input file is reported, not raised."""
    assert main([str(tmp_path / "missing.csv"), str(tmp_path / "summary.txt")]) == 1

    assert capsys.readouterr().err.startswith("Error: ")


@pytest.mark.parametrize(
    "arguments",
    [
        [],
        [str(DATA)],
        [str(DATA), "summary.txt", "--date", "01.11.2026"],
        [str(DATA), str(DATA)],
        [str(DATA), "summary.txt", "--unknown"],
    ],
)
def test_invalid_arguments_give_exit_code_2(arguments: list[str]) -> None:
    """Missing paths, a bad date, overwriting the input or an unknown option end with code 2."""
    with pytest.raises(SystemExit) as stop:
        main(arguments)

    assert stop.value.code == 2


def test_version_prints_installed_version(capsys: pytest.CaptureFixture[str]) -> None:
    """--version prints the program name and the installed version, then exits with 0."""
    with pytest.raises(SystemExit) as stop:
        main(["--version"])

    assert stop.value.code == 0
    assert capsys.readouterr().out == f"gym-membership {version('gym-membership-py')}\n"


def test_version_matches_pyproject() -> None:
    """The version of the package equals project.version in pyproject.toml."""
    with PYPROJECT.open("rb") as file:
        project = tomllib.load(file)["project"]

    assert __version__ == project["version"]


def test_module_runs_as_a_program(tmp_path: Path) -> None:
    """python -m gym_membership.cli works in a separate process."""
    output = tmp_path / "summary.txt"
    env = {**os.environ, "PYTHONUTF8": "1"}  # the Windows runner console is not UTF-8

    result = subprocess.run(
        [sys.executable, "-m", "gym_membership.cli", str(DATA), str(output)],
        capture_output=True,
        text=True,
        encoding="utf-8",
        env=env,
        check=False,
    )

    assert result.returncode == 0, result.stderr
    assert output.read_text(encoding="utf-8").startswith("Gym membership report\n")
