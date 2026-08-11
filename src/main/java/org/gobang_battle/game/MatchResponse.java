package org.gobang_battle.game;

import lombok.Data;

//websocket的匹配响应
@Data
public class MatchResponse {
    //接收到是不是需要的开始匹配或者结束匹配消息，或者是其他的非法请求
    private boolean ok;
    //匹配失败的原因
    private String reason;
    //接收到的信息是开始匹配还是停止匹配等等
    private String message;
}
