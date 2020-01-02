package tel.schich.estimationpoker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import tel.schich.estimationpoker.message.JoinGameMessage;

public class Main {

    private static final String WEBSOCKET_PATH = "/ws";

    public static void main(String[] args) throws InterruptedException, IOException {
        EventLoopGroup bossGroup = new EpollEventLoopGroup(1);
        EventLoopGroup workerGroup = new EpollEventLoopGroup();

        final Path fileBase;
        if (args.length == 0) {
            fileBase = Paths.get("src/main/content");
            if (!Files.exists(fileBase)) {
                Files.createDirectories(fileBase);
            }
        } else {
            fileBase = Paths.get(args[0]);
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));

        Message msg = new JoinGameMessage(1, null, Message.Type.JOIN_GAME);
        String jsonString = mapper.writeValueAsString(msg);
        System.out.println(jsonString);

        Message parsedMessage = mapper.readValue(jsonString, Message.class);
        System.out.println(parsedMessage);

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
            .channel(EpollServerSocketChannel.class)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                protected void initChannel(SocketChannel ch) {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast(new HttpServerCodec());
                    pipeline.addLast(new HttpObjectAggregator(65536));
                    pipeline.addLast(new WebSocketServerCompressionHandler());
                    pipeline.addLast(new WebSocketServerProtocolHandler(WEBSOCKET_PATH, null, true));
                    pipeline.addLast(new WebRequestHandler(fileBase));
                    pipeline.addLast(new WebSocketFrameHandler());
                }
            });

            Channel ch = b.bind(9000).sync().channel();

            ch.closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }

    }

}
