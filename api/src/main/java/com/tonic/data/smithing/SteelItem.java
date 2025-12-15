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
 * Steel smithable items.
 */
public enum SteelItem implements SmithableItem {
    DAGGER("Steel dagger", 1, ItemID.STEEL_DAGGER, 1, InterfaceID.Smithing.DAGGER, new SkillRequirement(Skill.SMITHING, 30)),
    SWORD("Steel sword", 1, ItemID.STEEL_SWORD, 1, InterfaceID.Smithing.SWORD, new SkillRequirement(Skill.SMITHING, 34)),
    SCIMITAR("Steel scimitar", 2, ItemID.STEEL_SCIMITAR, 1, InterfaceID.Smithing.SCIMITAR, new SkillRequirement(Skill.SMITHING, 35)),
    LONGSWORD("Steel longsword", 2, ItemID.STEEL_LONGSWORD, 1, InterfaceID.Smithing.LONGSWORD, new SkillRequirement(Skill.SMITHING, 36)),
    TWO_HANDED_SWORD("Steel 2h sword", 3, ItemID.STEEL_2H_SWORD, 1, InterfaceID.Smithing._2H, new SkillRequirement(Skill.SMITHING, 44)),
    AXE("Steel axe", 1, ItemID.STEEL_AXE, 1, InterfaceID.Smithing.AXE, new SkillRequirement(Skill.SMITHING, 31)),
    MACE("Steel mace", 1, ItemID.STEEL_MACE, 1, InterfaceID.Smithing.MACE, new SkillRequirement(Skill.SMITHING, 32)),
    WARHAMMER("Steel warhammer", 3, ItemID.STEEL_WARHAMMER, 1, InterfaceID.Smithing.WARHAMMER, new SkillRequirement(Skill.SMITHING, 39)),
    BATTLEAXE("Steel battleaxe", 3, ItemID.STEEL_BATTLEAXE, 1, InterfaceID.Smithing.BATTLEAXE, new SkillRequirement(Skill.SMITHING, 40)),
    CLAWS("Steel claws", 2, ItemID.STEEL_CLAWS, 1, InterfaceID.Smithing.CLAWS, new SkillRequirement(Skill.SMITHING, 43), new QuestRequirement(Quest.DEATH_PLATEAU), new WorldRequirement(true)),
    CHAINBODY("Steel chainbody", 3, ItemID.STEEL_CHAINBODY, 1, InterfaceID.Smithing.CHAINBODY, new SkillRequirement(Skill.SMITHING, 41)),
    PLATELEGS("Steel platelegs", 3, ItemID.STEEL_PLATELEGS, 1, InterfaceID.Smithing.PLATELEGS, new SkillRequirement(Skill.SMITHING, 46)),
    PLATESKIRT("Steel plateskirt", 3, ItemID.STEEL_PLATESKIRT, 1, InterfaceID.Smithing.PLATESKIRT, new SkillRequirement(Skill.SMITHING, 46)),
    PLATEBODY("Steel platebody", 5, ItemID.STEEL_PLATEBODY, 1, InterfaceID.Smithing.PLATEBODY, new SkillRequirement(Skill.SMITHING, 48)),
    NAILS("Steel nails", 1, ItemID.NAILS, 15, InterfaceID.Smithing.NAILS, new SkillRequirement(Skill.SMITHING, 34)),
    MED_HELM("Steel med helm", 1, ItemID.STEEL_MED_HELM, 1, InterfaceID.Smithing.MEDHELM, new SkillRequirement(Skill.SMITHING, 33)),
    FULL_HELM("Steel full helm", 2, ItemID.STEEL_FULL_HELM, 1, InterfaceID.Smithing.FULLHELM, new SkillRequirement(Skill.SMITHING, 37)),
    SQ_SHIELD("Steel sq shield", 2, ItemID.STEEL_SQ_SHIELD, 1, InterfaceID.Smithing.SQUARESHIELD, new SkillRequirement(Skill.SMITHING, 38)),
    KITESHIELD("Steel kiteshield", 3, ItemID.STEEL_KITESHIELD, 1, InterfaceID.Smithing.KITESHIELD, new SkillRequirement(Skill.SMITHING, 42)),
    THROWING_KNIVES("Steel throwing knives", 1, ItemID.STEEL_KNIFE, 5, InterfaceID.Smithing.KNIVES, new SkillRequirement(Skill.SMITHING, 37), new WorldRequirement(true)),
    DART_TIPS("Steel dart tip", 1, ItemID.STEEL_DART_TIP, 10, InterfaceID.Smithing.DARTTIPS, new SkillRequirement(Skill.SMITHING, 34), new QuestRequirement(Quest.THE_TOURIST_TRAP), new WorldRequirement(true)),
    ARROWHEADS("Steel arrowtips", 1, ItemID.STEEL_ARROWHEADS, 15, InterfaceID.Smithing.ARROWHEADS, new SkillRequirement(Skill.SMITHING, 35), new WorldRequirement(true)),
    BOLTS("Steel bolts (unf)", 1, ItemID.XBOWS_CROSSBOW_BOLTS_STEEL_UNFEATHERED, 10, InterfaceID.Smithing.BOLTS, new SkillRequirement(Skill.SMITHING, 33), new WorldRequirement(true)),
    LIMBS("Steel limbs", 1, ItemID.XBOWS_CROSSBOW_LIMBS_STEEL, 1, InterfaceID.Smithing.LIMBS, new SkillRequirement(Skill.SMITHING, 36), new WorldRequirement(true)),
    STEEL_STUDS("Steel studs", 1, ItemID.STUDS, 1, InterfaceID.Smithing.OTHER_1, new SkillRequirement(Skill.SMITHING, 36), new WorldRequirement(true)),
    JAVELIN_TIPS("Steel javelin tips", 1, ItemID.STEEL_JAVELIN_HEAD, 5, InterfaceID.Smithing.OTHER_3, new SkillRequirement(Skill.SMITHING, 36), new WorldRequirement(true)),
    STEEL_CHAIN("Steel chain", 1, ItemID.CHAIN, 1, InterfaceID.Smithing.OTHER_5, new SkillRequirement(Skill.SMITHING, 34), new WorldRequirement(true)),
    ;

    private final String displayName;
    private final int barCount;
    private final int outputId;
    private final int outputQuantity;
    private final int interfaceID;
    private final Requirements requirements;

    SteelItem(String displayName, int barCount, int outputId, int outputQuantity, int interfaceID, Requirement... reqs) {
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
