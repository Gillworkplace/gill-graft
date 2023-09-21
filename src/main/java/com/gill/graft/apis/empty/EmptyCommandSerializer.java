package com.gill.graft.apis.empty;

import com.gill.graft.apis.CommandSerializer;

/**
 * EmptyCommandSerializer
 *
 * @author gill
 * @version 2023/09/21
 **/
public class EmptyCommandSerializer implements CommandSerializer<String> {

    public static final EmptyCommandSerializer INSTANCE = new EmptyCommandSerializer();

    @Override
    public String serialize(String command) {
        return command;
    }

    @Override
    public String deserialize(String commandStr) {
        return commandStr;
    }
}
