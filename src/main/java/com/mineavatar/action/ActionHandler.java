package com.mineavatar.action;

import com.google.gson.JsonObject;

@FunctionalInterface
public interface ActionHandler {
    ActionResult execute(ActionContext ctx, JsonObject params);
}
