package org.gobang_battle.model;

import lombok.Data;

import java.util.Date;

@Data
public class User {
    private int userId;
    private String username;
    private String password;
    private int score;
    private int totalCount;
    private int winCount;
    private Date createTime;
    private Date updateTime;
}
