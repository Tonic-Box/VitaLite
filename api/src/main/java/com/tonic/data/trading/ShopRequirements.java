package com.tonic.data.trading;

import com.tonic.api.game.SkillAPI;
import com.tonic.services.pathfinder.requirements.*;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

/**
 * Shared shop access requirements used across multiple shops.
 */
public final class ShopRequirements {
    private ShopRequirements() {}

    /**
     * Warriors' Guild access requirement.
     * Requires either 130 combined Attack and Strength levels,
     * or level 99 in Attack or Strength.
     */
    public static final OtherRequirement WARRIOR_GUILD_REQ =
            new OtherRequirement(() -> {
                int attack = SkillAPI.getLevel(Skill.ATTACK);
                int strength = SkillAPI.getLevel(Skill.STRENGTH);
                return (attack + strength >= 130) ||
                        (attack >= 99) ||
                        (strength >= 99);
            });

    /**
     * Kourend visited requirement.
     * Checks if the player has visited Great Kourend.
     */
    public static final VarRequirement KOUREND_VISITED_REQ =
            new VarRequirement(
                    Comparison.GREATER_THAN_EQUAL,
                    VarType.VARBIT,
                    VarbitID.ZEAH_PLAYERHASVISITED,
                    1
            );

    /**
     * Ghostspeak Amulet or equivalent item requirement.
     * Required to communicate with ghost NPCs in Morytania.
     * Accepts Amulet of ghostspeak or any tier of Morytania legs.
     */
    public static final ItemRequirement GHOSTSPEAK_REQ =
            new ItemRequirement(
                    null,
                    1,
                    ItemID.AMULET_OF_GHOSTSPEAK,
                    ItemID.MORYTANIA_LEGS_MEDIUM,
                    ItemID.MORYTANIA_LEGS_HARD,
                    ItemID.MORYTANIA_LEGS_ELITE
            );

    /**
     * Creates a quest points requirement.
     *
     * @param amount the minimum number of quest points required
     * @return a VarRequirement for the specified quest point threshold
     */
    public static VarRequirement questPointsReq(int amount) {
        return new VarRequirement(
                Comparison.GREATER_THAN_EQUAL,
                VarType.VARP,
                VarPlayerID.QP,
                amount
        );
    }
}