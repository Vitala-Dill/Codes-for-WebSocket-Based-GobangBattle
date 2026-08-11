package org.gobang_battle.game;

import lombok.Data;

//落子响应
@Data
public class GameResponse {
    private String message;
    private int userId;
    private int col;
    private int row;
    private int win;
}
