package org.gobang_battle.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.gobang_battle.GobangBattleApplication;
import org.gobang_battle.game.GameRequest;
import org.gobang_battle.game.GameResponse;
import org.gobang_battle.game.OnlineUserState;
import org.gobang_battle.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.UUID;

@Data
//这里直接加@Component，Room就变成了单例，但是我们这里是服务器管理很多的游戏房间
public class Room {
    //这里使用字符串表示roomId，方便生成唯一的roomId，使用UUID
    private String roomId;
    private User player1;
    private User player2;
    private User whiteUser;//哪个玩家是先手，执白子

    private static final int MAX_ROW = 15;
    private static final int MAX_COL = 15;
    private ObjectMapper objectMapper = new ObjectMapper();
    private OnlineUserState userState;
    private RoomManager roomManager;
    private UserMapper userMapper;
    //添加一个棋盘
    //1）使用0表示当前位置未落子，默认的时候为0空棋盘
    //2）使用1表示user1的落子位置
    //3）使用2表示user2的落子位置
    private int[][]  board = new int[MAX_ROW][MAX_COL];
    public void moveChess(String requestJson) throws IOException {
        //1. 解析请求，得到落子的具体位置
        GameRequest request = objectMapper.readValue(requestJson, GameRequest.class);
        GameResponse response = new GameResponse();

        //2. 判定当前落子的是玩家1还是玩家2
        int chess = request.getUserId() == player1.getUserId() ? 1 : 2;
        int row = request.getRow();
        int col = request.getCol();
        if (board[row][col] != 0) {
            System.out.println("当前位置(" + row + "," + col + ")已经有子了");
            return;
        }
        board[row][col] = chess;

        //3. 打印棋盘
        printBoard();

        //4. 判定当前是否能分出胜负
        int winner = chessWinner(row, col, chess);

        //5. 返回响应，给房间中的所有客户端都返回
        response.setMessage("moveChess");
        response.setUserId(request.getUserId());
        response.setRow(row);
        response.setCol(col);
        response.setWin(winner);

        //6. 通过websocket发送响应
        WebSocketSession session1 = userState.getFromGameRoom(player1.getUserId());
        WebSocketSession session2 = userState.getFromGameRoom(player2.getUserId());
        //如果其中有玩家下线了，直接判定对手获胜
        if (session1 == null) {
            response.setWin(player2.getUserId());
            System.out.println("玩家1 掉线！");
        } else if (session2 == null) {
            response.setWin(player1.getUserId());
            System.out.println("玩家2 掉线！");
        }
        //把响应对象构造成JSON字符串
        String responseJSON = objectMapper.writeValueAsString(response);
        if (session1 != null) {
            session1.sendMessage(new TextMessage(responseJSON));
        }
        if (session2 != null) {
            session2.sendMessage(new TextMessage(responseJSON));
        }
        //7. 如果胜负已分，就可以销毁房间了
        if (response.getWin() != 0) {
            System.out.println("游戏结束，房间即将销毁!roomId = " + roomId + "获胜方为: " + response.getWin());
            //更改获胜方和失败方的信息
            int winUserId = response.getWin();
            int loseUserId = response.getWin() == player1.getUserId() ? player2.getUserId() : player1.getUserId();
            userMapper.userWin(winUserId);
            userMapper.userLose(loseUserId);
            //销毁房间
            roomManager.remove(roomId, player1.getUserId(), player2.getUserId());
        }
    }

    private void printBoard() {
        //在实际上线的时候，游戏房间很多不能都打印在一起
        //更好的做法是，应该给每个房间的信息都放到单独的日志文件中进行打印
        System.out.println("[打印棋盘信息] " + roomId);
        System.out.println("=============================================");
        for (int r = 0; r < MAX_ROW; r++) {
            for (int c = 0; c < MAX_COL; c++) {
                System.out.print(board[r][c] + " ");
            }
            System.out.println();
        }
        System.out.println("=============================================");
    }
    //判定胜负
    private int chessWinner(int row, int col, int chess) {
        //1. 检查所有的行
        // 遍历5种情况
        for (int c = col - 4; c <= col; c++) {
            try {//处理数组越界
                //每次循环， 就是其中一种情况，(row, c)就是最左侧棋子的坐标
                if (board[row][c] == chess && board[row][c + 1] == chess
                        && board[row][c + 2] == chess && board[row][c + 3] == chess
                        && board[row][c + 4] == chess) {
                    //构成五子连珠
                    return chess == 1 ? player1.getUserId() : player2.getUserId();
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                //当出现数组越界，就忽略
                continue;
            }
        }

        //2. 检查所有列
        for (int r = row - 4; r  <=  row; r++) {
            try {
                if (board[r][col] == chess && board[r + 1][col] == chess
                        && board[r + 2][col] == chess && board[r + 3][col] == chess
                        && board[r + 4][col] == chess) {
                    //构成五子连珠
                    return chess == 1 ? player1.getUserId() : player2.getUserId();
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                continue;
            }
        }

        //3. 检查左对角线
        for (int r = row - 4, c = col - 4; r <= row && c <= col ; r++, c++) {
            try {
                if (board[r][c] == chess && board[r + 1][c + 1] == chess
                        && board[r + 2][c + 2] == chess && board[r + 3][r + 3] == chess
                        && board[r + 4][c + 4] == chess) {
                    //构成五子连珠
                    return chess == 1 ? player1.getUserId() : player2.getUserId();
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                continue;
            }
        }

        //4. 检查右对角线
        for (int r = row - 4, c = col + 4; r <= row && c >= col; r++, c--) {
            try {
                if (board[r][c] == chess && board[r + 1][c - 1] == chess
                        && board[r + 2][c - 2] == chess && board[r + 3][c - 3] == chess
                        && board[r + 4][c - 4] == chess) {
                    //构成五子连珠
                    return chess == 1 ? player1.getUserId() : player2.getUserId();
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                continue;
            }
        }
        //5. 胜负未分，就直接返回0
        return 0;
    }

    public Room() {
        //使用UUID生成房间id
        //UUID其实是16进制的数字
        roomId = UUID.randomUUID().toString();

        //通过入口类的context成员，手动获取roomManager和onlineUserState
        userState = GobangBattleApplication.context.getBean(OnlineUserState.class);
        roomManager = GobangBattleApplication.context.getBean(RoomManager.class);
        userMapper = GobangBattleApplication.context.getBean(UserMapper.class);
    }
}
