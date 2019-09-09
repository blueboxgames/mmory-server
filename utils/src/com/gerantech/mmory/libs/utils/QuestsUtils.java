package com.gerantech.mmory.libs.utils;

import com.gerantech.mmory.libs.data.SFSDataModel;
import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.core.Player;
import com.gerantech.mmory.core.constants.MessageTypes;
import com.gerantech.mmory.core.others.Quest;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import haxe.root.Array;

import java.sql.SQLException;

/**
 * Created by ManJav on 8/27/2018.
 */
public class QuestsUtils extends UtilBase
{
    public static QuestsUtils getInstance()
    {
        return (QuestsUtils)UtilBase.get(QuestsUtils.class);
    }

    public void insertNewQuests(Player player)
    {
        fill(player);

        if( player.quests.length == 0 )
            return;

        String query = "INSERT INTO quests (player_id, `type`, `key`, step) VALUES ";
        Quest q;
        for(int i = 0; i < player.quests.length; i++ )
        {
            q = player.quests.__get(i);
            query += "(" + player.id + ", " + q.type + ", " + q.key + ", " + q.nextStep + ")";
            query += (i < player.quests.length - 1 ? ", " : ";");
        }
        int idFrom = 0;
        try {
            idFrom = Math.toIntExact((long) ext.getParentZone().getDBManager().executeInsert(query, new Object[]{}));
        } catch (SQLException e) {e.printStackTrace();}

        // insert ids
        for(int i = 0; i < player.quests.length; i++ )
            player.quests.__get(i).id = idFrom + i;
    }

    public ISFSArray getAll(int playerId)
    {
        String query = "SELECT id, `type`, `key`, step FROM quests WHERE player_id = " + playerId + " ORDER BY timestamp";
        ISFSArray quests = new SFSArray();
        try {
            quests = ext.getParentZone().getDBManager().executeQuery(query, new Object[]{});
        } catch (SQLException e) {e.printStackTrace();}
        return quests;
    }

    public void updateAll(Player player, ISFSArray quests)
    {

        ISFSObject q;
        Quest quest;
        player.quests = new Array<Quest>();
        for(int i = 0; i < quests.size(); i++ )
        {
            q = quests.getSFSObject(i);
            quest = new Quest(player, q.getInt("type"), q.getInt("key"), q.getInt("step"));
            quest.id = q.getInt("id");
            player.quests.push(quest);
        }
    }

    public int collectReward(Game game, int questId)
    {
        int questIndex = game.player.getQuestIndexById(questId);
        if( questIndex == -1 )
            return MessageTypes.RESPONSE_NOT_FOUND;
        
        Quest quest = game.player.quests.__get(questIndex);
        quest.current = Quest.getCurrent(game.player, quest.type, quest.key);
        quest.nextStep = Quest.getNextStep(game.player, quest.type, quest.key);
        // trace("old ==> " + quest.toString());

        // exchange
        int response = ExchangeUtils.getInstance().process(game, Quest.getExchangeItem(quest.type, quest.nextStep), 0, 0);
        if( response != MessageTypes.RESPONSE_SUCCEED )
            return response;
        game.player.quests.remove(quest);
        fill(game.player);
        quest = game.player.quests.__get(game.player.quests.length - 1);
        quest.id = questId;
        // trace("new ==> " + quest.toString());

        // update DB
        String query = "UPDATE quests SET `type`=" + quest.type + ", `key`=" + quest.key + ", step=" + quest.nextStep + " WHERE `id`=" + quest.id;
        trace(query);
        try {
            ext.getParentZone().getDBManager().executeUpdate(query, new Object[]{});
        } catch (SQLException e) {e.printStackTrace();}

        return response;
    }

    public void fill(Player player)
    {
        if( player.quests == null )
            player.quests = new Array<Quest>();

        if( player.get_battleswins() < 3 )
            return;

        // initialize first quests
        if( player.quests.length == 0 )
        {
            player.quests.push( Quest.instantiate(player, Quest.TYPE_3_BATTLES) );
            player.quests.push( Quest.instantiate(player, Quest.TYPE_7_CARD_COLLECT) );
            player.quests.push( Quest.instantiate(player, Quest.TYPE_8_CARD_UPGRADE) );
            player.quests.push( Quest.instantiate(player, Quest.TYPE_9_BOOK_OPEN) );
        }

        if( player.quests.length >= Quest.MAX_QUESTS ) // check quests is full
            return;

        // fill quests after a quest acomplished 
        int type = player.quests.length > 0 ? player.quests.__get(player.quests.length - 1).type : -1;
        if( type == Quest.TYPE_9_BOOK_OPEN ) // loop to finding new type 
            type = Quest.TYPE_0_LEVELUP;
        else
            type ++;
        
        while( type < 10 )
        {
            if( type != 2 && player.getQuestIndexByType(type) == -1 ) // not an operation and not exists
            {
                player.quests.push( Quest.instantiate(player, type));
                fill(player);
                return;
            }
            type ++;
        }
    }


    public static ISFSObject toSFS(Quest quest)
    {
        SFSObject ret = new SFSObject();
        ret.putInt("id", quest.id);
        ret.putInt("key", quest.key);
        ret.putInt("type", quest.type);
        ret.putInt("target", quest.target);
        ret.putInt("current", quest.current);
        ret.putInt("nextStep", quest.nextStep);
        ret.putSFSArray("rewards", SFSDataModel.toSFSArray(quest.rewards));
        return ret;
    }

    public static ISFSArray toSFS(Array<Quest> quests)
    {
        ISFSArray ret = new SFSArray();
        for( int i = 0; i < quests.length; i++ )
            ret.addSFSObject( QuestsUtils.toSFS(quests.__get(i)));
        return ret;
    }

}
