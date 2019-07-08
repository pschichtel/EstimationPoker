package tel.schich.estimationpoker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.CLOSE;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_0;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

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
                    pipeline.addLast(new WebSocketIndexPageHandler(fileBase));
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

    private static final class WebSocketIndexPageHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        private final Path fileBase;

        public WebSocketIndexPageHandler(Path fileBase) {
            this.fileBase = fileBase;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
            // Handle a bad request.
            if (!req.decoderResult().isSuccess()) {
                sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
                return;
            }

            // Allow only GET methods.
            if (!GET.equals(req.method())) {
                sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN));
                return;
            }

            String path = req.uri();
            if (path.equals("/")) {
                path = "/index.html";
            }

            System.out.println("Request for: " + path);


            ByteBuf content = getContentFor(fileBase, path);
            // Send the index page
            if (content != null) {
                FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, content);

                String contentType = "text/plain";
                if (path.endsWith(".html")) {
                    contentType = "text/html";
                } else if (path.endsWith(".js")) {
                    contentType = "text/javascript";
                } else if (path.endsWith(".css")) {
                    contentType = "text/css";
                } else if (path.endsWith(".svg")) {
                    contentType = "image/svg+xml";
                }

                res.headers().set(CONTENT_TYPE, contentType + "; charset=UTF-8");
                HttpUtil.setContentLength(res, content.readableBytes());

                sendHttpResponse(ctx, req, res);
            } else {
                sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND));
            }
        }

        private static ByteBuf getContentFor(Path fileBase, String path) throws IOException {
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            Path fullPath = fileBase.resolve(path);
            if (!Files.exists(fullPath)) {
                return null;
            }
            try (FileChannel contentStream = FileChannel.open(fullPath, StandardOpenOption.READ)) {
                if (contentStream == null) {
                    return null;
                }

                try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    ByteBuffer buf = ByteBuffer.allocate(4096);
                    int bytesRead;
                    while ((bytesRead = contentStream.read(buf)) != -1) {
                        out.write(buf.array(), 0, bytesRead);
                        buf.clear();
                    }
                    return Unpooled.wrappedBuffer(out.toByteArray());
                }
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }

        private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
            // Generate an error page if response getStatus code is not OK (200).
            if (res.status().code() != 200) {
                ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), StandardCharsets.UTF_8);
                res.content().writeBytes(buf);
                buf.release();
                HttpUtil.setContentLength(res, res.content().readableBytes());
            }

            // Send the response and close the connection if necessary.
            if (!HttpUtil.isKeepAlive(req) || res.status().code() != 200) {
                // Tell the client we're going to close the connection.
                res.headers().set(CONNECTION, CLOSE);
                ctx.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
            } else {
                if (req.protocolVersion().equals(HTTP_1_0)) {
                    res.headers().set(CONNECTION, KEEP_ALIVE);
                }
                ctx.writeAndFlush(res);
            }
        }

    }

    private static final class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
        protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame msg) {
            if (msg instanceof TextWebSocketFrame) {
                String text = ((TextWebSocketFrame) msg).text();
                System.out.println(text);
                ctx.writeAndFlush(new TextWebSocketFrame("echo: " + text));
            }
        }
    }
}
