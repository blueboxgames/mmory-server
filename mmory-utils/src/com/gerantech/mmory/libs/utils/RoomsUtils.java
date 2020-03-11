package com.gerantech.mmory.libs.utils;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.gerantech.mmory.libs.BBGRoom;
import com.smartfoxserver.v2.api.CreateRoomSettings;
import com.smartfoxserver.v2.entities.SFSRoomRemoveMode;
import com.smartfoxserver.v2.entities.User;

/**
 * RoomsUtils Created by ManJav on 3/11/2020.
 */
public class RoomsUtils extends UtilBase {

  public static RoomsUtils getInstance() {
    return (RoomsUtils) UtilBase.get(RoomsUtils.class);
  }

  private int roomId = -1;
  protected ConcurrentHashMap<Integer, BBGRoom> rooms = new ConcurrentHashMap<>();

  public AbstractMap<Integer, BBGRoom> getRooms() {
    return this.rooms;
  }

  public BBGRoom getRoom(int id) {
    return this.rooms.containsKey(id) ? this.rooms.get(id) : null;
  }

  public List<BBGRoom> getGroup(String groupId) {
    List<BBGRoom> ret = new ArrayList<>();
    Set<Map.Entry<Integer, BBGRoom>> entries = this.rooms.entrySet();
    for (Map.Entry<Integer, BBGRoom> entry : entries)
      if (entry.getValue().getGroupId().equals(groupId))
        ret.add(entry.getValue());
    return ret;
  }

  public BBGRoom findByName(String name) {
    Set<Map.Entry<Integer, BBGRoom>> entries = this.rooms.entrySet();
    for (Map.Entry<Integer, BBGRoom> entry : entries)
      if (entry.getValue().getName().equals(name))
        return entry.getValue();
    return null;
  }

  public BBGRoom findByUser(User user) {
    Set<Map.Entry<Integer, BBGRoom>> entries = this.rooms.entrySet();
    for (Map.Entry<Integer, BBGRoom> entry : entries)
      if (entry.getValue().containsUser(user))
        return entry.getValue();
    return null;
  }

  public BBGRoom make(Class<?> roomClass, User owner, CreateRoomSettings settings) {
    BBGRoom newRoom = null;
    try {
      newRoom = (BBGRoom) roomClass.newInstance();
    } catch (Exception e) {
      e.printStackTrace();
    }
    newRoom.init(++roomId, settings);
    newRoom.setOwner(owner);
    this.rooms.put(newRoom.getId(), newRoom);
    ext.getLogger().info(String.format("BBGRoom created: %s, %s, type = %s",
        new Object[] { newRoom.getZone().toString(), newRoom.toString(), newRoom.getClass().getSimpleName() }));
    return newRoom;
  }

  /**
   * join users in room
   * 
   * @param room
   * @param user
   */
  public void join(BBGRoom room, User user) {
    this.join(room, user, -1);
  }

  /**
   * join users in battle room
   * 
   * @param room
   * @param user
   * @param spectatingUser
   */
  public void join(BBGRoom room, User user, int spectatingUser) {
    room.addUser(user, spectatingUser > -1 ? BBGRoom.USER_TYPE_SPECTATOR : BBGRoom.USER_TYPE_PLAYER);
    ext.getLogger().info(String.format("BBGRoom joined: %s, %s, spectatingUser = %s",
        new Object[] { room.toString(), user.toString(), spectatingUser }));
  }

  public void leave(BBGRoom room, User user) {
    room.leave(user);
    ext.getLogger().info(String.format("User %s exit from %s", new Object[] { user.toString(), room.getName() }));
    if (room.getUserList().size() == 0 && room.getAutoRemoveMode() == SFSRoomRemoveMode.WHEN_EMPTY)
      this.remove(room);
  }

  /**
   * Kick all users and remove room
   * 
   * @param room
   */
  public void remove(BBGRoom room) {
    if (room.getUserList().size() > 0) {
      room.setAutoRemoveMode(SFSRoomRemoveMode.WHEN_EMPTY);
      List<User> users = room.getUsersByType(-1);
      for (User u : users)
        this.leave(room, u);
    } else {
      room.destroy();
      this.removeReferences(room);
      rooms.remove(room.getId());
      ext.getLogger().info(String.format("BBGRoom removed: %s, %s, num remaining battles = %s",
          new Object[] { room.getZone().toString(), room.toString(), rooms.size() }));
    }
  }

  protected void removeReferences(BBGRoom room) {
  }
}