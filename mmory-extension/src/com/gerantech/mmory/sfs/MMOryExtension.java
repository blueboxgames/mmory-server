package com.gerantech.mmory.sfs;

import java.time.Instant;

import com.gerantech.mmory.core.constants.SFSCommands;
import com.gerantech.mmory.libs.utils.DBUtils;
import com.gerantech.mmory.libs.utils.LobbyUtils;
import com.gerantech.mmory.libs.utils.UtilBase;
import com.gerantech.mmory.sfs.administration.PlayersGetHandler;
import com.gerantech.mmory.sfs.administration.RestoreHandler;
import com.gerantech.mmory.sfs.administration.SearchInChatsHandler;
import com.gerantech.mmory.sfs.administration.SpectateRequestJoinHandler;
import com.gerantech.mmory.sfs.administration.SpectateRequestLeaveHandler;
import com.gerantech.mmory.sfs.administration.ban.BanHandler;
import com.gerantech.mmory.sfs.administration.ban.GetBannedDataHandler;
import com.gerantech.mmory.sfs.administration.ban.GetOffenderDataHandler;
import com.gerantech.mmory.sfs.administration.ban.InfractionsDeleteHandler;
import com.gerantech.mmory.sfs.administration.ban.InfractionsGetHandler;
import com.gerantech.mmory.sfs.administration.issues.IssueGetHandler;
import com.gerantech.mmory.sfs.administration.issues.IssueReportHandler;
import com.gerantech.mmory.sfs.administration.issues.IssueTrackHandler;
import com.gerantech.mmory.sfs.battle.BattleRoom;
import com.gerantech.mmory.sfs.battle.handlers.BattleJointHandler;
import com.gerantech.mmory.sfs.battle.handlers.BattleRequestCancelHandler;
import com.gerantech.mmory.sfs.battle.handlers.BattleRequestJoinHandler;
import com.gerantech.mmory.sfs.battle.handlers.BattleRequestLeaveHandler;
import com.gerantech.mmory.sfs.battle.handlers.BattleRequestStartHandler;
import com.gerantech.mmory.sfs.battle.handlers.BattleSendStickerHandler;
import com.gerantech.mmory.sfs.battle.handlers.BattleSummonRequestHandler;
import com.gerantech.mmory.sfs.battle.handlers.BattleUsersExitHandler;
import com.gerantech.mmory.sfs.challenges.handlers.ChallengeCollectRewardHandler;
import com.gerantech.mmory.sfs.challenges.handlers.ChallengeGetAllHandler;
import com.gerantech.mmory.sfs.challenges.handlers.ChallengeJoinHandler;
import com.gerantech.mmory.sfs.challenges.handlers.ChallengeUpdateHandler;
import com.gerantech.mmory.sfs.handlers.CardUpgradeHandler;
import com.gerantech.mmory.sfs.handlers.ChangeDeckHandler;
import com.gerantech.mmory.sfs.handlers.ExchangeHandler;
import com.gerantech.mmory.sfs.handlers.JoinZoneEventHandler;
import com.gerantech.mmory.sfs.handlers.LoginEventHandler;
import com.gerantech.mmory.sfs.handlers.OauthHandler;
import com.gerantech.mmory.sfs.handlers.ProfileRequestHandler;
import com.gerantech.mmory.sfs.handlers.PurchaseVerificationHandler;
import com.gerantech.mmory.sfs.handlers.RankRequestHandler;
import com.gerantech.mmory.sfs.handlers.RegisterPushHandler;
import com.gerantech.mmory.sfs.handlers.ResetLobbiesHandler;
import com.gerantech.mmory.sfs.handlers.SelectNameRequestHandler;
import com.gerantech.mmory.sfs.handlers.UserPrefsRequestHandler;
import com.gerantech.mmory.sfs.inbox.InboxBroadcastMessageHandler;
import com.gerantech.mmory.sfs.inbox.InboxConfirmHandler;
import com.gerantech.mmory.sfs.inbox.InboxGetRelationsHandler;
import com.gerantech.mmory.sfs.inbox.InboxGetThreadsHandler;
import com.gerantech.mmory.sfs.inbox.InboxOpenHandler;
import com.gerantech.mmory.sfs.quests.QuestInitializeHandler;
import com.gerantech.mmory.sfs.quests.QuestRewardCollectHandler;
import com.gerantech.mmory.sfs.socials.handlers.BuddyBattleRequestHandler;
import com.gerantech.mmory.sfs.socials.handlers.FriendAddRequestHandler;
import com.gerantech.mmory.sfs.socials.handlers.FriendDataHandler;
import com.gerantech.mmory.sfs.socials.handlers.FriendRemoveRequestHandler;
import com.gerantech.mmory.sfs.socials.handlers.LobbyCreateHandler;
import com.gerantech.mmory.sfs.socials.handlers.LobbyDataHandler;
import com.gerantech.mmory.sfs.socials.handlers.LobbyJoinHandler;
import com.gerantech.mmory.sfs.socials.handlers.LobbyLeaveHandler;
import com.gerantech.mmory.sfs.socials.handlers.LobbyPublicRequestHandler;
import com.gerantech.mmory.sfs.socials.handlers.LobbyRemoveHandler;
import com.gerantech.mmory.sfs.socials.handlers.RoadCollectRewardHandler;
import com.gerantech.mmory.sfs.utils.PasswordGenerator;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.extensions.SFSExtension;

