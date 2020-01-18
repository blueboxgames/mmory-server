package com.gerantech.mmory.sfs.battle.bots;

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
import com.gerantech.mmory.core.utils.Point2;
import com.gerantech.mmory.sfs.battle.BattleRoom;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;

import haxe.root.Array;

/**
 * Created by ManJav on 9/30/2018.
 */
public class BattleBot
{
    /** Time between each summon that bot commits. */
    static final private int SUMMON_DELAY = 1060;
    /** Debug flag. */
    // static final private boolean DEBUG_MODE = true;
    /** Middle summon X position threshold. */
    static final private double SUMMON_X_THRESHOLD = 200;
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

    // Bot summoning random
    private boolean shouldPlayRandom;

    // noobiness = SUMMON_DELAY * NoobCounter ;
    // Noob counter should be found using battlefield difficauly.
    // NoobCounter = ABS( log(difficauly+1) - 3 )
    private double noobCounter;

    /**--------- Logging ---------*/
    /** Enable or disable logging. */
    static final private boolean LOG_ENABLED = false;
    /** Logs general information about bot. */
    static final private int LOG_GENERAL = 0x1;
    /** Logs selections of bot and it's card decision. */
    static final private int LOG_TYPESELECT = 0x10;
    /** Logs everything. */
    static final private int LOG_VERBOSE = 0x1000000;
    /** Stores log flags */
    private int logFlags = 0x0;
    /** Log buffer */
    private String logBuffer = "";
    private void flushLogBuffer()
    {
        this.logBuffer = "";
    }
    /** Log flag getter. */
    private boolean getLogFlag(int flag)
    {
        return (this.logFlags &= flag) == flag ? true : false;
    }
    /** Log flag setter. */
    private void setFlag(int flag, boolean isTrue)
    {
        if( isTrue )
            this.logFlags |= flag;
        else
            this.logFlags ^= flag;
    }

    /**
     * A trainable bot to play instead of player.
     * @param battleRoom
     */
    public BattleBot(BattleRoom battleRoom)
    {
    /** 
     * Data which will decide on which unit type to select.
     * Recieves an array containing coefficents on each parameter to decide.
     */
        Array<?> scriptRef = (haxe.root.Array<?>) ScriptEngine.get(ScriptEngine.T67_BATTLE_BOT_BOUNTY, null, null, null, null);
        this.positionCoefficent = (double) scriptRef.__get(0);
        this.damageCoefficent = (double) scriptRef.__get(1);
        this.healthCoefficent = (double) scriptRef.__get(2);
        this.targetTypeCoefficent = (double) scriptRef.__get(3);
        this.speedCoefficent = (double) scriptRef.__get(4);
        this.battleRoom = battleRoom;
        this.battleField = battleRoom.battleField;
        this.noobCounter = Math.log( Math.abs( (double) battleField.difficulty - 3 ) );
        this.sidePreference = headOrTail();
        this.shouldPlayRandom = 1 - ( battleField.difficulty * 0.5 ) < Math.random();

        this.chatParams = new SFSObject();
        this.chatParams.putDouble("ready", battleField.now + 15000);

        this.player = battleField.games.__get(0).player;

        // Set log flags
        this.setFlag(LOG_GENERAL, false);
        this.setFlag(LOG_TYPESELECT, false);
        this.setFlag(LOG_VERBOSE, false);

        if( getLogFlag(LOG_GENERAL) )
        {
            this.logBuffer += "General\n---------------\n";
            this.logBuffer += "Player Points: " + player.getResource(ResourceType.R2_POINT) + "\n";
            this.logBuffer += "Bot Points: " + battleField.games.__get(1).player.getResource(ResourceType.R2_POINT) + "\n";
            this.logBuffer += "Win-rate: " + player.getResource(ResourceType.R16_WIN_RATE) + "\n";
            this.logBuffer += "Difficulty: " + battleField.difficulty + "\n";
            this.logBuffer += "noobCounter: " + noobCounter + "\n";

            this.trace(this.logBuffer);
            this.flushLogBuffer();
        }
    }

    public void reset()
    {
        lastSummonInterval = 0;
    }

