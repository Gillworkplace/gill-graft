package com.gill.graft.example.intmap;

import java.util.List;

import com.gill.graft.Node;
import com.gill.graft.apis.RaftRpcService;
import com.gill.graft.apis.Server;
import com.gill.graft.apis.empty.EmptyLogStorage;
import com.gill.graft.apis.empty.EmptyMetaStorage;
import com.gill.graft.model.ProposeReply;

/**
 * MapServer
 *
 * @author gill
 * @version 2023/09/07
 **/
public class IntMapServer {

	private final Node node;

	private final IntMapDataStorage dataStorage = new IntMapDataStorage();

	private final IntMapCommandSerializer serializer = new IntMapCommandSerializer();

	public IntMapServer(int id) {
		this.node = Node.newNode(id, new EmptyMetaStorage(), dataStorage, new EmptyLogStorage(), node -> new Server() {

			/**
			 * 是否可以对外提供服务
			 *
			 * @return 是否
			 */
			@Override
			public boolean isReady() {
				return true;
			}

			/**
			 * 启动服务器
			 */
			@Override
			public void start() {

			}

			/**
			 * 停止服务器
			 */
			@Override
			public void stop() {

			}
		});
	}

	/**
	 * 启动
	 * 
	 * @param followers
	 *            节点
	 */
	public void start(List<RaftRpcService> followers) {
		node.start(followers);
	}

	/**
	 * 停止
	 */
	public void stop() {
		node.stop();
	}

	/**
	 * 设置
	 * 
	 * @param key
	 *            key
	 * @param value
	 *            value
	 * @return xid
	 */
	public int set(String key, int value) {
		IntMapCommand command = new IntMapCommand(IntMapCommand.Type.PUT, key, value);
		ProposeReply reply = node.propose(serializer.serialize(command));
		return reply.getIdx();
	}

	/**
	 * get
	 * 
	 * @param key
	 *            key
	 * @return value
	 */
	public Integer get(String key) {
		return get(key, null);
	}

	/**
	 * get
	 * 
	 * @param key
	 *            key
	 * @param readIdx
	 *            readIdx
	 * @return 结果
	 */
	public Integer get(String key, Integer readIdx) {
		if (readIdx != null && !node.checkReadIndex(readIdx)) {
			return -1;
		}
		return dataStorage.get(key);
	}
}
