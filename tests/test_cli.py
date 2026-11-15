"""Tests of the command-line interface."""

import tomllib
from importlib.metadata import version
from pathlib import Path

import pytest

from gym_membership import __version__
from gym_membership.cli import main

PYPROJECT = Path(__file__).resolve().parents[1] / "pyproject.toml"


def test_main_returns_success(capsys: pytest.CaptureFixture[str]) -> None:
    """main() finishes with exit code 0 and prints the ready message."""
    assert main([]) == 0
    assert capsys.readouterr().out == "Gym membership tools are ready\n"


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


def test_unknown_option_is_rejected(capsys: pytest.CaptureFixture[str]) -> None:
    """An unknown option ends with exit code 2 and a usage message on stderr."""
    with pytest.raises(SystemExit) as stop:
        main(["--unknown"])

    assert stop.value.code == 2
    assert "unrecognized arguments: --unknown" in capsys.readouterr().err
