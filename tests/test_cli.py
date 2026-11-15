"""Tests of the command-line interface."""

import pytest

from gym_membership.cli import main


def test_main_returns_success(capsys: pytest.CaptureFixture[str]) -> None:
    """main() finishes with exit code 0 and prints the ready message."""
    assert main([]) == 0
    assert capsys.readouterr().out == "Gym membership tools are ready\n"
