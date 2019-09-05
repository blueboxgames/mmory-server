package com.gerantech.mmory.sfs.battle.bots;

import java.util.Map;

import com.gerantech.mmory.core.Player;
import com.gerantech.mmory.core.battle.BattleField;
import com.gerantech.mmory.core.battle.GameObject;
import com.gerantech.mmory.core.battle.units.Unit;
import com.gerantech.mmory.core.constants.CardTypes;
import com.gerantech.mmory.core.constants.ResourceType;
import com.gerantech.mmory.core.constants.StickerType;
import com.gerantech.mmory.core.scripts.ScriptEngine;
import com.gerantech.mmory.core.socials.Challenge;
import com.gerantech.mmory.core.utils.CoreUtils;
import com.gerantech.mmory.sfs.battle.BattleRoom;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;

/**
 * Created by ManJav on 9/30/2018.
 */
public class BattleBot
{
    /** Time between each summon that bot commits. */
    static final private int SUMMON_DELAY = 1060;
    /** Debug flag. */
    static final private boolean DEBUG_MODE = true;
    /** Middle summon X position threshold. */
    static final private double SUMMON_X_THRESHOLD = 200;
    /** 
     * Data which will decide on which unit type to select.
     * Recieves an array containing coefficents on each parameter to decide.
     */
    static final private haxe.root.Array<?> PREF_COEFFICENTS = 
        (haxe.root.Array<?>) ScriptEngine.get(ScriptEngine.T67_BATTLE_BOT_BOUNTY, null, null, null, null);
    /** Know the player bot is playing with. */
    private Player player;
    /** Current Server BattleRoom. */
    private BattleRoom battleRoom;
    /** BattleField of the core data. */
    private BattleField battleField;
    /** Last summon interval. */
    private double lastSummonInterval = 0;
    
    // ---------------------------------
    private float battleRatio = 0;
    private SFSObject chatParams;
    private int defaultIndex = 0;
    // ---------------------------------

    // Bot select unit preferences coefficents.
    private double positionCoefficent;
    private double damageCoefficent;
    private double healthCoefficent;
    private double targetTypeCoefficent;
    private double speedCoefficent;

    // Bot summoning side preference. 0 -> Right; 1-> Left
    private int sidePreference;

    /**
     * A trainable bot to play instead of player.
     * @param battleRoom
     */
    public BattleBot(BattleRoom battleRoom)
    {
        this.positionCoefficent = (double) PREF_COEFFICENTS.__get(0);
        this.damageCoefficent = (double) PREF_COEFFICENTS.__get(1);
        this.healthCoefficent = (double) PREF_COEFFICENTS.__get(2);
        this.targetTypeCoefficent = (double) PREF_COEFFICENTS.__get(3);
        this.speedCoefficent = (double) PREF_COEFFICENTS.__get(4);
        this.battleRoom = battleRoom;
        this.battleField = battleRoom.battleField;
        this.sidePreference = headOrTail();

        this.chatParams = new SFSObject();
        this.chatParams.putDouble("ready", battleField.now + 15000);

        this.player = battleField.games.__get(0).player;
        this.trace("p-point:" + player.getResource(ResourceType.R2_POINT), "b-point:"+ battleField.games.__get(1).player.getResource(ResourceType.R2_POINT), " winRate:" + player.getResource(ResourceType.R16_WIN_RATE), "difficulty:" + battleField.difficulty);
    }

    public void reset()
    {
        lastSummonInterval = 0;
    }

    public void update()
    {
        if( battleField.state < BattleField.STATE_2_STARTED && (player.get_battleswins() < 3 || Math.random() < 0.3) )
            return;
        summonCard();
        updateChatProcess();
    }

