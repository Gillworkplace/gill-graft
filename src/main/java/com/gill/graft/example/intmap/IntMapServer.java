package com.gill.graft.example.intmap;

import java.util.ArrayList;
import java.util.List;

import com.gill.consensus.raftplus.Node;
import com.gill.consensus.raftplus.apis.EmptyLogStorage;
import com.gill.consensus.raftplus.apis.EmptyMetaStorage;
import com.gill.consensus.raftplus.model.ProposeReply;

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
		this.node = new Node(id, new EmptyMetaStorage(), dataStorage, new EmptyLogStorage());
	}

	/**
	 * 启动
	 * 
	 * @param servers
	 *            节点
	 */
	public void start(List<? extends IntMapServer> servers) {
		List<Node> nodes = new ArrayList<>();
		for (IntMapServer server : servers) {
			nodes.add(server.node);
		}
		node.start(nodes);
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
		IntMapCommand command = IntMapCommand.builder(IntMapCommand.Type.PUT, key).value(value).build();
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
