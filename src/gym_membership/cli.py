"""Command-line interface: CSV of memberships in, text report out."""

from __future__ import annotations

import argparse
import sys
from collections.abc import Sequence
from datetime import date
from pathlib import Path

from . import __version__
from .errors import DataError
from .loader import load_memberships
from .reporting import format_report, write_report

DEFAULT_DATE = date(2026, 11, 1)
"""Reference date of variant 20; fixed so the report does not depend on the day it is run."""

EXIT_OK = 0
EXIT_DATA_ERROR = 1
# Exit code 2 is returned by argparse for invalid arguments.


def iso_date(text: str) -> date:
    """Convert a command-line value in ISO format to a date."""
    try:
        return date.fromisoformat(text)
    except ValueError as error:
        raise argparse.ArgumentTypeError(f"not a date (YYYY-MM-DD): '{text}'") from error


def build_parser() -> argparse.ArgumentParser:
    """Create the parser of the command-line arguments."""
    parser = argparse.ArgumentParser(
        prog="gym-membership",
        description="Report on gym memberships from a CSV file (variant 20)",
    )
    parser.add_argument("input", type=Path, help="CSV file with memberships")
    parser.add_argument("output", type=Path, help="report file to write")
    parser.add_argument(
        "--date",
        type=iso_date,
        default=DEFAULT_DATE,
        help=f"reference date YYYY-MM-DD (default: {DEFAULT_DATE.isoformat()})",
    )
    parser.add_argument("--version", action="version", version=f"%(prog)s {__version__}")
    return parser


def main(argv: Sequence[str] | None = None) -> int:
    """Read the CSV, write the report and return the exit code."""
    parser = build_parser()
    args = parser.parse_args(argv)
    if args.input.resolve() == args.output.resolve():
        parser.error("the report must not overwrite the input file")

    try:
        records = load_memberships(args.input)
        text = format_report(records, args.date)
        write_report(args.output, text)
    except (DataError, OSError) as error:
        print(f"Error: {error}", file=sys.stderr)
        return EXIT_DATA_ERROR

    print(text, end="")
    print(f"Report saved to {args.output}")
    return EXIT_OK


if __name__ == "__main__":
    raise SystemExit(main())
