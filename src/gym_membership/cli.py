"""Command-line interface of the gym membership package."""

from __future__ import annotations

import argparse
from collections.abc import Sequence

from . import __version__


def build_parser() -> argparse.ArgumentParser:
    """Create the parser of the command-line arguments."""
    parser = argparse.ArgumentParser(
        prog="gym-membership",
        description="Gym membership data processing, variant 20",
    )
    parser.add_argument(
        "--version",
        action="version",
        version=f"%(prog)s {__version__}",
    )
    return parser


def main(argv: Sequence[str] | None = None) -> int:
    """Parse the arguments, print the ready message and return the exit code."""
    build_parser().parse_args(argv)
    print("Gym membership tools are ready")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
