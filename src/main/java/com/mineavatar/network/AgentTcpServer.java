package com.mineavatar.network;

import com.mineavatar.MineAvatar;
import com.mineavatar.action.ActionRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import net.minecraft.server.MinecraftServer;

import java.nio.charset.StandardCharsets;

/**
 * TCP server using only Minecraft's bundled Netty core modules.
 * Protocol: 4-byte big-endian length prefix + UTF-8 JSON payload.
 *
 * No external dependencies — all classes come from netty-codec and netty-transport
 * which are guaranteed to be in Minecraft's classpath.
 */
public class AgentTcpServer {

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private final int port;

    public AgentTcpServer(int port) {
        this.port = port;
    }

    public void start(MinecraftServer server, ActionRegistry registry) {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(2);

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    // Inbound: read 4-byte length prefix, then extract frame
                                    .addLast(new LengthFieldBasedFrameDecoder(
                                            1048576,  // max frame size: 1 MB
                                            0,        // length field offset
                                            4,        // length field size (bytes)
                                            0,        // length adjustment
                                            4         // bytes to strip (remove the length prefix itself)
                                    ))
                                    .addLast(new StringDecoder(StandardCharsets.UTF_8))
                                    // Outbound: prepend 4-byte length prefix
                                    .addLast(new LengthFieldPrepender(4))
                                    .addLast(new StringEncoder(StandardCharsets.UTF_8))
                                    // Application logic
                                    .addLast(new JsonRpcHandler(server, registry));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 8)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            serverChannel = bootstrap.bind(port).sync().channel();
            MineAvatar.LOGGER.info("[TCP] Agent TCP server started on port {}", port);
        } catch (InterruptedException e) {
            MineAvatar.LOGGER.error("[TCP] Failed to start TCP server on port {}", port, e);
            Thread.currentThread().interrupt();
            stop();
        } catch (Exception e) {
            MineAvatar.LOGGER.error("[TCP] Failed to start TCP server on port {}", port, e);
            stop();
        }
    }

    public void stop() {
        if (serverChannel != null) {
            try {
                serverChannel.close().sync();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            serverChannel = null;
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
            bossGroup = null;
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            workerGroup = null;
        }
        MineAvatar.LOGGER.info("[TCP] Agent TCP server stopped");
    }
}
