package org.gobang_battle.game;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OnlineUserState {
    //用哈希表来保存用户的在线状态，key是用户id，value是websocket会话
    //刚进入游戏大厅，保存当前用户的状态
    private final ConcurrentHashMap<Integer, WebSocketSession> gameHall = new ConcurrentHashMap<>();
    //这是用户进入对战页面，也就是进入游戏房间的时候的用户状态
    private final ConcurrentHashMap<Integer, WebSocketSession> gameRoom = new ConcurrentHashMap<>();
    public void enterGameHall(int userId, WebSocketSession webSocketSession) {
        gameHall.put(userId, webSocketSession);
    }
    //离开游戏大厅，就是离线状态
    public void exitGameHall(int userId) {
        gameHall.remove(userId);
    }
    //通过用户id获取当前websocket会话状态
    public WebSocketSession getFromGameHall(int userId) {
        return gameHall.get(userId);
    }
    //进入对战房间的状态
    public void enterGameRoom(int userId, WebSocketSession webSocketSession) {
        gameRoom.put(userId, webSocketSession);
    }
    //离开游戏房间
    public void exitGameRoom(int userId, WebSocketSession webSocketSession) {
        gameRoom.remove(userId);
    }
    //获取会话状态
    public WebSocketSession getFromGameRoom(int userId) {
        return gameRoom.get(userId);
    }
}
