package com.mineavatar.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mineavatar.MineAvatar;
import com.mineavatar.MineAvatarConfig;
import com.mineavatar.action.ActionContext;
import com.mineavatar.action.ActionRegistry;
import com.mineavatar.action.ActionResult;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;

/**
 * Handles JSON-RPC 2.0 messages over length-prefixed TCP.
 * Transport-agnostic: receives decoded String messages from the Netty pipeline,
 * writes String responses back (pipeline handles length-prefix encoding).
 */
public class JsonRpcHandler extends SimpleChannelInboundHandler<String> {

    private final MinecraftServer server;
    private final ActionRegistry registry;
    private boolean authenticated = false;

    public JsonRpcHandler(MinecraftServer server, ActionRegistry registry) {
        this.server = server;
        this.registry = registry;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String text) {
        JsonObject request;
        try {
            request = JsonParser.parseString(text).getAsJsonObject();
        } catch (JsonSyntaxException | IllegalStateException e) {
            ctx.writeAndFlush(errorResponse(null, -32700, "Parse error: " + e.getMessage()));
            return;
        }

        JsonElement idElem = request.get("id");
        String id = idElem != null && !idElem.isJsonNull() ? idElem.getAsString() : null;
        String method = request.has("method") ? request.get("method").getAsString() : null;
        JsonObject params = request.has("params") && request.get("params").isJsonObject()
                ? request.getAsJsonObject("params")
                : new JsonObject();

        if (method == null) {
            ctx.writeAndFlush(errorResponse(id, -32600, "Missing 'method' field"));
            return;
        }

        if ("auth".equals(method)) {
            handleAuth(ctx, id, params);
            return;
        }

        if (!authenticated) {
            ctx.writeAndFlush(errorResponse(id, -32000, "Not authenticated. Send 'auth' first."));
            return;
        }

        server.execute(() -> {
            ActionContext actionCtx = new ActionContext(server);
            ActionResult result = registry.dispatch(method, actionCtx, params);

            JsonObject response = new JsonObject();
            response.addProperty("jsonrpc", "2.0");
            if (id != null) response.addProperty("id", id);
            response.add("result", result.toJson());

            ctx.writeAndFlush(response.toString());
        });
    }

    private void handleAuth(ChannelHandlerContext ctx, @Nullable String id, JsonObject params) {
        String token = params.has("token") ? params.get("token").getAsString() : "";
        String expectedToken = MineAvatarConfig.WS_TOKEN.get();

        if (!expectedToken.equals(token)) {
            ctx.writeAndFlush(errorResponse(id, -32001, "Invalid token"));
            return;
        }

        authenticated = true;
        MineAvatar.LOGGER.info("[TCP] Client authenticated from {}", ctx.channel().remoteAddress());

        JsonObject result = new JsonObject();
        result.addProperty("success", true);

        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        if (id != null) response.addProperty("id", id);
        response.add("result", result);

        ctx.writeAndFlush(response.toString());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        MineAvatar.LOGGER.info("[TCP] Client connected: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        MineAvatar.LOGGER.info("[TCP] Client disconnected: {}", ctx.channel().remoteAddress());
        authenticated = false;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        MineAvatar.LOGGER.error("[TCP] Handler exception from {}", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }

    private static String errorResponse(@Nullable String id, int code, String message) {
        JsonObject error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("message", message);

        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        if (id != null) response.addProperty("id", id);
        response.add("error", error);
        return response.toString();
    }
}
