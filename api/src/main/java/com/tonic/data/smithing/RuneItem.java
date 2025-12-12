package com.tonic.data.smithing;

import com.tonic.services.pathfinder.requirements.QuestRequirement;
import com.tonic.services.pathfinder.requirements.Requirement;
import com.tonic.services.pathfinder.requirements.WorldRequirement;
import com.tonic.services.pathfinder.requirements.Requirements;
import com.tonic.services.pathfinder.requirements.SkillRequirement;

import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;

/**
 * Rune smithable items.
 */
public enum RuneItem implements SmithableItem {
    DAGGER("Rune dagger", 1, ItemID.RUNE_DAGGER, 1, InterfaceID.Smithing.DAGGER, new SkillRequirement(Skill.SMITHING, 85)),
    SWORD("Rune sword", 1, ItemID.RUNE_SWORD, 1, InterfaceID.Smithing.SWORD, new SkillRequirement(Skill.SMITHING, 89)),
    SCIMITAR("Rune scimitar", 2, ItemID.RUNE_SCIMITAR, 1, InterfaceID.Smithing.SCIMITAR, new SkillRequirement(Skill.SMITHING, 90)),
    LONGSWORD("Rune longsword", 2, ItemID.RUNE_LONGSWORD, 1, InterfaceID.Smithing.LONGSWORD, new SkillRequirement(Skill.SMITHING, 91)),
    TWO_HANDED_SWORD("Rune 2h sword", 3, ItemID.RUNE_2H_SWORD, 1, InterfaceID.Smithing._2H, new SkillRequirement(Skill.SMITHING, 99)),
    AXE("Rune axe", 1, ItemID.RUNE_AXE, 1, InterfaceID.Smithing.AXE, new SkillRequirement(Skill.SMITHING, 86)),
    MACE("Rune mace", 1, ItemID.RUNE_MACE, 1, InterfaceID.Smithing.MACE, new SkillRequirement(Skill.SMITHING, 87)),
    WARHAMMER("Rune warhammer", 3, ItemID.RUNE_WARHAMMER, 1, InterfaceID.Smithing.WARHAMMER, new SkillRequirement(Skill.SMITHING, 94)),
    BATTLEAXE("Rune battleaxe", 3, ItemID.RUNE_BATTLEAXE, 1, InterfaceID.Smithing.BATTLEAXE, new SkillRequirement(Skill.SMITHING, 95)),
    CLAWS("Rune claws", 2, ItemID.RUNE_CLAWS, 1, InterfaceID.Smithing.CLAWS, new SkillRequirement(Skill.SMITHING, 98), new QuestRequirement(Quest.DEATH_PLATEAU), new WorldRequirement(true)),
    CHAINBODY("Rune chainbody", 3, ItemID.RUNE_CHAINBODY, 1, InterfaceID.Smithing.CHAINBODY, new SkillRequirement(Skill.SMITHING, 96)),
    PLATELEGS("Rune platelegs", 3, ItemID.RUNE_PLATELEGS, 1, InterfaceID.Smithing.PLATELEGS, new SkillRequirement(Skill.SMITHING, 99)),
    PLATESKIRT("Rune plateskirt", 3, ItemID.RUNE_PLATESKIRT, 1, InterfaceID.Smithing.PLATESKIRT, new SkillRequirement(Skill.SMITHING, 99)),
    PLATEBODY("Rune platebody", 5, ItemID.RUNE_PLATEBODY, 1, InterfaceID.Smithing.PLATEBODY, new SkillRequirement(Skill.SMITHING, 99)),
    NAILS("Rune nails", 1, ItemID.NAILS_RUNE, 15, InterfaceID.Smithing.NAILS, new SkillRequirement(Skill.SMITHING, 89), new WorldRequirement(true)),
    MED_HELM("Rune med helm", 1, ItemID.RUNE_MED_HELM, 1, InterfaceID.Smithing.MEDHELM, new SkillRequirement(Skill.SMITHING, 88)),
    FULL_HELM("Rune full helm", 2, ItemID.RUNE_FULL_HELM, 1, InterfaceID.Smithing.FULLHELM, new SkillRequirement(Skill.SMITHING, 92)),
    SQ_SHIELD("Rune sq shield", 2, ItemID.RUNE_SQ_SHIELD, 1, InterfaceID.Smithing.SQUARESHIELD, new SkillRequirement(Skill.SMITHING, 93)),
    KITESHIELD("Rune kiteshield", 3, ItemID.RUNE_KITESHIELD, 1, InterfaceID.Smithing.KITESHIELD, new SkillRequirement(Skill.SMITHING, 97)),
    THROWING_KNIVES("Rune throwing knives", 1, ItemID.RUNE_KNIFE, 5, InterfaceID.Smithing.KNIVES, new SkillRequirement(Skill.SMITHING, 92), new WorldRequirement(true)),
    DART_TIPS("Rune dart tip", 1, ItemID.RUNE_DART_TIP, 10, InterfaceID.Smithing.DARTTIPS, new SkillRequirement(Skill.SMITHING, 89), new QuestRequirement(Quest.THE_TOURIST_TRAP), new WorldRequirement(true)),
    ARROWHEADS("Rune arrowtips", 1, ItemID.RUNE_ARROWHEADS, 15, InterfaceID.Smithing.ARROWHEADS, new SkillRequirement(Skill.SMITHING, 90), new WorldRequirement(true)),
    BOLTS("Rune bolts (unf)", 1, ItemID.XBOWS_CROSSBOW_BOLTS_RUNITE_UNFEATHERED, 10, InterfaceID.Smithing.BOLTS, new SkillRequirement(Skill.SMITHING, 88), new WorldRequirement(true)),
    LIMBS("Rune limbs", 1, ItemID.XBOWS_CROSSBOW_LIMBS_RUNITE, 1, InterfaceID.Smithing.LIMBS, new SkillRequirement(Skill.SMITHING, 91), new WorldRequirement(true)),
    JAVELIN_TIPS("Rune javelin tips", 1, ItemID.RUNE_JAVELIN_HEAD, 5, InterfaceID.Smithing.OTHER_1, new SkillRequirement(Skill.SMITHING, 91), new WorldRequirement(true)),
    ;

    private final String displayName;
    private final int barCount;
    private final int outputId;
    private final int outputQuantity;
    private final int interfaceID;
    private final Requirements requirements;

    RuneItem(String displayName, int barCount, int outputId, int outputQuantity, int interfaceID, Requirement... reqs) {
        this.displayName = displayName;
        this.barCount = barCount;
        this.outputId = outputId;
        this.outputQuantity = outputQuantity;
        this.interfaceID = interfaceID;
        if (reqs != null && reqs.length > 0) {
            this.requirements = new Requirements();
            this.requirements.addRequirements(reqs);
        } else {
            this.requirements = null;
        }
    }

    @Override
    public String getDisplayName() { return displayName; }

    @Override
    public int getBarCount() { return barCount; }

    @Override
    public int getOutputId() { return outputId; }

    @Override
    public int getOutputQuantity() { return outputQuantity; }

    @Override
    public int getInterfaceID() { return interfaceID; }

    @Override
    public Requirements getRequirements() { return requirements; }

    @Override
    public boolean canAccess() {
        return requirements == null || requirements.fulfilled();
    }
}
