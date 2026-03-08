from __future__ import annotations

from dataclasses import dataclass
from importlib import import_module
from typing import Any


SDK_MODULE = "extremecraft_sdk.api.sdk"


@dataclass(frozen=True)
class OptionalDependencyStatus:
    name: str
    available: bool
    message: str
    error: str | None = None


class OptionalDependencyUnavailableError(RuntimeError):
    def __init__(self, dependency_name: str, feature: str, message: str) -> None:
        super().__init__(message)
        self.dependency_name = dependency_name
        self.feature = feature


def sdk_status(feature: str = "sdk") -> OptionalDependencyStatus:
    try:
        import_module(SDK_MODULE)
    except Exception as exc:  # noqa: BLE001
        return OptionalDependencyStatus(
            name="extremecraft_sdk",
            available=False,
            message=(
                f"Feature '{feature}' is unavailable because the optional dependency "
                f"'extremecraft_sdk' could not be imported."
            ),
            error=str(exc),
        )
    return OptionalDependencyStatus(
        name="extremecraft_sdk",
        available=True,
        message=f"Feature '{feature}' can use the local SDK integration.",
    )


def load_sdk_class(feature: str) -> type[Any]:
    try:
        module = import_module(SDK_MODULE)
        return module.ExtremeCraftSDK
    except Exception as exc:  # noqa: BLE001
        raise OptionalDependencyUnavailableError(
            "extremecraft_sdk",
            feature,
            (
                f"{feature} is unavailable because the optional dependency 'extremecraft_sdk' "
                f"is missing or failed to import: {exc}"
            ),
        ) from exc
