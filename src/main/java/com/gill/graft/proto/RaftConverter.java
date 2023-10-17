package com.gill.graft.proto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gill.graft.entity.BaseParam;
import com.gill.graft.entity.Reply;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * RaftConverter
 *
 * @author gill
 * @version 2023/10/16
 **/
public class RaftConverter {

	private static final Logger log = LoggerFactory.getLogger(RaftConverter.class);

	/**
	 * int encode
	 *
	 * @param integer integer
	 * @return byte[]
	 */
	public static byte[] intEncode(int integer) {
		byte[] bs = new byte[4];
		bs[0] = (byte) (integer & 0xff);
		bs[1] = (byte) ((integer >> Byte.SIZE) & 0xff);
		bs[2] = (byte) ((integer >> Byte.SIZE * 2) & 0xff);
		bs[3] = (byte) ((integer >> Byte.SIZE * 3) & 0xff);
		return bs;
	}

	/**
	 * intDecode
	 * 
	 * @param bytes
	 *            bytes
	 * @return ret
	 */
	public static int intDecode(byte[] bytes) {
		int integer = 0;
		for (int i = 0; i < 4 && i < bytes.length; i++) {
			int bits = bytes[i];
			integer |= bits << Byte.SIZE * i;
		}
		return integer;
	}

	/**
	 * buildBaseParam
	 * 
	 * @param param
	 *            param
	 * @return ret
	 */
	public static Raft.BaseParam buildReply(BaseParam param) {
		return Raft.BaseParam.newBuilder().setTerm(param.getTerm()).setNodeId(param.getNodeId()).build();
	}

	/**
	 * buildReply
	 *
	 * @param reply
	 *            reply
	 * @return ret
	 */
	public static Raft.Reply buildReply(Reply reply) {
		return Raft.Reply.newBuilder().setSuccess(reply.isSuccess()).setTerm(reply.getTerm()).build();
	}

	@FunctionalInterface
	public interface ProtocolBufferParser<T> {

		/**
		 * FunctionalInterface
		 *
		 * @param retB
		 *            ret
		 * @return ret
		 * @throws InvalidProtocolBufferException
		 *             ex
		 */
		T parseFrom(byte[] retB) throws InvalidProtocolBufferException;
	}

	/**
	 * parseFrom
	 * 
	 * @param retB
	 *            retB
	 * @param parser
	 *            parser
	 * @param method
	 *            method
	 * @return ret
	 * @param <T>
	 *            T
	 */
	public static <T> T parseFrom(byte[] retB, RaftConverter.ProtocolBufferParser<T> parser, String method) {
		try {
			return parser.parseFrom(retB);
		} catch (InvalidProtocolBufferException e) {
			log.error("{} parsing rpc response failed, bytes: {} e: {}", method, retB, e.getMessage());
		}
		return null;
	}
}
