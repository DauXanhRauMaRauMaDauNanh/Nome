package com.comphenix.protocol;

public class PacketType {
    public static class Play {
        public static class Client {
            public static final int POSITION = 0x14;
            public static final int POSITION_LOOK = 0x16;
            public static final int LOOK = 0x17;
            public static final int FLYING = 0x18;
            public static final int BLOCK_DIG = 0x1E;
            public static final int BLOCK_PLACE = 0x35;
            public static final int USE_ENTITY = 0x1B;
            public static final int CHAT = 0x06;
            public static final int CHAT_COMMAND = 0x05;
            public static final int KEEP_ALIVE = 0x15;
            public static final int TRANSACTION = 0x00;
        }

        public static class Server {
            public static final int MAP_CHUNK = 0x27;
            public static final int BLOCK_CHANGE = 0x09;
            public static final int POSITION = 0x40;
            public static final int CHAT = 0x69;
            public static final int KEEP_ALIVE = 0x26;
            public static final int SET_SLOT = 0x14;
            public static final int NAMED_ENTITY_SPAWN = 0x01;
            public static final int ENTITY_DESTROY = 0x3D;
        }
    }
}
