package org.gobang_battle.game;

import lombok.Data;

//客户端连接游戏房间之后，服务器返回的响应
@Data
public class GameReadyResponse {
    private String message;//是否是准备就绪
    private boolean ok;
    private String reason;
    private String roomId;
    private int thisUserId;
    private int thatUserId;
    private int whiteUser;//是不是先手白子，0为不是，1为是
}
