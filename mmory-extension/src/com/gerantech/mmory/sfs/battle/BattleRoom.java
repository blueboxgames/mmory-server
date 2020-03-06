package com.gerantech.mmory.sfs.battle;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.core.InitData;
import com.gerantech.mmory.core.battle.BattleField;
import com.gerantech.mmory.core.battle.Outcome;
import com.gerantech.mmory.core.battle.fieldes.FieldData;
import com.gerantech.mmory.core.battle.units.Card;
import com.gerantech.mmory.core.battle.units.Unit;
import com.gerantech.mmory.core.constants.CardTypes;
import com.gerantech.mmory.core.constants.ExchangeType;
import com.gerantech.mmory.core.constants.MessageTypes;
import com.gerantech.mmory.core.constants.ResourceType;
import com.gerantech.mmory.core.exchanges.ExchangeItem;
import com.gerantech.mmory.core.socials.Challenge;
import com.gerantech.mmory.core.utils.maps.IntIntMap;
import com.gerantech.mmory.libs.BBGRoom;
import com.gerantech.mmory.core.constants.SFSCommands;
import com.gerantech.mmory.libs.data.LobbySFS;
import com.gerantech.mmory.libs.utils.BattleUtils;
import com.gerantech.mmory.libs.utils.DBUtils;
import com.gerantech.mmory.libs.utils.HttpUtils;
import com.gerantech.mmory.libs.utils.LobbyUtils;
import com.gerantech.mmory.libs.utils.RankingUtils;
import com.gerantech.mmory.sfs.battle.bots.BattleBot;
import com.gerantech.mmory.sfs.battle.factories.EndCalculator;
import com.gerantech.mmory.sfs.battle.factories.TouchDownEndCalculator;
import com.gerantech.mmory.sfs.callbacks.BattleEventCallback;
import com.gerantech.mmory.sfs.callbacks.ElixirChangeCallback;
import com.gerantech.mmory.sfs.callbacks.HitUnitCallback;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.api.CreateRoomSettings;
import com.smartfoxserver.v2.entities.SFSRoomRemoveMode;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;

public class BattleRoom extends BBGRoom {
	public BattleField battleField;
	public EndCalculator endCalculator;
	public ScheduledFuture<?> autoJoinTimer;
	
	private BattleBot bot;
	private ScheduledFuture<?> timer;
	private double unitsUpdatedAt;
	private double forceUpdatedAt;
	private List<Integer> reservedUnitIds;
	private BattleEventCallback eventCallback;

	public void init(int id, CreateRoomSettings settings)
	{
		super.init(id, settings);
		this.battleField = new BattleField();
	}

	public void create(Boolean opponentNotFound) {
		if( this.autoJoinTimer != null )
			this.autoJoinTimer.cancel(true);
		this.autoJoinTimer = null;

		List<User> players = getUsersByType(BBGRoom.USER_TYPE_PLAYER);
		this.battleField.singleMode = opponentNotFound || players.size() == 1;

		// reserve player data
		List<Game> games = new ArrayList<>();
		for (User u : players)
			games.add((Game) u.getSession().getProperty("core"));

		if( this.battleField.singleMode )
		{
			InitData data = new InitData();
			data.id = (int) (Math.random() * 9999);
			data.nickName = RankingUtils.getInstance().getRandomName();
			data.resources.set(ResourceType.R2_POINT, 0);
			Game botGame = new Game();
			botGame.init(data);
			games.add(botGame);
		}
		setProperty("games", games);

		// create battle field
		int mode = this.getPropertyAsInt("mode");
		if( !BattleUtils.getInstance().maps.containsKey(mode) )
		BattleUtils.getInstance().maps.put(mode, HttpUtils.post("http://localhost:8080/assets/map-" + mode + ".json", null, false).text);

		FieldData field = new FieldData(mode, BattleUtils.getInstance().maps.get(mode), ((Game)games.get(0)).appVersion);
		this.battleField.create(games.get(0), games.get(1), field, 0, System.currentTimeMillis(), containsProperty("hasExtraTime"), this.getPropertyAsInt("friendlyMode"));

		if( this.battleField.field.mode == Challenge.MODE_1_TOUCHDOWN )
			this.endCalculator = new TouchDownEndCalculator(this);
		else
			this.endCalculator = new EndCalculator(this);
	}

