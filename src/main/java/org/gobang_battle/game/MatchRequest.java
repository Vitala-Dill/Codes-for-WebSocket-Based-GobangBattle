package org.gobang_battle.game;

import lombok.Data;

//websocket的匹配请求
@Data
public class MatchRequest {
    //匹配请求就是开始匹配/停止匹配
    private String message = "";
}
