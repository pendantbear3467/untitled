from __future__ import annotations

from dataclasses import dataclass


@dataclass
class AnimationClip:
    name: str
    duration_ms: int = 3000
    loop: bool = True


class AnimationPlayer:
    def __init__(self) -> None:
        self.clip = AnimationClip(name="idle")
        self.current_ms = 0
        self.playing = True

    def set_clip(self, clip: AnimationClip) -> None:
        self.clip = clip
        self.current_ms = 0

    def tick(self, delta_ms: int) -> float:
        if not self.playing or self.clip.duration_ms <= 0:
            return self.progress

        self.current_ms += delta_ms
        if self.current_ms > self.clip.duration_ms:
            if self.clip.loop:
                self.current_ms %= self.clip.duration_ms
            else:
                self.current_ms = self.clip.duration_ms
        return self.progress

    @property
    def progress(self) -> float:
        if self.clip.duration_ms <= 0:
            return 0.0
        return max(0.0, min(1.0, self.current_ms / self.clip.duration_ms))
