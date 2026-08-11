package org.gobang_battle.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.gobang_battle.model.User;

@Mapper
public interface UserMapper {
    //插入用户
    int insertUser(User user);
    //根据用户名查询用户信息
    @Select("select user_id, username, password, score, total_count, win_count, create_time, update_time " +
            "from `user` where username = #{username}")
    User selectByUsername(@Param("username") String username);
    //总比赛场次 + 1， 获胜场次 + 1， 天梯分数 + 100
    void userWin(int userId);
    //总比赛场数 + 1， 获胜场次不变，天梯分数 - 100
    void userLose(int userId);
}
