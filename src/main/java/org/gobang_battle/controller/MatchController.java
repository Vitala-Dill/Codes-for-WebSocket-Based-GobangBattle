package org.gobang_battle.controller;

import org.gobang_battle.game.MatchRequest;
import org.gobang_battle.game.MatchResponse;
import org.gobang_battle.game.Matcher;
import org.gobang_battle.game.OnlineUserState;
import lombok.extern.slf4j.Slf4j;
import org.gobang_battle.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@Slf4j
//这个类是用来处理匹配中的websocket请求
public class MatchController extends TextWebSocketHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();
    //JSON对象=>Java对象：objectMapper.readValue();
    //Java对象=>JSON对象：objectMapper.writeValueAs();
    @Autowired
    private OnlineUserState onlineUserState;
    @Autowired
    private Matcher matcher;
    //记录在线用户、绑定用户id和session
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        //玩家上线，添加上线状态
        //1.获取session属性，之前已经把用户信息复制到了WebSocket Session中
        //另外在处理用户登录逻辑的控制器中，获取了用户对象放到了HttpSession中
        //然后在Websocket配置类中，使用了拦截器复制了HttpSession中的session属性到Websocket Session中
        //但是这里的user可能会出现为null的情况
        //如果用户没有通过HTTP进行登录，直接通过输入游戏大厅网址来访问游戏大厅页面，就会出现为空的情况
        try {
            User user = (User) session.getAttributes().get("user");

            //2.判定当前的session是不是已经是在线状态，如果是就不执行后续逻辑
            WebSocketSession currentSession = onlineUserState.getFromGameHall(user.getUserId());
            if (currentSession != null || onlineUserState.getFromGameRoom(user.getUserId()) != null) {
                //当前用户已经上线登录了
                //告诉客户端不允许重复登陆
                MatchResponse response = new MatchResponse();
                response.setOk(true);
                response.setReason("禁止多开");
                response.setMessage("repeatConnection");
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
                session.close();
                return;
            }

            //3.获取到用户身份信息后，设置玩家的状态为在线状态
            onlineUserState.enterGameHall(user.getUserId(), session);
            System.out.println("玩家 " + user.getUsername() + " 进入游戏大厅!");
        } catch (NullPointerException e) {
            log.error("用户未登录：", e);
            //出现空指针异常，说明用户的身份信息为空，用户未登录
            //把当前用户未登录的错误信息返回
            MatchResponse response = new MatchResponse();
            response.setOk(false);
            response.setReason("您尚未登录，无法进行后续的匹配，请返回登录！");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        }
    }
    //处理客户端发来的文本消息
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("收到websocket信息：{}", message.getPayload());
        try {
            //处理开始匹配请求和处理停止匹配请求
            User user = (User) session.getAttributes().get("user");
            log.info("当前用户：{}", user);

            if (user == null) {
                log.error("用户为 null，无法处理匹配请求");
                MatchResponse response = new MatchResponse();
                response.setOk(false);
                response.setReason("用户未登录");
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
                return;
            }
            //获取客户端给服务器发送的数据
            String payload = message.getPayload();
            //这个是JSON格式的字符串，转化为Java对象
            MatchRequest request = objectMapper.readValue(payload, MatchRequest.class);
            log.info("解析后的请求：{}", request.getMessage());

            MatchResponse response = new MatchResponse();
            if (request.getMessage().equals("startMatch")) {
                log.info("处理开始匹配请求，用户：{}", user.getUsername());
                log.info("用户积分: {}", user.getScore());
                //进入匹配队列
                matcher.add(user);
                log.info("添加进匹配队列完成");
                //进入匹配队列后返回响应给客户端
                response.setOk(true);
                response.setMessage("startMatch");
            } else if (request.getMessage().equals("stopMatch")) {
                log.info("处理停止请求，用户：{}", user.getUsername());
                //退出匹配队列
                matcher.remove(user);
                //移除之后返回响应给客户端
                response.setOk(true);
                response.setMessage("stopMatch");
            } else {
                log.warn("未知的匹配请求: {}", request.getMessage());
                response.setOk(false);
                response.setReason("其他的非法匹配请求");
            }

            // 发送响应
            String responseJson = objectMapper.writeValueAsString(response);
            log.info("发送响应: {}", responseJson);
            session.sendMessage(new TextMessage(responseJson));
        } catch (Exception e) {
            log.error("处理消息时发生异常:", e);
            e.printStackTrace();
        }
    }
    //当发生底层传输错误时触发
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        try {
            //玩家下线，删除玩家上线会话
            User user = (User) session.getAttributes().get("user");
            //获取map中当前存储的有效session
            WebSocketSession currentSession = onlineUserState.getFromGameHall(user.getUserId());
            //这是当前有效的session在关闭，正常下线
            if (currentSession == session) {
                onlineUserState.exitGameHall(user.getUserId());
                log.info("用户 {} 正常下线", user.getUsername());
            } else {//这是被拒绝登录的新连接在关闭，不关闭旧连接
                log.info("用户 {} 重复登录，保存旧连接", user.getUsername());
            }
            //断开连接时记得移除玩家
            matcher.remove(user);
        } catch (NullPointerException e) {
            log.error("用户未登录：", e);
            //出现空指针异常，说明用户的身份信息为空，用户未登录
            //把当前用户未登录的错误信息返回
//            MatchResponse response = new MatchResponse();
//            response.setOk(false);
//            response.setReason("您尚未登录，无法进行后续的匹配，请返回登录！");
//            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        }
    }
    //处理连接关闭或者异常关闭的过期session
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        try {
            //玩家下线，删除玩家上线会话
            User user = (User) session.getAttributes().get("user");
            //获取map中当前存储的有效session
            WebSocketSession currentSession = onlineUserState.getFromGameHall(user.getUserId());
            //这是当前有效的session在关闭，正常下线
            if (currentSession == session) {
                onlineUserState.exitGameHall(user.getUserId());
                log.info("用户 {} 正常下线", user.getUserId());
            } else {//这是被拒绝登录的新连接在关闭，不关闭旧连接
                log.info("用户 {} 重复登录，保存旧连接", user.getUserId());
            }
            //断开连接时记得移除玩家
            matcher.remove(user);
        } catch (NullPointerException e) {
            log.error("用户未登录：", e);
            //出现空指针异常，说明用户的身份信息为空，用户未登录
            //把当前用户未登录的错误信息返回
//            MatchResponse response = new MatchResponse();
//            response.setOk(false);
//            response.setReason("您尚未登录，无法进行后续的匹配，请返回登录！");
//            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        }
    }
}
