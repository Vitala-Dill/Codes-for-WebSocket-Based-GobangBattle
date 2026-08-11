package org.gobang_battle.game;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.gobang_battle.model.Room;
import org.gobang_battle.model.RoomManager;
import org.gobang_battle.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

//匹配器，负责完成整个匹配功能
@Component
@Slf4j
public class Matcher {
    //创建三个匹配队列：青铜、白银、黄金
    private final Queue<User> bronzeQueue = new LinkedList<>();
    private final Queue<User> silverQueue = new LinkedList<>();
    private final Queue<User> goldQueue = new LinkedList<>();

    @Autowired
    private OnlineUserState onlineUserState;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private RoomManager roomManager;

    //操作匹配队列的方法
    //玩家进入到匹配队列
    public void add(User user) {
        log.info("调用add()方法，添加用户进匹配队列中");
        log.info("用户: {}, 积分: {}", user.getUsername(), user.getScore());
        try {
            if (user.getScore() < 2000) {
                synchronized (bronzeQueue) {
                    bronzeQueue.offer(user);
                    bronzeQueue.notify();
                    log.info("青铜队列唤醒中");
                }
                System.out.println("玩家 " + user.getUsername() + " 加入到了 bronzeQueue 中");
                log.info("玩家 {} 已加入青铜队列", user.getUsername());
            } else if (user.getScore() < 3000) {
                synchronized (silverQueue) {
                    silverQueue.offer(user);
                    silverQueue.notify();
                    log.info("白银队列唤醒中");
                }
                System.out.println("玩家 " + user.getUsername() + " 加入到了 silverQueue 中");
                log.info("玩家 {} 已加入白银队列", user.getUsername());
            } else {
                synchronized (goldQueue) {
                    goldQueue.offer(user);
                    goldQueue.notify();
                    log.info("黄金队列唤醒中");
                }
                System.out.println("玩家 " + user.getUsername() + " 加入到了 goldQueue 中");
                log.info("玩家 {} 已加入黄金队列", user.getUsername());
            }
        } catch (Exception e) {
            log.error("Matcher.add() 发生异常:", e);
            e.printStackTrace();
        }
    }

    //玩家退出匹配队列
    public void remove(User user) {
        if (user.getScore() < 2000) {
            synchronized (bronzeQueue) {
                bronzeQueue.remove(user);
            }
            System.out.println("玩家 " + user.getUsername() + " 从bronzeQueue中移除");
        } else if (user.getScore() < 3000) {
            synchronized (silverQueue) {
                silverQueue.remove(user);
            }
            System.out.println("玩家 " + user.getUsername() + " 从silverQueue中移除");
        } else {
            goldQueue.remove(user);
            System.out.println("玩家 " + user.getUsername() + " 从goldQueue中移除");
        }
    }

    public Matcher() {
        //创建三个线程，分别对这三个匹配队列进行操作
        Thread t1 = new Thread(() -> {
            //扫描青铜队列
            while (true) {
                handlerMatch(bronzeQueue);
            }
        });
        t1.start();
        Thread t2 = new Thread() {
            @Override
            public void run() {
                while (true) {
                    handlerMatch(silverQueue);
                }
            }
        };
        t2.start();
        Thread t3 = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    handlerMatch(goldQueue);
                }
            }
        });
        t3.start();
    }

    private void handlerMatch(Queue<User> matchQueue) {
        synchronized (matchQueue) {
            try {
                //1. 判断队列中的元素是否达到了2以上
                //队列的初始情况可能是空的，如果往队列中添加一个元素仍然无法进行后续的匹配
                //所以采用循环不断地判断当前队列中的元素个数是否达到要求
                while (matchQueue.size() < 2) {
                    //解决忙等
                    matchQueue.wait();
                    User user = matchQueue.peek();
                    assert user != null;
                    log.info("玩家 {} 进入到等待队列", user.getUsername());
                }
                //2. 从队列中取出两个玩家
                User player1 = matchQueue.poll();
                User player2 = matchQueue.poll();
                assert player2 != null;
                assert player1 != null;
                System.out.println("匹配出两个玩家：" + player1.getUsername() + ", " + player2.getUsername());
                //3. 获取到玩家的websocket会话
                //目的是为了告诉玩家是否匹配到了
                WebSocketSession session1 = onlineUserState.getFromGameHall(player1.getUserId());
                WebSocketSession session2 = onlineUserState.getFromGameHall(player2.getUserId());
                //双重校验
                if (session1 == null) {
                    //玩家1如果离线了，就让玩家2重新放会队列
                    matchQueue.offer(player2);
                    return;
                }
                if (session2 == null) {
                    matchQueue.offer(player1);
                    return;
                }
                //万一匹配到了同一个用户，这里进行了双重校验
                if (session1 == session2) {
                    matchQueue.offer(player1);
                    return;
                }
                //4. 把两个玩家放到对战房间中
                Room room = new Room();
                roomManager.add(room, player1.getUserId(), player2.getUserId());
                //5. 给玩家反馈信息：匹配到了
                MatchResponse response1 = new MatchResponse();
                response1.setOk(true);
                response1.setMessage("matchSuccess");
                String json1 = objectMapper.writeValueAsString(response1);
                session1.sendMessage(new TextMessage(json1));

                MatchResponse response2 = new MatchResponse();
                response2.setOk(true);
                response2.setMessage("matchSuccess");
                String json2 = objectMapper.writeValueAsString(response2);
                session2.sendMessage(new TextMessage(json2));
            } catch (IOException | InterruptedException e) {
                log.error("出现异常：", e);
            }
        }
    }
}