    private void summonCard()
    {
        // Don't summon during tutorial of mode 0.
        if( player.get_battleswins() < 1 && this.battleField.field.mode == 0 )
            return;

        // Don't rapid summon.
        if( lastSummonInterval == 0 )
            lastSummonInterval = battleField.now + SUMMON_DELAY;
        if( lastSummonInterval > battleField.now )
            return;

        // Player and Bot Targets.
        Unit playerHead = null;
        Unit botHead = null;

        // Current maximum bounty on player head card.
        double playerHeadBounty = 0;
        boolean shouldUseSpell = false;

        for( Map.Entry<Object, Unit> entry : battleField.units._map.entrySet() )
        {
            if( !CardTypes.isTroop((int)entry.getKey()) || entry.getValue().state < GameObject.STATE_2_MORTAL )
                continue;
            
            // Unit is part of player units.
            if( entry.getValue().side == 0 )
            {
                double bounty = calculateBounty(entry.getValue());
                if( bounty > playerHeadBounty )
                {
                    /**
                     * If a new unit calculated bounty is higher than current
                     * head bounty changes the current head and head bounty to
                     * current unit and current unit bounty.
                     */
                    playerHead = entry.getValue();
                    playerHeadBounty = bounty;
                }
                // Prioritize the ones in lead of 80% of field in touchdown.
                if( battleField.field.mode == Challenge.MODE_1_TOUCHDOWN && (playerHead != null && entry.getValue().y < playerHead.y) && entry.getValue().y < 0.2 * BattleField.HEIGHT )
                    playerHead = entry.getValue();
                
                if( CardTypes.isHero(entry.getValue().card.type) )
                {
                    if( (entry.getValue().cardHealth / entry.getValue().health) > 0.2 )
                    {
                        shouldUseSpell = true;
                        playerHead = entry.getValue();
                    }
                }
            }
            else
            {
                // bottom of bot troops
                if( botHead == null || botHead.y < entry.getValue().y || botHead.health > entry.getValue().health )
                    botHead = entry.getValue();
                // Change side preference to bot.
                sidePreference = BattleField.WIDTH * 0.5 > botHead.x ? 0 : 1;
            }
        }

        // Start of bot card summoning unit.
        int cardType;
        double x = BattleField.WIDTH * Math.random();
        double y = Math.random() * (BattleField.HEIGHT * 0.3);
        if( playerHead == null )
        {
            // Choose summon position wisely.
            if( battleField.field.mode == Challenge.MODE_0_HQ )
            {
                y = BattleField.HEIGHT * 0.45;
                if( this.sidePreference < 1 )
                    x = (BattleField.WIDTH - ( Math.random() * BattleField.PADDING )) + BattleField.PADDING;
                else
                    x = (Math.random() * BattleField.PADDING) + BattleField.PADDING;
            }
            else if ( battleField.field.mode == Challenge.MODE_1_TOUCHDOWN )
            {
                x = ( Math.random() * (2 * SUMMON_X_THRESHOLD)) + ((BattleField.WIDTH * 0.5) - SUMMON_X_THRESHOLD);
                y = BattleField.HEIGHT * 0.35;
            }

            cardType = battleField.decks.get(1).queue_get(defaultIndex);
            if( CardTypes.isSpell(cardType) && battleField.field.mode == Challenge.MODE_1_TOUCHDOWN )
            {
                skipCard(cardType);
                return;
            }

            if( cardType == 109 )
            {
                skipCard(cardType);
                return;
            }

            if( (double)battleField.elixirUpdater.bars.__get(1) < CoreUtils.clamp(battleField.difficulty * 0.7, 4, 9.5) )// waiting for more elixir to create waves
            {
                // trace("wait for", battleField.elixirUpdater.bars.__get(1), CoreUtils.clamp(battleField.difficulty * 0.7, 4, 9.5), battleField.difficulty);
                return;
            }
        }
        else
        {
            int cardIndex = 0;
            if( shouldUseSpell )
                cardIndex = getSpell() < 0 ? getCandidateCardIndex(playerHead.card.type) : getSpell();
            else
                cardIndex = getCandidateCardIndex(playerHead.card.type);
            cardType = battleField.decks.get(1).queue_get(cardIndex);
            double summonDegree = Math.random() * 180;

            if( !isRanged(cardType) )
            {
                if( battleField.field.mode == Challenge.MODE_0_HQ && playerHead.y > BattleField.HEIGHT * 0.45 || 
                    battleField.field.mode == Challenge.MODE_1_TOUCHDOWN && playerHead.y > BattleField.HEIGHT * 0.35 )
                {
                    if( (double) battleField.elixirUpdater.bars.__get(1) > 8)
                    {
                        if( !(getRangedCandidateCardIndex() < 0) )
                        {
                            cardIndex = getRangedCandidateCardIndex();
                            cardType = battleField.decks.get(1).queue_get(cardIndex);
                            if( cardType == 109 )
                            {
                                skipCard(cardType);
                                return;
                            }
                        }
                    }
                    else
                        return;
                }
            }
            x = isRanged(cardType) ? playerHead.x + ( Math.cos( summonDegree ) * battleField.decks.get(1)._map.get(cardType).bulletRangeMax ) : playerHead.x;
            y = isRanged(cardType) ? playerHead.y + ( Math.sin( summonDegree ) * battleField.decks.get(1)._map.get(cardType).bulletRangeMax ) : playerHead.y;
            // trace("playerHeader:"+ playerHead.card.type, "x:"+ x, "y:"+ y, "e:"+ battleField.elixirUpdater.bars.__get(1), "ratio:" + battleRoom.endCalculator.ratio());
            // trace("cardType"+ cardType, "x:"+ x, "y:"+ y, "e:"+ battleField.elixirUpdater.bars.__get(1), "ratio:" + battleRoom.endCalculator.ratio());

            if( CardTypes.isSpell(cardType) || playerHead.y < BattleField.HEIGHT * 0.4 )// drop spell
            {
                if( CardTypes.isSpell(cardType) )
                    // trace("isSpell", cardType);
                y = playerHead.y - (CardTypes.isTroop(playerHead.card.type) && playerHead.state == GameObject.STATE_4_MOVING ? 200 : 0);
            }
            else if( cardType == 109 )
            {
                if( botHead == null || CardTypes.isTroop(botHead.card.type) )
                {
                    skipCard(cardType);
                    return;
                }
                y = botHead.y - 300; //summon healer for covering
            }
        }

        // when battlefield is empty
        if( botHead == null && cardType == 109 || CardTypes.isTroop(botHead.card.type) && cardType == 109 )// skip spells and healer
            return;

        if( defaultIndex  != 0 )
            defaultIndex = 0;

        int id = battleRoom.summonUnit(1, cardType, validatedX(x), validatedY(y));
        if( id >= 0 )
        {
            lastSummonInterval = battleField.now + SUMMON_DELAY;
            return;
        }
//
//        // fake stronger bot
//        if( player.get_battleswins() > 3 )
//            battleField.elixirSpeeds.__set(1, battleRoom.endCalculator.ratio() > 1 ? 1 + battleField.difficulty * 0.04 : 1);
    }