	public void start()
	{
		if( this.battleField.state >= BattleField.STATE_2_STARTED )
			return;
		this.battleField.start(System.currentTimeMillis(), System.currentTimeMillis());
		this.battleField.elixirUpdater.callback = new ElixirChangeCallback(this);
		this.battleField.unitsHitCallback = new HitUnitCallback(this);
		this.eventCallback = new BattleEventCallback(this);
		this.unitsUpdatedAt = battleField.now;

		if( this.battleField.singleMode )
		{
			// sometimes auto start battle
			this.bot = new BattleBot(this);
			this.bot.autoStart = Math.random() > 0.2 && !this.battleField.games.__get(0).player.inTutorial();
		}

		timer = SmartFoxServer.getInstance().getTaskScheduler().scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {

				if( battleField.state < BattleField.STATE_2_STARTED || battleField.state > BattleField.STATE_4_ENDED )
					return;
				try {
					double battleDuration = battleField.getDuration();
					if( battleField.now - unitsUpdatedAt >= 500 )
					{
						updateReservesData();

						if( battleField.singleMode && battleDuration > 4 )
							pokeBot();
						unitsUpdatedAt = battleField.now;
					}
					battleField.update((int) (Instant.now().toEpochMilli() - battleField.now));
					checkEnding(battleDuration);
				}
				catch (Error | Exception e) { e.printStackTrace(); }
			}
		}, 0, BattleField.DELTA_TIME, TimeUnit.MILLISECONDS);
	}

	public void updateReservesData()
	{
		boolean force = battleField.debugMode || battleField.now - forceUpdatedAt >= 3000;
		if( force )
			forceUpdatedAt = battleField.now;
		List<Integer> reservedUnitIds = this.getChangedUnits(force);
		if( reservedUnitIds == null )
			return;

		this.reservedUnitIds = reservedUnitIds;
		ISFSObject units = new SFSObject();
		units.putIntArray("keys", reservedUnitIds);
		
		/**
		 * TEST_FLAG: Code bellow sneds some test information about changed units.
		 */
		if( force )
		{
			List<String> testData = new ArrayList<>();
			for( int k:reservedUnitIds )
			{
				Unit unit = this.battleField.getUnit(k);
				testData.add(unit.id + "," + unit.x + "," + unit.y + "," + unit.health + "," + unit.card.type + "," + unit.side + "," + unit.card.level);
			}
			units.putUtfStringArray("data", testData);
		}
		send(SFSCommands.BATTLE_UNIT_CHANGE, units, this.getUserList());
	}

	private List<Integer> getChangedUnits(boolean force)
	{
		List<Integer> ret = new ArrayList<>();
		for( int i = 0; i < battleField.units.length ; i++ )
			if( battleField.units.__get(i).health > 0 )
				ret.add(battleField.units.__get(i).id);
		
		if( this.reservedUnitIds == null || force )
			return ret;

		return this.reservedUnitIds.equals(ret) ? null : ret;
	}

	// summon unit  =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	public int summonUnit(int side, int type, double x, double y, double time)
	{
		if( this.battleField.state == BattleField.STATE_1_CREATED )
			this.battleField.state = BattleField.STATE_2_STARTED;
		if( this.battleField.state > BattleField.STATE_3_PAUSED )
			return MessageTypes.RESPONSE_NOT_ALLOWED;

		int id = this.battleField.summonUnit(type, side, x, y, time);

		SFSObject params = new SFSObject();
		if( id == MessageTypes.RESPONSE_NOT_ALLOWED )
		{
			params.putDouble("now", this.battleField.now);
			send(SFSCommands.BATTLE_SUMMON, params, getPlayersList().get(side));
			return id;
		}

		if( id > -1 )
		{
			SFSArray units = new SFSArray();

			if( CardTypes.isSpell(type) )
			{
				Card card = this.battleField.games.__get(side).player.cards.get(type);
				units.addSFSObject(getSFSUnit(type, id, side, card.level, x, y));
				params.putDouble("time", time);
				params.putSFSArray("units", units);
				send(SFSCommands.BATTLE_SUMMON, params, getUserList());
				return id;
			}

			Unit unit = this.battleField.getUnit(id);
			for (int i = id; i > id - unit.card.quantity; i--)
			{
				unit = this.battleField.getUnit(i);
				unit.eventCallback = eventCallback;

				units.addSFSObject(getSFSUnit(type, unit.id, side, unit.card.level, unit.x, unit.y));
			}
			params.putDouble("time", time);
			params.putSFSArray("units", units);
			send(SFSCommands.BATTLE_SUMMON, params, getUserList());
		}
		return id;
	}

	private ISFSObject getSFSUnit(int type, int id, int side, int level, double x, double y)
	{
		SFSObject u = new SFSObject();
		u.putInt("t", type);
		u.putInt("i", id);
		u.putInt("s", side);
		u.putInt("l", level);
		u.putDouble("x", x);
		u.putDouble("y", y);
		return u;
	}

	public void sendNewRoundResponse(int winner, int unitId)
	{
		SFSObject params = new SFSObject();
		params.putInt("winner", winner);
		if( this.battleField.field.mode == Challenge.MODE_1_TOUCHDOWN )
		{
			params.putInt("unitId", unitId);
			params.putInt("round", ((TouchDownEndCalculator)endCalculator).round);
		}
		params.putInt("0", endCalculator.scores[0]);
		params.putInt("1", endCalculator.scores[1]);
		send(SFSCommands.BATTLE_NEW_ROUND, params, getUserList());

		if( battleField.singleMode )
		{
			bot.reset();
			bot.chatStarting(1/endCalculator.ratio());
		}
	}


	// stickers =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	public void sendSticker(User sender, ISFSObject params)
	{
		for (User u : getUserList())
		{
			if( battleField.singleMode && sender != null )
				bot.chatAnswering(params);

			if( sender == null || u.getId() != sender.getId() )
				send(SFSCommands.BATTLE_SEND_STICKER, params, u);
		}
	}


	// leave =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	public void leave(User user, boolean retryMode)
	{
		if( isSpectator(user) || !this.battleField.field.isOperation() )
		{
			BattleUtils.getInstance().leave(this, user);
			return;
		}

		this.battleField.state = BattleField.STATE_4_ENDED;
		if( retryMode )
		{
			close();
			BattleUtils.getInstance().remove(this);
			return;
		}
		endCalculator.scores[1] = endCalculator.scores[0] = 0;
		calculateResult();
		close();
		BattleUtils.getInstance().remove(this);
	}

	private void pokeBot()
	{
		/* 
		// Does not let bot to start, bot summon state should not rely on state.
		if( this.battleField.state < BattleField.STATE_1_CREATED || this.battleField.state > BattleField.STATE_4_ENDED )
			return;
		*/
		bot.update();
	}

	private void checkEnding(double battleDuration)
	{
		if( this.battleField.state > BattleField.STATE_2_STARTED || battleDuration < 3 )
			return;

		// this.endCalculator.scoreED32C  333333333333333s[0] = 3;
		boolean haveWinner = endCalculator.check();
		// trace("state:" + this.battleField.state + " haveWinner " + haveWinner + " scores[0]:" + endCalculator.scores[0] + " scores[1]:" + endCalculator.scores[1] );
		if( haveWinner )
			this.end(battleDuration);
		else if( battleDuration > this.battleField.getTime(2) && (this.endCalculator.ratio() != 1 || this.battleField.field.isOperation()) )
			this.end(battleDuration);
		else if( ( battleDuration > this.battleField.getTime(3) && !this.battleField.field.isOperation()) )
			this.end(battleDuration);
		//trace("duration:" + battleDuration, "t2:" + this.battleField.getTime(2), "t3:" + this.battleField.getTime(3), "ratio:" + endCalculator.ratio());
	}

	private void end(double battleDuration)
	{
		this.battleField.state = BattleField.STATE_4_ENDED;
		trace(this.getName(), "ended duration:" + battleDuration, " (" + this.battleField.field.times.toString() + ")");

	    calculateResult();
		close();
	}

	private void calculateResult()
	{
		DBUtils dbUtils = DBUtils.getInstance();
		SFSArray outcomesSFSData = new SFSArray();
		int now = (int) Instant.now().getEpochSecond();

		IntIntMap[] outcomesList = new IntIntMap[battleField.games.length];
		for (int i=0; i < this.battleField.games.length; i++)
		{
			Game game = this.battleField.games.__get(i);

			SFSObject outcomeSFS = new SFSObject();
			outcomeSFS.putInt("id", game.player.id);
			outcomeSFS.putText("name", game.player.nickName);
			outcomeSFS.putInt("score", endCalculator.scores[i]);

			outcomesList[i] = Outcome.get(this.battleField, this.getPropertyAsInt("index"), i, endCalculator.scores[i], endCalculator.scores[i==0?1:0]);
			//trace("i:", i, "scores:"+scores[i], "ratio:"+(float)numBuildings[i] / (float)numBuildings[i==0?1:0] );

			IntIntMap insertMap = new IntIntMap();
			IntIntMap updateMap = new IntIntMap();
			ExchangeItem earnedBook = null;

			int[] ouyKeys = outcomesList[i].keys();
			for ( int r : ouyKeys )
			{
				if( game.player.resources.exists(r) )
					updateMap.set(r, outcomesList[i].get(r));
				else
					insertMap.set(r, outcomesList[i].get(r));
				//trace(game.player.id + ": (", r, outcomesList[i].get(r) , ")" );

				if( game.player.isBot() )
					continue;

				// set battle book outcome
				if( ResourceType.isBook(r) )
				{
					earnedBook = game.exchanger.items.get(outcomesList[i].get(r));
					earnedBook.outcomesStr = r + ":" + game.player.get_arena(0);
					earnedBook.expiredAt = 0;
				}

				// update stars
				if( r == ResourceType.R17_STARS && game.player.get_arena(0) > 0 )
				{
					int res = game.exchanger.collectStars(outcomesList[i].get(r), now);
					ExchangeItem stars = game.exchanger.items.get(ExchangeType.C104_STARS);
					if( res == MessageTypes.RESPONSE_SUCCEED )
						dbUtils.updateExchange(ExchangeType.C104_STARS, game.player.id, stars.expiredAt, stars.numExchanges, "", "");
				}

				outcomeSFS.putInt(r + "", outcomesList[i].get(r));
			}

			outcomesSFSData.addSFSObject(outcomeSFS);

			// update DB
			if( !game.player.isBot() )
			{
				//trace("battle outcomes:", outcomesList[i].toString());
				// increase daily battles
				if( game.player.get_battleswins() > 0 )
				{
					ExchangeItem dailyBattles = game.exchanger.items.get(ExchangeType.C29_DAILY_BATTLES);
					if( dailyBattles == null )
						dailyBattles = new ExchangeItem(ExchangeType.C29_DAILY_BATTLES, 0, 0, "", "");
					dailyBattles.numExchanges ++;
					dbUtils.updateExchange(ExchangeType.C29_DAILY_BATTLES, game.player.id, dailyBattles.expiredAt, dailyBattles.numExchanges, "", "");
				}

				// add rewards
				game.player.addResources(outcomesList[i]);
				if( earnedBook != null )
					dbUtils.updateExchange(earnedBook.type, game.player.id,0, earnedBook.numExchanges, earnedBook.outcomesStr, "");
				dbUtils.updateResources(game.player, updateMap);
				dbUtils.insertResources(game.player, insertMap);
			}
		}

		sendData(outcomesSFSData);
		//updateChallenges(outcomesList, now);
		updateLobbies();
	}

    /*private void updateChallenges(IntIntMap[] outcomesList, int now)
    {
        if( this.battleField.field.isOperation() || this.battleField.friendlyMode > 0 )
            return;

        for (int i=0; i < this.battleField.games.length; i++)
        {
            Game game = this.battleField.games.__get(i);    // update active challenges
            if( !game.player.isBot() && outcomesList[i].get(ResourceType.R2_POINT) > 0 )
            {
                ISFSArray challenges = ChallengeUtils.getInstance().getChallengesOfAttendee(-1, game.player, false);
                for (int c = 0; c < challenges.size(); c++)
                {
                    ChallengeSFS challenge = (ChallengeSFS) challenges.getSFSObject(c);
                    if( challenge.base.getState(now) != Challenge.STATE_1_STARTED || game.inBattleChallengMode != challenge.base.type )
                        continue;
                    ISFSObject attendee = ChallengeUtils.getInstance().getAttendee(game.player.id, challenge);
                    attendee.putInt("point", attendee.getInt("point") + 1);
                    attendee.putInt("updateAt", now);
                    ChallengeUtils.getInstance().scheduleSave(challenge);
                }
            }
        }
    }*/

	private void updateLobbies()
	{
		if( this.battleField.field.isOperation() )
			return;
		LobbySFS lobbySFS;
		for( int i=0; i < this.battleField.games.length; i++ )
		{
			Game game = this.battleField.games.__get(i);
			lobbySFS = LobbyUtils.getInstance().getDataByMember(game.player.id);
			if( lobbySFS == null )
				return;

			int index = LobbyUtils.getInstance().getMemberIndex(lobbySFS, game.player.id);
			int activity = lobbySFS.getMembers().getSFSObject(index).containsKey("ac") ? lobbySFS.getMembers().getSFSObject(index).getInt("ac") : 0;
			lobbySFS.getMembers().getSFSObject(index).putInt("ac", activity + 1);
			LobbyUtils.getInstance().save(lobbySFS, null, null, -1, -1, -1, -1, lobbySFS.getMembersBytes(), null);
		}
	}

	private void sendData(SFSArray outcomesSFSData)
	{
		SFSObject params = new SFSObject();
		params.putSFSArray("outcomes", outcomesSFSData);//trace(outcomesSFSData.getDump());
		for (int i=0; i < getUserList().size(); i++)
				send( SFSCommands.BATTLE_END, params, getUserList().get(i) );
	}

	@Override
	public int getState() {
		return this.battleField.state;
	}

    private void close()
	{
		this.setAutoRemoveMode(SFSRoomRemoveMode.WHEN_EMPTY);
		if( this.battleField.field.isOperation() || this.getUserList().size() == 0 )
			BattleUtils.getInstance().remove(this);

		if( timer != null )
			timer.cancel(true);
		timer = null;

		if( this.battleField != null )
			battleField.dispose();
	}

	public int getPlayerGroup(User user)
	{
		if( user == null )
			return 0;

		if( this.isSpectator(user) )
			return getPlayerGroup(this.getUserByName(user.getVariable("spectatedUser").getStringValue()));

		return this.battleField.getSide(getGame(user).player.id);
	}

	@Override
	public void destroy()
	{
		//clearAllHandlers();
		if( this.battleField.state < BattleField.STATE_5_DISPOSED )
			this.battleField.state = BattleField.STATE_5_DISPOSED;

		trace(this.getName(), "destroyed.");
		BattleUtils.getInstance().removeReferences(this);
		super.destroy();
	}

	public String toString() {
		return String.format("[ Battle: %s, Id: %s, mode: %s, type: %s, friendlyMode: %s, state: %s ]", new Object[] { this.getName(), this.getId(), this.getPropertyAsInt("mode"), this.getPropertyAsInt("type"), this.getPropertyAsInt("friendlyMode"), this.getState() });
	}

}