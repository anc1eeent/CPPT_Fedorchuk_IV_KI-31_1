"""Tests of the base class Membership. They describe what models.py must do."""

from datetime import date

import pytest

from gym_membership.models import Membership

START = date(2026, 10, 5)
END = date(2026, 11, 5)


class Plain(Membership):
    """The smallest concrete membership: the base class itself is abstract."""

    kind = "plain"

    def visit_cost(self) -> float:
        """Return a fixed cost; the formula does not matter for these tests."""
        return 0.0


class Other(Membership):
    """Another subtype with the same fields, to check equality between subtypes."""

    kind = "other"

    def visit_cost(self) -> float:
        """Return a fixed cost."""
        return 0.0


def anna(visits: int = 12) -> Plain:
    """Build the membership used in most tests."""
    return Plain("Анна Литвин", START, END, visits)


def test_init_stores_the_values() -> None:
    """__init__ keeps every value; spaces around the name are removed."""
    membership = Plain("  Анна Литвин  ", START, END, 12)

    assert membership.customer == "Анна Литвин"
    assert (membership.start, membership.end, membership.visits) == (START, END, 12)


def test_base_class_cannot_be_created() -> None:
    """Membership is abstract: only a subtype with visit_cost() can be created."""
    with pytest.raises(TypeError):
        Membership("Анна Литвин", START, END, 12)  # type: ignore[abstract]


def test_customer_setter_checks_the_value() -> None:
    """A new name is stored without spaces; a blank name is rejected."""
    membership = anna()

    membership.customer = " Анна Коваль "
    assert membership.customer == "Анна Коваль"

    with pytest.raises(ValueError, match="customer"):
        membership.customer = "   "
    assert membership.customer == "Анна Коваль"


@pytest.mark.parametrize("visits", [0, 1, 12, 10_000])
def test_visits_setter_accepts_zero_and_more(visits: int) -> None:
    """Zero is the boundary value; any whole number from zero up is valid."""
    membership = anna()

    membership.visits = visits

    assert membership.visits == visits


@pytest.mark.parametrize("visits", [-1, 1.5, "12"])
def test_visits_setter_rejects_invalid_values(visits: object) -> None:
    """A negative number, a fraction or text is rejected and the old value stays."""
    membership = anna()

    with pytest.raises(ValueError, match="visits"):
        membership.visits = visits  # type: ignore[assignment]
    assert membership.visits == 12


def test_invalid_visits_are_rejected_by_init() -> None:
    """__init__ uses the setter, so it checks the value too."""
    with pytest.raises(ValueError, match="visits"):
        Plain("Анна Литвин", START, END, -5)


def test_dates_are_read_only() -> None:
    """start and end have no setter."""
    membership = anna()

    with pytest.raises(AttributeError):
        membership.start = END  # type: ignore[misc]
    with pytest.raises(AttributeError):
        membership.end = START  # type: ignore[misc]


@pytest.mark.parametrize("end", [START, date(2026, 10, 1)])
def test_end_must_be_after_start(end: date) -> None:
    """A membership that ends on or before its start day is rejected."""
    with pytest.raises(ValueError, match="end"):
        Plain("Анна Литвин", START, end, 12)


@pytest.mark.parametrize(
    ("start", "end", "months"),
    [
        (date(2026, 10, 5), date(2026, 11, 5), 1),
        (date(2026, 6, 10), date(2026, 12, 10), 6),
        (date(2026, 1, 15), date(2027, 1, 15), 12),
    ],
)
def test_months_between_start_and_end(start: date, end: date, months: int) -> None:
    """months counts whole calendar months, also across a new year."""
    assert Plain("Анна Литвин", start, end, 12).months == months


def test_is_active_on_includes_start_and_excludes_end() -> None:
    """Active from the first day up to the day before the end."""
    membership = anna()

    assert membership.is_active_on(START)
    assert membership.is_active_on(date(2026, 11, 4))
    assert not membership.is_active_on(END)
    assert not membership.is_active_on(date(2026, 10, 4))


def test_str_is_for_people() -> None:
    """str() gives a short readable line."""
    assert str(anna()) == "Анна Литвин: plain, 2026-10-05 to 2026-11-05, 12 visits"


def test_repr_is_for_developers() -> None:
    """repr() shows the class and every value, like the code that creates the object."""
    assert repr(anna()) == (
        "Plain(customer='Анна Литвин', start=datetime.date(2026, 10, 5), "
        "end=datetime.date(2026, 11, 5), visits=12)"
    )


def test_equal_when_type_and_values_are_equal() -> None:
    """Two objects of the same subtype with the same values are equal and have the same hash."""
    first, second = anna(), anna()

    assert first == second
    assert first is not second
    assert hash(first) == hash(second)
    assert len({first, second}) == 1


def test_not_equal_when_a_value_or_the_subtype_differs() -> None:
    """Different visits or another subtype make the objects different."""
    assert anna(12) != anna(13)
    assert anna() != Other("Анна Литвин", START, END, 12)


def test_comparison_with_another_type_is_not_implemented() -> None:
    """__eq__ returns NotImplemented for a foreign type, so == gives False."""
    membership = anna()

    assert membership.__eq__("Анна Литвин") is NotImplemented
    assert membership != {"customer": "Анна Литвин"}
