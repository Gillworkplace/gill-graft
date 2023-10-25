package com.gill.graft;

import cn.hutool.json.JSONUtil;
import com.gill.graft.mock.TestMethod;
import com.gill.graft.model.LogEntry;
import com.gill.graft.proto.Raft;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * BaseTest
 *
 * @author gill
 * @version 2023/08/01
 **/
public abstract class BaseTest {

	protected static <T> void print(List<T> targets) {
		System.out.println();
		for (T target : targets) {
			System.out.printf("%s%n", target);
			printSplit();
		}
		System.out.println();
	}

	protected static void printSplit() {
		System.out.println("=========");
	}

	protected static void sleep(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public static int findFreePort() {
		int port = 0;
		try (ServerSocket socket = new ServerSocket(0)) {
			port = socket.getLocalPort();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return port;
	}

    static void request(int port, Raft.Request request,
                        BiConsumer<ChannelHandlerContext, Raft.Response> responseHandler) {
        NioEventLoopGroup group = new NioEventLoopGroup();
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Bootstrap bs = new Bootstrap();
            bs.group(group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {

                @Override
                protected void initChannel(SocketChannel sc) {
                    ChannelPipeline pl = sc.pipeline();
                    pl.addLast(new ProtobufVarint32FrameDecoder());
                    pl.addLast(new ProtobufDecoder(Raft.Response.getDefaultInstance()));

                    pl.addLast(new ProtobufVarint32LengthFieldPrepender());
                    pl.addLast(new ProtobufEncoder());

                    pl.addLast(new SimpleChannelInboundHandler<Raft.Response>() {

                        @Override
                        protected void channelRead0(ChannelHandlerContext ctx, Raft.Response response) {
                            responseHandler.accept(ctx, response);
                            latch.countDown();
                        }
                    });
                }
            });
            Channel c = bs.connect("127.0.0.1", port).sync().channel();
            c.writeAndFlush(request);
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            group.shutdownGracefully();
        }
    }

    static void stopNodes(List<? extends TestMethod> nodes) {
        for (TestMethod node : nodes) {
            node.stop();
        }
    }

    static <T extends TestMethod> T findLeader(List<T> nodes) {
        Optional<T> leaderOpt = nodes.stream().filter(TestMethod::isLeader).findFirst();
        if (!leaderOpt.isPresent()) {
            Assertions.fail("not find leader");
        }
        return leaderOpt.get();
    }

    static <T extends TestMethod> T findFollower(List<T> nodes) {
        Optional<T> followerOpt = nodes.stream().filter(TestMethod::isFollower).findFirst();
        if (!followerOpt.isPresent()) {
            Assertions.fail("not find follower");
        }
        return followerOpt.get();
    }

    static void waitUtilStable(List<? extends TestMethod> nodes) {
        while (true) {
            Optional<? extends TestMethod> leader = nodes.stream().filter(TestMethod::isLeader).findFirst();
            if (leader.isPresent() && leader.get().isStable()) {
                break;
            }
            sleep(10);
        }
    }

    static void waitUtilStable(List<? extends TestMethod> nodes, long timeout) {
        long time = System.currentTimeMillis();
        while (System.currentTimeMillis() <= time + timeout) {
            Optional<? extends TestMethod> leader = nodes.stream().filter(TestMethod::isLeader).findFirst();
            if (leader.isPresent() && leader.get().isStable()) {
                return;
            }
            sleep(10);
        }
        stopNodes(nodes);
        Assertions.fail("timeout");
    }

    static void assertAllLogs(List<? extends TestMethod> nodes, TestMethod... excludeNodes) {
        Optional<? extends TestMethod> leaderOpt = nodes.stream().filter(TestMethod::isLeader).findFirst();
        if (!leaderOpt.isPresent()) {
            Assertions.fail("not find leader");
        }
        TestMethod leader = leaderOpt.get();
        List<LogEntry> logs = leader.getLog();
        String expected = JSONUtil.toJsonStr(logs);
        Set<Integer> excludeSet = Arrays.stream(excludeNodes).map(TestMethod::getId).collect(Collectors.toSet());
        try {
            for (TestMethod node : nodes) {
                if (excludeSet.contains(node.getId())) {
                    continue;
                }
                Assertions.assertEquals(expected, JSONUtil.toJsonStr(node.getLog()));
            }
        } catch (Throwable e) {
            System.out.println("============ AFTER EXCEPTION ===============");
            stopNodes(nodes);
            throw e;
        }
    }

    static void assertLogs(List<? extends TestMethod> nodes) {
        Optional<? extends TestMethod> leaderOpt = nodes.stream().filter(TestMethod::isLeader).findFirst();
        if (!leaderOpt.isPresent()) {
            Assertions.fail("not find leader");
        }
        TestMethod leader = leaderOpt.get();
        List<LogEntry> logs = leader.getLog();
        String expected = JSONUtil.toJsonStr(logs);
        int cnt = 0;
        try {
            for (TestMethod node : nodes) {
                if (expected.equals(JSONUtil.toJsonStr(node.getLog()))) {
                    cnt++;
                }
            }
            Assertions.assertTrue(cnt > nodes.size() / 2);
        } catch (Throwable e) {
            System.out.println("============ AFTER EXCEPTION ===============");
            stopNodes(nodes);
            throw e;
        }
    }

    static void assertCluster(List<? extends TestMethod> nodes) {
        int leaderCnt = 0;
        int followerCnt = 0;
        int availableCnt = 0;
        for (TestMethod node : nodes) {
            if (node.isLeader()) {
                leaderCnt++;
            }
            if (node.isFollower()) {
                followerCnt++;
            }
            if (node.isUp()) {
                availableCnt++;
            }
        }
        try {
            Assertions.assertEquals(1, leaderCnt, "leader 数目异常");
            Assertions.assertEquals(availableCnt - 1, followerCnt, "follower 数目异常");
        } catch (Throwable e) {
            System.out.println("============ AFTER EXCEPTION ===============");
            stopNodes(nodes);
            throw e;
        }
    }
}
