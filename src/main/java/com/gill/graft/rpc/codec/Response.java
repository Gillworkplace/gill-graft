package com.gill.graft.rpc.codec;

/**
 * Response
 *
 * @author gill
 * @version 2023/10/17
 **/
public class Response {

    private byte[] data;

    public Response(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
