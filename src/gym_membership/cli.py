"""Command-line interface of the gym membership package."""

from collections.abc import Sequence


def main(argv: Sequence[str] | None = None) -> int:
    """Print the ready message and return the exit code."""
    del argv
    print("Gym membership tools are ready")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
