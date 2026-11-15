"""Gym membership data processing, variant 20."""

from importlib.metadata import PackageNotFoundError, version

try:
    # The version comes from the installed metadata, so it always equals project.version.
    __version__ = version("gym-membership-py")
except PackageNotFoundError:  # the package was not installed, e.g. run straight from src/
    __version__ = "0.0.0+unknown"
