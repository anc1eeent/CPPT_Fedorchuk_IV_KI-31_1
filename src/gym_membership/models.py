"""Object model of variant 20: a gym membership and its subtypes."""

from __future__ import annotations

from abc import ABC, abstractmethod
from datetime import date
from typing import ClassVar


class Membership(ABC):
    """A gym membership of one customer for a period of time.

    The class is abstract: the price of a visit depends on the subtype, so only
    MonthlyMembership and AnnualMembership are created.
    """

    kind: ClassVar[str] = "membership"
    """Name of the subtype, equal to the value of the CSV column ``kind``."""

    def __init__(self, customer: str, start: date, end: date, visits: int) -> None:
        """Create a membership; every value is checked."""
        self.customer = customer
        self.visits = visits
        if end <= start:
            raise ValueError(f"end {end} must be after start {start}")
        self._start = start
        self._end = end

    @property
    def customer(self) -> str:
        """Name of the customer, without spaces around it."""
        return self._customer

    @customer.setter
    def customer(self, value: str) -> None:
        """Set the name; a blank name raises ValueError."""
        if not value.strip():
            raise ValueError("customer must not be blank")
        else:
            self._customer = value.strip()

    @property
    def visits(self) -> int:
        """Number of visits, a whole number from zero."""
        return self._visits

    @visits.setter
    def visits(self, value: int) -> None:
        """Set the visits; a negative number or a value that is not int raises ValueError."""
        if not isinstance(value, int) or value < 0:
            raise ValueError(f"visits must be a whole number from 0, got {value!r}")
        else:
            self._visits = value

    @property
    def start(self) -> date:
        """First day of the membership."""
        return self._start

    @property
    def end(self) -> date:
        """First day after the membership."""
        return self._end

    @property
    def months(self) -> int:
        """Whole calendar months from start to end."""
        return (self.end.year - self.start.year) * 12 + (self.end.month - self.start.month)

    def is_active_on(self, day: date) -> bool:
        """Return True if the membership is active on the day: start <= day < end."""
        return self.start <= day < self.end

    @abstractmethod
    def visit_cost(self) -> float:
        """Return the price of one visit; every subtype has its own formula."""

    def __str__(self) -> str:
        """Short text for people."""
        return f"{self.customer}: {self.kind}, {self.start} to {self.end}, {self.visits} visits"

    def __repr__(self) -> str:
        """Text for developers: the class and every value."""
        return (
            f"{type(self).__name__}(customer={self.customer!r}, start={self.start!r}, "
            f"end={self.end!r}, visits={self.visits})"
        )

    def __eq__(self, other: object) -> bool:
        """Equal when the subtype and all values are equal."""
        if not isinstance(other, Membership):
            return NotImplemented
        return type(self) is type(other) and (self.customer, self.start, self.end, self.visits) == (
            other.customer,
            other.start,
            other.end,
            other.visits,
        )

    def __hash__(self) -> int:
        """Hash of the values that never change, consistent with __eq__."""
        return hash((type(self), self.start, self.end))


class MonthlyMembership(Membership):
    """A membership paid every month; the price of a visit grows with the months."""

    kind: ClassVar[str] = "monthly"
    PRICE_PER_MONTH: ClassVar[float] = 600.0
    """Price of one month in hryvnias."""

    def visit_cost(self) -> float:
        """Return the price of all months divided by the visits (at least one visit)."""
        return self.PRICE_PER_MONTH * self.months / max(1, self.visits)


class AnnualMembership(Membership):
    """A membership paid once for the whole contract, whatever its length."""

    kind: ClassVar[str] = "annual"
    CONTRACT_PRICE: ClassVar[float] = 5400.0
    """Price of the whole contract in hryvnias."""

    def visit_cost(self) -> float:
        """Return the contract price divided by the visits (at least one visit)."""
        return self.CONTRACT_PRICE / max(1, self.visits)
