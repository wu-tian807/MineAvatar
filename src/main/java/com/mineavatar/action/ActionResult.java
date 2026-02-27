package com.mineavatar.action;

import com.google.gson.JsonObject;

import javax.annotation.Nullable;

/**
 * Unified return type for all agent actions.
 * Serializable to JSON for WebSocket, and convertible to human-readable text for commands.
 */
public class ActionResult {

    private final boolean success;
    private final JsonObject data;
    @Nullable private final String error;
    @Nullable private final String message;
    @Nullable private final String hint;

    private ActionResult(boolean success, JsonObject data, @Nullable String error,
                         @Nullable String message, @Nullable String hint) {
        this.success = success;
        this.data = data;
        this.error = error;
        this.message = message;
        this.hint = hint;
    }

    public static ActionResult ok() {
        return new ActionResult(true, new JsonObject(), null, null, null);
    }

    public static ActionResult ok(JsonObject data) {
        return new ActionResult(true, data, null, null, null);
    }

    public static ActionResult ok(String key, String value) {
        JsonObject data = new JsonObject();
        data.addProperty(key, value);
        return new ActionResult(true, data, null, null, null);
    }

    public static ActionResult fail(String error, String message) {
        return new ActionResult(false, new JsonObject(), error, message, null);
    }

    public static ActionResult fail(String error, String message, String hint) {
        return new ActionResult(false, new JsonObject(), error, message, hint);
    }

    public boolean isSuccess() { return success; }
    public JsonObject getData() { return data; }
    @Nullable public String getError() { return error; }
    @Nullable public String getMessage() { return message; }
    @Nullable public String getHint() { return hint; }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("success", success);
        if (success) {
            json.add("data", data);
        } else {
            json.addProperty("error", error);
            json.addProperty("message", message);
            if (hint != null) json.addProperty("hint", hint);
        }
        return json;
    }

    /**
     * Human-readable string for command feedback.
     */
    public String toReadable() {
        if (success) {
            return data.size() == 0 ? "OK" : data.toString();
        }
        String text = error + ": " + message;
        if (hint != null) text += " (" + hint + ")";
        return text;
    }
}
