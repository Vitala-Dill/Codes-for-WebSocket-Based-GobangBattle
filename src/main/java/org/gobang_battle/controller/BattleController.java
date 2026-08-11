package org.gobang_battle.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gobang_battle.game.GameReadyResponse;
import org.gobang_battle.game.GameResponse;
import org.gobang_battle.game.OnlineUserState;
import org.gobang_battle.mapper.UserMapper;
import org.gobang_battle.model.Room;
import org.gobang_battle.model.RoomManager;
import org.gobang_battle.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

@Component
public class BattleController extends TextWebSocketHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private RoomManager roomManager;
    @Autowired
    private OnlineUserState onlineUserState;
    @Autowired
    private UserMapper userMapper;
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        GameReadyResponse response = new GameReadyResponse();

        //1. 先获取到用户的身份信息：（从HttpSession中获取用户对象，已经复制到了websocket session中）
        User user = (User) session.getAttributes().get("user");
        if (user == null) {
            response.setOk(false);
            response.setReason("用户尚未登陆!");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
            return;
        }
        //2. 判定当前用户是否已经进入房间，通过房间管理器查询
        Room room = roomManager.getRoomByUserId(user.getUserId());
        if (room == null) {
            //如果为空说明用户还没有匹配到
            response.setOk(false);
            response.setReason("用户尚未匹配成功!");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
            return;
        }
        //3. 判断是不是多开，通过userId获取当前的websocket session状态，不为空说明之前登陆了
        if (onlineUserState.getFromGameRoom(user.getUserId()) != null
                || onlineUserState.getFromGameHall(user.getUserId()) != null) {
            //如果一个游戏账号，一个在游戏大厅，一个在游戏房间也认为是多开
            response.setOk(true);//单独处理多开情况
            response.setReason("禁止多开");
            response.setMessage("repeatConnection");//设置为发送消息
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
            return;
        }
        //4. 设置当前玩家上线
        onlineUserState.enterGameRoom(user.getUserId(), session);
        System.out.println("玩家 " + user.getUsername() + " 进入游戏房间 " + room.getRoomId());
        //5. 把两个玩家加入到游戏房间中
        // 前面创建房间/匹配过程是在game_hall.html里完成的，
        // 当匹配到对手就会跳转到真正的游戏房间game_rom.html
        // 页面跳转有可能会失败
        // 比较准确的做法是，两个玩家进入到game_room.html页面，这才是真正准备好进入房间了
        synchronized (room) {
            if (room.getPlayer1() == null) {
                //说明第一个玩家还没进入房间
                //那就直接把连接上websocket的玩家作为user1，加入房间
                room.setPlayer1(user);
                room.setWhiteUser(user);//那就让第一个进入游戏房间的玩家为先手玩家
                System.out.println("玩家 " + user.getUsername() + " 准备就绪，进入房间中...");
                return;
            }
            if (room.getPlayer2() == null) {
                //说明第一个玩家还没进入房间
                //那就直接把连接上websocket的玩家作为user1，加入房间
                room.setPlayer2(user);
                System.out.println("玩家 " + user.getUsername() + " 准备就绪，进入房间中...");
                //只有在两个玩家都进入到游戏房间，才会返回websocket响应，gameReady
                //通知玩家1
                noticeGameReady(room, room.getPlayer1(), room.getPlayer2());
                //通知玩家2
                noticeGameReady(room, room.getPlayer2(), room.getPlayer1());
                return;
            }
        }
        //6. 此时如果又有其他的玩家尝试连接这个房间，就提示报错
        response.setOk(false);
        response.setReason("当前房间人数已满，请重新匹配进入其他房间!");
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    private void noticeGameReady(Room room, User thisUser, User thatUser) throws IOException {
        GameReadyResponse response = new GameReadyResponse();
        response.setMessage("gameReady");
        response.setOk(true);
        response.setReason("");
        response.setRoomId(room.getRoomId());
        response.setThisUserId(thisUser.getUserId());
        response.setThatUserId(thatUser.getUserId());
        response.setWhiteUser(room.getWhiteUser().getUserId());
        WebSocketSession webSocketSession = onlineUserState.getFromGameRoom(thisUser.getUserId());
        webSocketSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        //1. 从会话中拿到用户身份信息
        User user = (User) session.getAttributes().get("user");
        if (user == null) {
            System.out.println("玩家尚未登陆!");
            return;
        }
        //2. 根据玩家id获取到房间信息
        Room room = roomManager.getRoomByUserId(user.getUserId());
        //3. 通过room对象具体处理请求
        room.moveChess(message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        //1. 从会话中拿到用户信息
        User user = (User) session.getAttributes().get("user");
        if (user == null) {
            //这里仅作简单处理，反正已经下线了
            return;
        }
        //2. 判断用户是否在线，在线就把用户从在线列表删除
        WebSocketSession exitSession = onlineUserState.getFromGameRoom(user.getUserId());
        if (session == exitSession) {
            //说明就是想要正常下线，这里可以避免多开退出登录影响到正常登录部分
            onlineUserState.exitGameRoom(user.getUserId(), session);
        }
        System.out.println("用户：" + user.getUsername() + " 游戏房间连接异常！");

        //连接异常或者关闭连接，通知对手获胜
        noticeThatUserWin(user);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        //1. 从会话中拿到用户信息
        User user = (User) session.getAttributes().get("user");
        if (user == null) {
            //这里仅作简单处理，反正已经下线了
            return;
        }
        //2. 判断用户是否在线，在线就把用户从在线列表删除
        WebSocketSession exitSession = onlineUserState.getFromGameRoom(user.getUserId());
        if (session == exitSession) {
            //说明就是想要正常下线，这里可以避免多开退出登录影响到正常登录部分
            onlineUserState.exitGameRoom(user.getUserId(), session);
        }
        System.out.println("用户：" + user.getUsername() + " 离开对战房间");
        //连接异常或者关闭连接，通知对手获胜
        noticeThatUserWin(user);
    }
    //通知对手获胜，记得更新对手为获胜方的信息
    private void noticeThatUserWin(User user) throws IOException {
        //1. 根据当前玩家，找到玩家所在房间
        Room room = roomManager.getRoomByUserId(user.getUserId());
        if (room == null) {
            //房间被销毁了就不用通知对手了
            System.out.println("当前房间被销毁，无需通知对手！");
            return;
        }

        //2. 根据房间找到对手信息，当前玩家是不是玩家1，是那对手就是玩家2，否则对手就是玩家1
        User thatUser = (user == room.getPlayer1()) ? room.getPlayer2() : room.getPlayer1();
        //3. 找到对手的在线状态
        WebSocketSession session = onlineUserState.getFromGameRoom(thatUser.getUserId());
        if (session == null) {
            //对手也掉线了，无需通知
            System.out.println("对手也掉线了，无需通知!");
            return;
        }
        //4. 构造响应，同时对手获胜了
        GameResponse response = new GameResponse();
        response.setWin(thatUser.getUserId());
        response.setMessage("moveChess");
        response.setUserId(thatUser.getUserId());
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));

        //5. 更新玩家的分数信息，断线通知对手获胜
        int winUserId = thatUser.getUserId();
        int loseUserId = user.getUserId();
        userMapper.userLose(loseUserId);
        userMapper.userWin(winUserId);

        //6. 释放房间
        roomManager.remove(room.getRoomId(), room.getPlayer1().getUserId(), room.getPlayer2().getUserId());
    }
}
