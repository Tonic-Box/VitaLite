package com.tonic.data.smithing;

import com.tonic.services.pathfinder.requirements.*;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;

/**
 * Bronze smithable items.
 */
public enum BronzeItem implements SmithableItem {
    DAGGER("Bronze dagger", 1, ItemID.BRONZE_DAGGER, 1, InterfaceID.Smithing.DAGGER, new SkillRequirement(Skill.SMITHING, 1)),
    SWORD("Bronze sword", 1, ItemID.BRONZE_SWORD, 1, InterfaceID.Smithing.SWORD, new SkillRequirement(Skill.SMITHING, 4)),
    SCIMITAR("Bronze scimitar", 2, ItemID.BRONZE_SCIMITAR, 1, InterfaceID.Smithing.SCIMITAR, new SkillRequirement(Skill.SMITHING, 5)),
    LONGSWORD("Bronze longsword", 2, ItemID.BRONZE_LONGSWORD, 1, InterfaceID.Smithing.LONGSWORD, new SkillRequirement(Skill.SMITHING, 6)),
    TWO_HANDED_SWORD("Bronze 2h sword", 3, ItemID.BRONZE_2H_SWORD, 1, InterfaceID.Smithing._2H, new SkillRequirement(Skill.SMITHING, 14)),
    AXE("Bronze axe", 1, ItemID.BRONZE_AXE, 1, InterfaceID.Smithing.AXE, new SkillRequirement(Skill.SMITHING, 1)),
    MACE("Bronze mace", 1, ItemID.BRONZE_MACE, 1, InterfaceID.Smithing.MACE, new SkillRequirement(Skill.SMITHING, 2)),
    WARHAMMER("Bronze warhammer", 3, ItemID.BRONZE_WARHAMMER, 1, InterfaceID.Smithing.WARHAMMER, new SkillRequirement(Skill.SMITHING, 9)),
    BATTLEAXE("Bronze battleaxe", 3, ItemID.BRONZE_BATTLEAXE, 1, InterfaceID.Smithing.BATTLEAXE, new SkillRequirement(Skill.SMITHING, 10)),
    CLAWS("Bronze claws", 2, ItemID.BRONZE_CLAWS, 1, InterfaceID.Smithing.CLAWS, new SkillRequirement(Skill.SMITHING, 13), new QuestRequirement(Quest.DEATH_PLATEAU), new WorldRequirement(true)),
    CHAINBODY("Bronze chainbody", 3, ItemID.BRONZE_CHAINBODY, 1, InterfaceID.Smithing.CHAINBODY, new SkillRequirement(Skill.SMITHING, 11)),
    PLATELEGS("Bronze platelegs", 3, ItemID.BRONZE_PLATELEGS, 1, InterfaceID.Smithing.PLATELEGS, new SkillRequirement(Skill.SMITHING, 16)),
    PLATESKIRT("Bronze plateskirt", 3, ItemID.BRONZE_PLATESKIRT, 1, InterfaceID.Smithing.PLATESKIRT, new SkillRequirement(Skill.SMITHING, 16)),
    PLATEBODY("Bronze platebody", 5, ItemID.BRONZE_PLATEBODY, 1, InterfaceID.Smithing.PLATEBODY, new SkillRequirement(Skill.SMITHING, 18)),
    NAILS("Bronze nails", 1, ItemID.NAILS_BRONZE, 15, InterfaceID.Smithing.NAILS, new SkillRequirement(Skill.SMITHING, 4), new WorldRequirement(true)),
    MED_HELM("Bronze med helm", 1, ItemID.BRONZE_MED_HELM, 1, InterfaceID.Smithing.MEDHELM, new SkillRequirement(Skill.SMITHING, 3)),
    FULL_HELM("Bronze full helm", 2, ItemID.BRONZE_FULL_HELM, 1, InterfaceID.Smithing.FULLHELM, new SkillRequirement(Skill.SMITHING, 7)),
    SQ_SHIELD("Bronze sq shield", 2, ItemID.BRONZE_SQ_SHIELD, 1, InterfaceID.Smithing.SQUARESHIELD, new SkillRequirement(Skill.SMITHING, 8)),
    KITESHIELD("Bronze kiteshield", 3, ItemID.BRONZE_KITESHIELD, 1, InterfaceID.Smithing.KITESHIELD, new SkillRequirement(Skill.SMITHING, 12)),
    THROWING_KNIVES("Bronze throwing knives", 1, ItemID.BRONZE_KNIFE, 5, InterfaceID.Smithing.KNIVES, new SkillRequirement(Skill.SMITHING, 7), new WorldRequirement(true)),
    DART_TIPS("Bronze dart tip", 1, ItemID.BRONZE_DART_TIP, 10, InterfaceID.Smithing.DARTTIPS, new SkillRequirement(Skill.SMITHING, 4), new QuestRequirement(Quest.THE_TOURIST_TRAP), new WorldRequirement(true)),
    ARROWHEADS("Bronze arrowtips", 1, ItemID.BRONZE_ARROWHEADS, 15, InterfaceID.Smithing.ARROWHEADS, new SkillRequirement(Skill.SMITHING, 5), new WorldRequirement(true)),
    BOLTS("Bronze bolts (unf)", 1, ItemID.XBOWS_CROSSBOW_BOLTS_BRONZE_UNFEATHERED, 10, InterfaceID.Smithing.BOLTS, new SkillRequirement(Skill.SMITHING, 3), new WorldRequirement(true)),
    LIMBS("Bronze limbs", 1, ItemID.XBOWS_CROSSBOW_LIMBS_BRONZE, 1, InterfaceID.Smithing.LIMBS, new SkillRequirement(Skill.SMITHING, 6), new WorldRequirement(true)),
    WIRE("Bronze wire", 1, ItemID.BRONZECRAFTWIRE, 1, InterfaceID.Smithing.OTHER_1, new SkillRequirement(Skill.SMITHING, 4), new WorldRequirement(true)),
    JAVELIN_TIPS("Bronze javelin heads", 1, ItemID.BRONZE_JAVELIN_HEAD, 5, InterfaceID.Smithing.OTHER_3, new SkillRequirement(Skill.SMITHING, 6), new WorldRequirement(true))
    ;

    private final String displayName;
    private final int barCount;
    private final int outputId;
    private final int outputQuantity;
    private final int interfaceID;
    private final Requirements requirements;

    BronzeItem(String displayName, int barCount, int outputId, int outputQuantity, int interfaceID, Requirement... reqs) {
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