    public void update()
    {
        /** With randomness of:
         * * 1/5 -> no summon
         * * 4/5 -> do summon
         * bot won't summon.
         */
        if( Math.random() < 0.2 )
        {
            trace("BattleBot: 1: randomness summon return.");
            return;
        }
        /** Don't summon if mode is not 1 and dice roll says not to */
        else if( this.battleField.field.mode == 0 )
        {
            if( player.get_battleswins() < 1 )
            {
                trace("BattleBot: 2: bosscar less than 1 win return.");
                return;
            }
        }

        if( battleRoom.getState() >= BattleField.STATE_2_STARTED )
        {
            summonCard();
            updateChatProcess();
        }
    }

    private void summonCard()
    {
        // Don't rapid summon.
        if( lastSummonInterval == 0 )
        {
            if( battleField.difficulty < 5 )
                lastSummonInterval = battleField.now + ( SUMMON_DELAY * this.noobCounter ) + (SUMMON_DELAY * Math.random());
            else
                lastSummonInterval = battleField.now + ( SUMMON_DELAY * this.noobCounter ); 
        }
        if( lastSummonInterval > battleField.now )
        {
            trace("BattleBot: 4: rapid summon return");
            return;
        }

        // Player and Bot Targets.
        Unit playerHead = null;
        Unit botHead = null;

        // Current maximum bounty on player head card.
        Unit u = null;
        double playerHeadBounty = 0;
        boolean shouldUseSpell = false;
        int leftCount = 0;
        int rightCount = 0;
        for( int i = 0; i < battleField.units.length ; i++ )
        {
            u = battleField.units.__get(i);
			int k = u.id;
            if( !CardTypes.isTroop(k) || u.state < GameObject.STATE_2_MORTAL )
                continue;
            
            // Unit is part of player units.
            if( u.side == 0 )
            {
                double bounty = calculateBounty(u);
                if( bounty > playerHeadBounty )
                {
                    /**
                     * If a new unit calculated bounty is higher than current
                     * head bounty changes the current head and head bounty to
                     * current unit and current unit bounty.
                     */
                    playerHead = u;
                    playerHeadBounty = bounty;
                }
                // Prioritize the ones in lead of 80% of field in touchdown.
                if( battleField.field.mode == Challenge.MODE_1_TOUCHDOWN && 
                    (playerHead != null && u.y < playerHead.y) && 
                    u.y < 0.2 * BattleField.HEIGHT )
                    playerHead = u;
                
                // Prioritize hero with low hp.
                if( CardTypes.isHero(u.card.type) && (u.health < u.cardHealth * 0.3) )
                {
                    if( getSpell() != -1 )
                    {
                        shouldUseSpell = true;
                        playerHead = u;
                        playerHeadBounty -= 1;
                    }
                }
            }
            else
            {
                if( u.x > BattleField.WIDTH * 0.5 )
                    rightCount++;
                else if ( u.x <  BattleField.WIDTH * 0.5 )
                    leftCount++;
                // bottom of bot troops
                if( botHead == null || botHead.y < u.y || botHead.health > u.health )
                    botHead = u;
            }
        }

        this.sidePreference = leftCount >= rightCount ? 0 : 1;
        // Start of bot card summoning unit.
        int cardType;
        int id;
        double x = BattleField.WIDTH * Math.random();
        double y = Math.random() * (BattleField.HEIGHT * 0.3);
        if( playerHead == null )
        {
            // Choose summon position wisely.
            if( battleField.field.mode == Challenge.MODE_0_HQ )
            {
                y = BattleField.HEIGHT * 0.45;
                if( this.sidePreference < 1 )
                    x = (BattleField.WIDTH - ( Math.random() * BattleField.PADDING )) - BattleField.PADDING;
                else
                    x = (Math.random() * BattleField.PADDING) + BattleField.PADDING;
            }
            else if( battleField.field.mode == Challenge.MODE_1_TOUCHDOWN )
            {
                x = ( Math.random() * (2 * SUMMON_X_THRESHOLD)) + ((BattleField.WIDTH * 0.5) - SUMMON_X_THRESHOLD);
                y = BattleField.HEIGHT * 0.35;
            }

            cardType = battleField.decks.get(1)._queue[defaultIndex];
            if( CardTypes.isSpell(cardType) && battleField.field.mode == Challenge.MODE_1_TOUCHDOWN )
            {
                if( getLogFlag(LOG_VERBOSE) )
                {
                    // Double flush buffer, multiple log should not happend.
                    // this is a cause of deadlock.
                    this.flushLogBuffer();
                    this.logBuffer = "Bot\n---\n";
                    this.logBuffer += "Spell in touchdown skipped.\n";
                    this.trace(this.logBuffer);
                    this.flushLogBuffer();
                }
                skipCard(cardType);
                trace("BattleBot: 5: return");
                return;
            }

            if( cardType == 109 && battleField.difficulty > 1 )
            {
                skipCard(cardType);
                trace("BattleBot: 6: return");
                return;
            }

            // waiting for more elixir to create waves
            if( (double)battleField.elixirUpdater.bars.__get(1) < CoreUtils.clamp(battleField.difficulty * 0.7, 4, 9.5) )
                return;
        }
        else
        {
            if( botHead != null )
                sidePreference = BattleField.WIDTH * 0.5 > botHead.x ? 0 : 1;
            int cardIndex = 0;
            if( shouldUseSpell && battleField.difficulty > 1 )
            {
                cardIndex = getSpell() < 0 ? getCandidateCardIndex(playerHead.card.type) : getSpell();
            }
            else
            {
                if( shouldPlayRandom )
                    cardIndex = getCandidateCardIndex(playerHead.card.type);
                else
                    cardIndex = (int) Math.floor(Math.random()*4.5);
            }
            cardType = battleField.decks.get(1)._queue[cardIndex];
            double summonDegree = Math.random() * 180;

            if( !isRanged(cardType) && !CardTypes.isSpell(cardType) )
            {
                if( battleField.field.mode == Challenge.MODE_0_HQ && playerHead.y > BattleField.HEIGHT * 0.45 || 
                    battleField.field.mode == Challenge.MODE_1_TOUCHDOWN && playerHead.y > BattleField.HEIGHT * 0.35 )
                {
                    if( (double) battleField.elixirUpdater.bars.__get(1) > 8 )
                    {
                        // Selects a ranged card from deck.
                        if( !(getRangedCandidateCardIndex() < 0) )
                        {
                            cardIndex = getRangedCandidateCardIndex();
                            cardType = battleField.decks.get(1)._queue[cardIndex];
                            if( cardType == 109 )
                            {
                                skipCard(cardType);
                                trace("BattleBot: 7: return");
                                return;
                            }
                        }
                    }
                }
            }

            if( CardTypes.isFlying(playerHead.card.type) )
            {
                // Only summon card when card has less more than 30% of it's health.
                // REMINDER: C117 counter must be area damage.
                if( playerHead.health > playerHead.card.health * 0.3 || playerHead.card.count() > 2 )
                {
                    trace("CardTypes.isFlying() => Must now counter flying card.");
                    if( battleField.decks.get(1).get(cardType).focusHeight > playerHead.card.z )
                    {
                        cardIndex = getCardIndexForHeight(playerHead.card.z);
                        if( cardIndex == -1 )
                        {
                            trace("CardTypes.isFlying() => No counter in deck must spawn a bait.");
                            trace("Heres my deck: " + battleField.decks.get(1).queue_String() );
                            cardType = battleField.decks.get(1)._queue[getCandidateCardIndex(playerHead.card.type)];
                        }
                        else
                            cardType = battleField.decks.get(1)._queue[cardIndex];
                        trace("CardTypes.isFlying() => Spawning " + cardType +  " for flying card");
                    }
                }
            }

            if( isRanged(cardType) )
            {
                double rangeUnitXPosition = playerHead.x + ( Math.cos( summonDegree ) * battleField.decks.get(1).get(cardType).bulletRangeMax );
                if( rangeUnitXPosition > BattleField.WIDTH || rangeUnitXPosition < 0 )
                    x = playerHead.x - ( Math.cos( summonDegree ) * battleField.decks.get(1).get(cardType).bulletRangeMax );
                else
                    x = rangeUnitXPosition;
            }
            else
                x = playerHead.x;
            y = isRanged(cardType) ? playerHead.y + ( Math.sin( summonDegree ) * battleField.decks.get(1).get(cardType).bulletRangeMax ) : playerHead.y;
            
            if( isBuilding(cardType) && !CardTypes.isSpell(cardType) )
            {
                if( playerHead.y < BattleField.HEIGHT * 0.6 )
                {
                    x = getBuildingPosition().x;
                    y = getBuildingPosition().y;
                }
                else
                {
                    skipCard(cardType);
                    trace("BattleBot: 9: return");
                    return;
                }
            }

            // Drop spell
            if( CardTypes.isSpell(cardType) )
            {
                // Change spell if hero it's hero and a lot hp.
                if( CardTypes.isHero(playerHead.card.type) && playerHead.health > playerHead.cardHealth * 0.3 )
                    cardIndex = (int) Math.floor(Math.random()*4.5);
                x = playerHead.x;
                y = playerHead.y - (CardTypes.isTroop(playerHead.card.type) && playerHead.state == GameObject.STATE_4_MOVING ? 80 : 0);
                trace("Using Spell on " + playerHead.card.type + " in: " + x + "," + y);
            }
            if( cardType == 109 )
            {
                if( botHead == null || CardTypes.isTroop(botHead.card.type) )
                {
                    skipCard(cardType);
                    trace("BattleBot: 10: return");
                    return;
                }
                y = botHead.y - 300; //summon healer for covering
            }
        }

        // when battlefield is empty
        if( botHead == null && cardType == 109 )// skip spells and healer
        {
            trace("BattleBot: 11: return");
            return;
        }

        if( defaultIndex  != 0 )
            defaultIndex = 0;

        // Ceil the x and y before passing to summon.
        x = Math.ceil(x);
        y = Math.ceil(y);
        
        
        if( CardTypes.isSpell(cardType) )
        {
            id = battleRoom.summonUnit(1, cardType, x, y, this.battleField.now);
        }
        else 
        {
            if( playerHead != null )
            {
                if( playerHead.y > BattleField.HEIGHT * 0.6 )
                {
                    // With randomness of half 0.5
                    // if( Math.random() > 0.5 )
                    y = Math.random() * BattleField.HEIGHT * 0.4;
                }
            }
            if( y > BattleField.HEIGHT * 0.3 )
            {
                if( x < (BattleField.WIDTH * 0.5) && x > (BattleField.WIDTH * 0.25) )
                    CoreUtils.clamp(x, 0, BattleField.WIDTH * 0.25);
                if( x > (BattleField.WIDTH * 0.5) && x < (BattleField.WIDTH * 0.75) )
                    CoreUtils.clamp(x, (BattleField.WIDTH * 0.75), BattleField.WIDTH );
            }
            Point2 summonPoint = mirrorSummon(x,y,cardType);
            id = battleRoom.summonUnit(1, cardType, summonPoint.x, summonPoint.y, this.battleField.now);
            trace("Bot tries to summon at: ("+x +","+ y+") | Validated point: (" + summonPoint.x +","+  summonPoint.y+")");
        }

        if( id >= 0 )
        {
            lastSummonInterval = battleField.now + SUMMON_DELAY;
            trace("BattleBot: 12: return");
            return;
        }
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

    private int getCardIndexForHeight(int z)
    {
        for (int i = 0; i < 4; i++)
        {
            int index = battleField.decks.get(1)._queue[i];
            if( battleField.decks.get(1).get(index).focusHeight < z || CardTypes.isSpell(battleField.decks.get(1).get(index).type) )
                return i;
        }
        return -1;
    }

    private int getRangedCandidateCardIndex()
    {
        for (int i = 0; i < 4; i++)
        {
            int index = battleField.decks.get(1)._queue[i];
            if( isRanged( battleField.decks.get(1).get(index).type ) )
                return i;
        }
        return -1;
    }

    private int getSpell()
    {
        for (int i = 0; i < 4; i++)
        {
            int index = battleField.decks.get(1)._queue[i];
            if( CardTypes.isSpell( battleField.decks.get(1).get(index).type ) )
                return i;
        }
        return -1;
    }

    private Point2 mirrorSummon(double x, double y, int cardType)
    {
        Point2 reversePoint = battleField.fixSummonPosition(new Point2(BattleField.WIDTH - x, BattleField.HEIGHT - y), cardType, battleField.getSummonState(battleField.side == 0 ? 0 : 1), 1);
        return new Point2( BattleField.WIDTH - reversePoint.x, BattleField.HEIGHT - reversePoint.y );
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
        return battleField.decks.get(1).get(cardType).bulletRangeMax > 150 ? true : false;
    }

    private boolean isBuilding(int cardType)
    {
        return battleField.decks.get(1).get(cardType).speed == 0 ? true : false;
    }

    private Point2 getBuildingPosition()
    {
        double x = 0;
        double y = 0;
        if( battleField.field.mode == Challenge.MODE_1_TOUCHDOWN || battleField.field.mode == Challenge.MODE_2_BAZAAR )
        {
            double touchdownXThreshold = 200;
            double touchdownYThreshold = 50;
            x = ((BattleField.WIDTH * 0.5) - touchdownXThreshold) + (Math.random() * 2 * touchdownXThreshold);
            y = BattleField.HEIGHT - ( Math.random() * touchdownYThreshold );
        }
        if( battleField.field.mode == Challenge.MODE_0_HQ )
        {
            double baseDefXThreshold = BattleField.WIDTH * 0.5;
            double baseDefYThreshold = BattleField.HEIGHT * 0.5;
            x = ((BattleField.WIDTH * 0.5) - baseDefXThreshold) + (Math.random() * 2 * baseDefXThreshold);
            y = BattleField.HEIGHT - ( Math.random() * baseDefYThreshold );
        }
        return new Point2(x, y);
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

    // /**
    //  * A method to validate calculated x
    //  * @deprecated
    //  * @param x
    //  * @param y
    //  * @return
    //  */
    // private double validatedX(double x, double y)
    // {
    //     if( battleField.getSummonState(1) > BattleField.SUMMON_AREA_HALF )
    //     {
    //         if( battleField.getSummonState(1) == BattleField.SUMMON_AREA_RIGHT &&  y < (BattleField.HEIGHT * 0.5) )
    //             return CoreUtils.clamp(x, BattleField.WIDTH * 0.5 , 810);
    //         if( battleField.getSummonState(1) == BattleField.SUMMON_AREA_LEFT && y < (BattleField.HEIGHT * 0.5) )
    //             return CoreUtils.clamp(x, 150, BattleField.WIDTH * 0.5);
    //     }
    //     return CoreUtils.clamp(x, 150, 810);
    // }

    // /**
    //  * A method to validate calculated y
    //  * @deprecated
    //  * @param x
    //  * @param y
    //  * @return
    //  */
    // private double validatedY(double x, double y)
    // {
    //     // Summon in 1/3
    //     if( battleField.getSummonState(1) == BattleField.SUMMON_AREA_THIRD )
    //         return y > (BattleField.HEIGHT * 0.3) - 100 ? (BattleField.HEIGHT * 0.3) - 100 : y;
    //     // Summon in 1/2
    //     if( battleField.getSummonState(1) == BattleField.SUMMON_AREA_HALF )
    //         return y > (BattleField.HEIGHT * 0.5) - 100 ? (BattleField.HEIGHT * 0.5) - 100 : y;
    //     // Summon in 2/3 Left Half
    //     if( battleField.getSummonState(1) > BattleField.SUMMON_AREA_HALF )
    //       return y > (BattleField.HEIGHT * 0.6) - 100 ? (BattleField.HEIGHT * 0.6) - 100 : y;
    //     return (y > BattleField.HEIGHT * 0.3) ? BattleField.HEIGHT * 0.3 : y;
    // }

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
        if( LOG_ENABLED )
            battleRoom.trace(args);
    }
}