    private void skipCard(int cardType)
    {
        defaultIndex ++;
        if( defaultIndex >= battleField.decks.get(1).keys().length )
            defaultIndex = 0;
        // trace("skipCard", cardType, "index:", defaultIndex);
    }

    private int getCandidateCardIndex(int type)
    {
        haxe.root.Array<?> candidates = (haxe.root.Array<?>) ScriptEngine.get(-3, type, null, null, null);
        int len = candidates.length;
        for (int i = defaultIndex; i < len; i++)
        {
            int index = battleField.decks.get(1).queue_indexOf((int) candidates.__get(i));
            // trace("queue_indexOf", i, candidates.__get(i), index);
            if( index > 0 && index < 4 )
                return index;
        }
        return 0;
    }

    private int getRangedCandidateCardIndex()
    {
        for (int i = 0; i < 4; i++)
        {
            int index = (int) battleField.decks.get(1)._queue.get(i);
            if( isRanged( battleField.decks.get(1)._map.get(index).type ) )
                return i;
        }
        return -1;
    }

    private int getSpell()
    {
        for (int i = 0; i < 4; i++)
        {
            int index = (int) battleField.decks.get(1)._queue.get(i);
            if( CardTypes.isSpell( battleField.decks.get(1)._map.get(index).type ) )
                return i;
        }
        return -1;
    }

