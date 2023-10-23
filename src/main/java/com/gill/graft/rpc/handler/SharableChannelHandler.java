package com.gill.graft.rpc.handler;

import com.gill.graft.proto.Raft;

import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

/**
 * SharableChannelHandler
 *
 * @author gill
 * @version 2023/10/18
 **/
public class SharableChannelHandler {

	public static final ChannelHandler PROTOBUF_FRAME_ENCODER = new ProtobufVarint32LengthFieldPrepender();

	public static final ChannelHandler PROTOBUF_PROTOCOL_ENCODER = new ProtobufEncoder();

	public static final ChannelHandler PROTOBUF_PROTOCOL_DECODER_REQUEST = new ProtobufDecoder(
			Raft.Request.getDefaultInstance());

	public static final ChannelHandler PROTOBUF_PROTOCOL_DECODER_RESPONSE = new ProtobufDecoder(
			Raft.Response.getDefaultInstance());
}
