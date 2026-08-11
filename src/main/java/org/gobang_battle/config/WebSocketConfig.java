package org.gobang_battle.config;

import org.gobang_battle.controller.BattleController;
import org.gobang_battle.controller.MatchController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Autowired
    private MatchController matchController;
    @Autowired
    private BattleController battleController;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(matchController, "/match")//添加websocket的访问路径
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .setAllowedOrigins("*");//允许一切来源
        registry.addHandler(battleController, "/battle")//添加websocket的访问路径
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .setAllowedOrigins("*");
    }
}
