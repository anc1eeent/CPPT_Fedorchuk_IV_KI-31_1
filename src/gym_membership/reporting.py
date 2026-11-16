"""Text report of variant 20 with a stable order of lines and values."""

from __future__ import annotations

from datetime import date
from pathlib import Path

from .analysis import active_on, average_visits, leader, unique_customers, visits_by_kind
from .loader import Membership

TITLE = "Gym membership report"


def format_line(label: str, *values: object, joiner: str = ", ", **pairs: object) -> str:
    """Build one line of the report: ``label: values, key=value``.

    Positional values are listed in the given order, keyword pairs follow as ``key=value``
    in the order they were passed. A line without values reads ``label: none``.
    """
    parts = [str(value) for value in values] + [f"{key}={value}" for key, value in pairs.items()]
    return f"{label}: {joiner.join(parts) if parts else 'none'}"


def format_report(records: list[Membership], reference_date: date) -> str:
    """Return the full report text; the same records always give the same text.

    Active customers keep the order of the input file, other customers are sorted by their
    Unicode code points, so the order does not depend on the language of the system.
    Names are joined with "; " because a name may contain a comma.
    """
    active = active_on(records, reference_date)
    customers = sorted(unique_customers(records))
    top = leader(records)
    lines = [
        TITLE,
        format_line("Reference date", reference_date.isoformat()),
        format_line("Records", len(records)),
        format_line("Active memberships", len(active)),
        format_line("Active customers", *(record["customer"] for record in active), joiner="; "),
        format_line("Visits by kind", **visits_by_kind(records)),
        format_line("Unique customers", len(customers)),
        format_line("Customers", *customers, joiner="; "),
        format_line("Average visits", f"{average_visits(records):.2f}"),
        format_line("Leader", *([f"{top['customer']} ({top['visits']} visits)"] if top else [])),
    ]
    return "\n".join(lines) + "\n"


def write_report(path: Path, text: str) -> None:
    """Write the report as UTF-8 with LF line ends; create the folder if it is missing."""
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as file:
        file.write(text)
