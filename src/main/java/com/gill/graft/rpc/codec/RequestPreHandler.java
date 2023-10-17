package com.gill.graft.rpc.codec;

import com.gill.graft.proto.Raft;
import com.gill.graft.rpc.ConnectionDock;
import com.google.protobuf.ByteString;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RequestCodec
 *
 * @author gill
 * @version 2023/10/17
 **/
public class RequestPreHandler extends MessageToMessageEncoder<Request> {

    private final AtomicLong reqGen = new AtomicLong(0);

    private final ConnectionDock dock;

    public RequestPreHandler(ConnectionDock dock) {
        this.dock = dock;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Request request, List<Object> out) throws Exception {
        long requestId = reqGen.incrementAndGet();
        CompletableFuture<Response> cf = request.getCf();
        dock.add(requestId, cf);
        Raft.Request wrapRequest = Raft.Request.newBuilder().setRequestId(requestId).setServiceId(request.getServiceId())
                .setData(ByteString.copyFrom(request.getData())).build();
        out.add(wrapRequest);
    }
}
