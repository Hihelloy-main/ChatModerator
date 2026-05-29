package com.Hihelloy.chatmoderator.utils;

public class ModerationResult {

    private final boolean blocked;
    private final String reason;
    private final ViolationType violationType;

    public enum ViolationType {
        NONE,
        WORD_FILTER,
        HATE_SPEECH,
        SEXUAL,
        VIOLENCE,
        SELF_HARM,
        SPAM,
        HARASSMENT,
        ERROR
    }

    private ModerationResult(boolean blocked, String reason, ViolationType violationType) {
        this.blocked = blocked;
        this.reason = reason;
        this.violationType = violationType;
    }

    public static ModerationResult safe() {
        return new ModerationResult(false, "Safe", ViolationType.NONE);
    }

    public static ModerationResult block(String reason) {
        return new ModerationResult(true, reason, ViolationType.WORD_FILTER);
    }

    public static ModerationResult block(String reason, ViolationType type) {
        return new ModerationResult(true, reason, type);
    }

    public static ModerationResult error(String reason) {
        return new ModerationResult(true, reason, ViolationType.ERROR);
    }

    public boolean isBlocked() {
        return blocked;
    }

    public boolean isSafe() {
        return !blocked;
    }

    public String getReason() {
        return reason;
    }

    public ViolationType getViolationType() {
        return violationType;
    }

    @Override
    public String toString() {
        return "ModerationResult{" +
                "blocked=" + blocked +
                ", reason='" + reason + '\'' +
                ", violationType=" + violationType +
                '}';
    }
}