/**
 * @author ManJav
 */
public class MMOryExtension extends SFSExtension
{
	public void init()
	{
   	UtilBase.setExtension(this);
		UtilBase.setBattleClass(BattleRoom.class);

		// Add server event handlers
		addEventHandler(SFSEventType.USER_LOGIN, LoginEventHandler.class);
		addEventHandler(SFSEventType.USER_JOIN_ZONE, JoinZoneEventHandler.class);
		addEventHandler(SFSEventType.USER_JOIN_ROOM, BattleJointHandler.class);
		addEventHandler(SFSEventType.USER_LEAVE_ROOM, BattleUsersExitHandler.class);
		addEventHandler(SFSEventType.USER_DISCONNECT, BattleUsersExitHandler.class);

		// Add startBattle request handler
		addRequestHandler(SFSCommands.BATTLE_JOIN, BattleRequestJoinHandler.class);
		addRequestHandler(SFSCommands.BATTLE_START, BattleRequestStartHandler.class);
		addRequestHandler(SFSCommands.BATTLE_CANCEL, BattleRequestCancelHandler.class);
		addRequestHandler(SFSCommands.BATTLE_LEAVE, BattleRequestLeaveHandler.class);

		addRequestHandler(SFSCommands.BATTLE_SUMMON, BattleSummonRequestHandler.class);
		addRequestHandler(SFSCommands.BATTLE_SEND_STICKER, BattleSendStickerHandler.class);

		// Add billing upgrade handler
		addRequestHandler(SFSCommands.CARD_UPGRADE, CardUpgradeHandler.class);
		addRequestHandler(SFSCommands.COLLECT_ROAD_REWARD, RoadCollectRewardHandler.class);
		
		// Add rank handler
		addRequestHandler(SFSCommands.RANK, RankRequestHandler.class);
		addRequestHandler(SFSCommands.SELECT_NAME, SelectNameRequestHandler.class);
		addRequestHandler(SFSCommands.EXCHANGE, ExchangeHandler.class);
		addRequestHandler(SFSCommands.OAUTH, OauthHandler.class);
		addRequestHandler(SFSCommands.VERIFY_PURCHASE, PurchaseVerificationHandler.class);
		addRequestHandler(SFSCommands.CHANGE_DECK, ChangeDeckHandler.class);
		addRequestHandler(SFSCommands.PREFS, UserPrefsRequestHandler.class);
		addRequestHandler(SFSCommands.REGISTER_PUSH, RegisterPushHandler.class);
		
		// Social handlers
		addRequestHandler(SFSCommands.LOBBY_DATA, LobbyDataHandler.class);
		addRequestHandler(SFSCommands.LOBBY_JOIN, LobbyJoinHandler.class);
		addRequestHandler(SFSCommands.LOBBY_LEAVE, LobbyLeaveHandler.class);
		addRequestHandler(SFSCommands.LOBBY_CREATE, LobbyCreateHandler.class);
		addRequestHandler(SFSCommands.LOBBY_PUBLIC, LobbyPublicRequestHandler.class);
		addRequestHandler(SFSCommands.LOBBY_REMOVE, LobbyRemoveHandler.class);
		
		addRequestHandler(SFSCommands.BUDDY_ADD, FriendAddRequestHandler.class);
		addRequestHandler(SFSCommands.BUDDY_REMOVE, FriendRemoveRequestHandler.class);
		addRequestHandler(SFSCommands.BUDDY_BATTLE, BuddyBattleRequestHandler.class);
		
		addRequestHandler(SFSCommands.BUDDY_DATA, FriendDataHandler.class);
		
		addRequestHandler(SFSCommands.PROFILE, ProfileRequestHandler.class);

		addRequestHandler(SFSCommands.INBOX_GET_THREADS, InboxGetThreadsHandler.class);
		addRequestHandler(SFSCommands.INBOX_GET_RELATIONS, InboxGetRelationsHandler.class);
		addRequestHandler(SFSCommands.INBOX_OPEN, InboxOpenHandler.class);
		addRequestHandler(SFSCommands.INBOX_CONFIRM, InboxConfirmHandler.class);
		addRequestHandler(SFSCommands.INBOX_BROADCAST, InboxBroadcastMessageHandler.class);

		// administration handlers
		addRequestHandler(SFSCommands.ISSUE_REPORT, IssueReportHandler.class);
		addRequestHandler(SFSCommands.ISSUE_GET, IssueGetHandler.class);
		addRequestHandler(SFSCommands.ISSUE_TRACK, IssueTrackHandler.class);
		addRequestHandler(SFSCommands.RESTORE, RestoreHandler.class);
		addRequestHandler(SFSCommands.BAN, BanHandler.class);
		addRequestHandler(SFSCommands.BANNED_DATA_GET, GetBannedDataHandler.class);
		addRequestHandler(SFSCommands.OFFENDER_DATA_GET, GetOffenderDataHandler.class);
		addRequestHandler(SFSCommands.INFRACTIONS_GET, InfractionsGetHandler.class);
		addRequestHandler(SFSCommands.INFRACTIONS_DELETE, InfractionsDeleteHandler.class);
		addRequestHandler(SFSCommands.PLAYERS_GET, PlayersGetHandler.class);
		addRequestHandler(SFSCommands.SEARCH_IN_CHATS, SearchInChatsHandler.class);
		addRequestHandler(SFSCommands.SPECTATE_JOIN, SpectateRequestJoinHandler.class);
		addRequestHandler(SFSCommands.SPECTATE_LEAVE, SpectateRequestLeaveHandler.class);
		addRequestHandler("resetalllobbies", ResetLobbiesHandler.class);

		addRequestHandler(SFSCommands.CHALLENGE_JOIN, ChallengeJoinHandler.class);
		addRequestHandler(SFSCommands.CHALLENGE_UPDATE, ChallengeUpdateHandler.class);
		addRequestHandler(SFSCommands.CHALLENGE_GET_ALL, ChallengeGetAllHandler.class);
		addRequestHandler(SFSCommands.CHALLENGE_COLLECT, ChallengeCollectRewardHandler.class);

		addRequestHandler(SFSCommands.QUEST_INIT, QuestInitializeHandler.class);
		addRequestHandler(SFSCommands.QUEST_REWARD_COLLECT, QuestRewardCollectHandler.class);
	}

	@Override
	public Object handleInternalMessage(String cmdName, Object params)
	{
		trace(cmdName, params);
		if( cmdName.equals("setumtime") )
			return (LoginEventHandler.UNTIL_MAINTENANCE = (int)Instant.now().getEpochSecond() + Integer.parseInt((String) params)) + ";;";
		// else if( cmdName.equals("ban") )
		// 	return BanUtils.getInstance().checkOffends((String) params);
		else if( cmdName.equals("servercheck") )
			return "OK HAHAHA.";
		else if( cmdName.equals("resetkeylimit") )
			return DBUtils.getInstance().resetDailyBattles();
		else if( cmdName.equals("resetlobbiesactiveness") )
			return LobbyUtils.getInstance().resetActivities();
		else if( cmdName.equals("getplayernamebyic") )
			return PasswordGenerator.getIdAndNameByInvitationCode((String) params);
		else if( cmdName.equals("cleanInactives"))
			return DBUtils.getInstance().cleanInactiveUsers((String) params);
		//else if( cmdName.equals("custom") )
		//	return LobbyUtils.getInstance().moveActiveness();
		
		return null;
	}
}