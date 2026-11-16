"""Errors of the gym membership package."""

from __future__ import annotations


class DataError(Exception):
    """The input CSV cannot be used: a wrong header, a missing value or a bad value.

    The message names the line of the file and the field, so the user can find the problem.
    The original error, if there is one, is kept as ``__cause__`` by ``raise ... from``.
    """

    def __init__(self, message: str, *, line: int | None = None, field: str | None = None) -> None:
        """Create the error with the line number and the field name, when they are known."""
        self.line = line
        self.field = field
        place = []
        if line is not None:
            place.append(f"line {line}")
        if field is not None:
            place.append(f"field '{field}'")
        super().__init__(f"{', '.join(place)}: {message}" if place else message)