    public void chatStarting(float battleRatio)
    {
        if( battleField.field.isOperation() || player.inTutorial() )
            return;

        // verbose bot threshold
        if( chatParams.getDouble("ready") > battleField.now || Math.random() > 0.1 )
            return;

        // trace(this.battleRatio, battleRatio);
        if( battleRatio != this.battleRatio )
        {
            chatParams.putInt("t", StickerType.getRandomStart(battleRatio, battleField.games.__get(0)));
            chatParams.putInt("tt", 1);
            chatParams.putDouble("ready", battleField.now + Math.random() * 2500 + 1500);
        }
        this.battleRatio = battleRatio;
    }

    public void chatAnswering(ISFSObject params)
    {
        if( chatParams.getDouble("ready") > battleField.now || Math.random() < 0.4 )
            return;

        int answer = StickerType.getAnswer( params.getInt("t") );
        if( answer <= -1 )
            return;

        chatParams.putInt("t", answer);
        chatParams.putInt("tt", 1);
        chatParams.putInt("wait", 0);
        chatParams.putDouble("ready", battleField.now + Math.random() * 2500 + 2500);
    }

    /**
     * A method to calculate a unit bounty.
     */
    private double calculateBounty(Unit unit)
    {
        // Closeness to target.
        double closeness = Math.abs(unit.y - BattleField.HEIGHT) * this.positionCoefficent;
        // Damage multiplier, Tasmanian Devil To kill faster.
        double damageMult = unit.card.bulletDamage * this.damageCoefficent;
        // Health multiplier, Bugs Bunny never dies, so does Firouz.
        double healthMult = unit.health * this.healthCoefficent;
        // Speed multiplier, Speedy Gonzales has to die faster before it reaches target!
        double speedMult = unit.card.speed * this.speedCoefficent;
        // Target type
        double buildingFocusMult = (unit.card.focusUnit ? 1 : 0) * this.targetTypeCoefficent;
        
        // --- [Target Multiplier] ---
        // We might need distance to target in future.
        // double distanceToTarget = CoreUtils.getDistance(unit.x, unit.y, unit.target.x, unit.target.y);
        return closeness + damageMult + healthMult + speedMult + buildingFocusMult;
    }

    /**
     * Ranged unit validation.
     * @param cardType
     * @return
     */
    private boolean isRanged(int cardType)
    {
        return battleField.decks.get(1)._map.get(cardType).bulletRangeMax > 150 ? true : false;
    }

    /**
     * Return a random 0 or 1
     */
    private int headOrTail()
    {
        if( Math.random() < 0.5 )
            return 1;
        return 0;
    }

    /**
     * A method to validate calculated x
     * @param x
     * @return
     */
    private double validatedX(double x)
    {
        if( battleField.field.mode == Challenge.MODE_0_HQ )
        {

        }
        return x;
    }

    /**
     * A method to validate calculated y
     * @param y
     * @return
     */
    private double validatedY(double y)
    {
        if( battleField.field.mode == Challenge.MODE_0_HQ )
            return y > BattleField.HEIGHT * 0.45 ? BattleField.HEIGHT * 0.45 : y;
        else if ( battleField.field.mode == Challenge.MODE_1_TOUCHDOWN )
            return y > BattleField.HEIGHT * 0.35 ? BattleField.HEIGHT * 0.35 : y;
        return y;
    }

    private void updateChatProcess()
    {
        if( chatParams.getDouble("ready") > battleField.now || !chatParams.containsKey("t") )
            return;

        battleRoom.sendSticker(null, chatParams);
        chatParams.removeElement("t");
        chatParams.putDouble("ready", battleField.now + 10000);
    }
    private void trace(Object... args)
    {
        if( DEBUG_MODE )
            battleRoom.trace(args);
    }
}