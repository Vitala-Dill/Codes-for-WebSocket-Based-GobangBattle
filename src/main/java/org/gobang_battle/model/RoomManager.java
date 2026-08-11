package org.gobang_battle.model;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

//房间管理类管理多个房间
//也希望有唯一实例
@Component
@Slf4j
public class RoomManager {
    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, String> userIdToRoomId = new ConcurrentHashMap<>();

    //添加新的游戏房间
    public void add(Room room, int userId1, int userId2) {
        rooms.put(room.getRoomId(), room);
        userIdToRoomId.put(userId1, room.getRoomId());
        userIdToRoomId.put(userId2, room.getRoomId());
        log.info("添加新的游戏房间: {}", room.getRoomId());
    }
    //移除游戏房间
    public void remove(String roomId, int userId1, int userId2) {
        rooms.remove(roomId);
        userIdToRoomId.remove(userId1);
        userIdToRoomId.remove(userId2);
        log.info("移除房间号为 {} 的房间", roomId);
    }

    //通过房间号可以查询房间信息
    public Room getRoomByRoomId(String roomId) {
        return rooms.get(roomId);
    }

    //通过玩家id找到房间
    public Room getRoomByUserId(int userId) {
        String roomId = userIdToRoomId.get(userId);
        if (roomId == null) {
            //映射关系不存在，说明玩家还没有匹配到对手进入游戏房间
            log.info("未查询到该玩家所在房间号, userId:{}",userId);
            return null;
        }
        return rooms.get(roomId);
    }
}
