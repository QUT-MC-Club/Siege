package io.github.restioson.siege.game.active.capturing;

public enum CapturingStateSidebarBlink {
    // Blink between the owning team colour and grey (e.g. contested)
    OWNING_TEAM_TO_GREY,
    // Blink between the owning team colour and capturing team colour
    OWNING_TEAM_TO_CAPTURING,
    // Do not blink
    NO_BLINK
}
