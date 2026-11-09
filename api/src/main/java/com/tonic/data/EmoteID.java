package com.tonic.data;

import com.tonic.api.widgets.EmoteAPI;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.gameval.InterfaceID;

@AllArgsConstructor
@Getter
public enum EmoteID
{
    YES(0),
    NO(1),
    BOW(2),
    ANGRY(3),
    THINK(4),
    WAVE(5),
    SHRUG(6),
    CHEER(7),
    BECKON(8),
    LAUGH(9),
    JUMP_FOR_JOY(10),
    YAWN(11),
    DANCE(12),
    JIG(13),
    SPIN(14),
    HEADBANG(15),
    CRY(16),
    BLOW_KISS(17),
    PANIC(18),
    RASPBERRY(19),
    CLAP(20),
    SALUTE(21),
    GOBLIN_BOW(22),
    GOBLIN_SALUTE(23),
    GLASS_BOX(24),
    CLIMB_ROPE(25),
    LEAN(26),
    GLASS_WALL(27),
    IDEA(28),
    STOMP(29),
    FLAP(30),
    SLAP_HEAD(31),
    ZOMBIE_WALK(32),
    ZOMBIE_DANCE(33),
    SCARED(34),
    RABBIT_HOP(35),
    SIT_UP(36),
    PUSH_UP(37),
    STAR_JUMP(38),
    JOG(39),
    FLEX(40),
    ZOMBIE_HAND(41),
    HYPERMOBILE_DRINKER(42),
    SKILLCAPE(43),
    AIR_GUITAR(44),
    URI_TRANSFORM(45),
    SMOOTH_DANCE(46),
    CRAZY_DANCE(47),
    PREMIER_SHIELD(48),
    EXPLORE(49),
    RELIC_UNLOCK(50),
    PARTY(51),
    TRICK(52),
    FORTIS_SALUTE(53),
    SIT_DOWN(54),
    CRAB_DANCE(55),
    ;

    public static final int EMOTES_WIDGET_ID = InterfaceID.Emote.CONTENTS;
    private final int id;

    public void perform()
    {
        EmoteAPI.perform(this);
    }
}