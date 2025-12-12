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
 * Adamant smithable items.
 */
public enum AdamantItem implements SmithableItem {
    DAGGER("Adamant dagger", 1, ItemID.ADAMANT_DAGGER, 1, InterfaceID.Smithing.DAGGER, new SkillRequirement(Skill.SMITHING, 70)),
    SWORD("Adamant sword", 1, ItemID.ADAMANT_SWORD, 1, InterfaceID.Smithing.SWORD, new SkillRequirement(Skill.SMITHING, 74)),
    SCIMITAR("Adamant scimitar", 2, ItemID.ADAMANT_SCIMITAR, 1, InterfaceID.Smithing.SCIMITAR, new SkillRequirement(Skill.SMITHING, 75)),
    LONGSWORD("Adamant longsword", 2, ItemID.ADAMANT_LONGSWORD, 1, InterfaceID.Smithing.LONGSWORD, new SkillRequirement(Skill.SMITHING, 76)),
    TWO_HANDED_SWORD("Adamant 2h sword", 3, ItemID.ADAMANT_2H_SWORD, 1, InterfaceID.Smithing._2H, new SkillRequirement(Skill.SMITHING, 84)),
    AXE("Adamant axe", 1, ItemID.ADAMANT_AXE, 1, InterfaceID.Smithing.AXE, new SkillRequirement(Skill.SMITHING, 71)),
    MACE("Adamant mace", 1, ItemID.ADAMANT_MACE, 1, InterfaceID.Smithing.MACE, new SkillRequirement(Skill.SMITHING, 72)),
    WARHAMMER("Adamant warhammer", 3, ItemID.ADAMNT_WARHAMMER, 1, InterfaceID.Smithing.WARHAMMER, new SkillRequirement(Skill.SMITHING, 79)),
    BATTLEAXE("Adamant battleaxe", 3, ItemID.ADAMANT_BATTLEAXE, 1, InterfaceID.Smithing.BATTLEAXE, new SkillRequirement(Skill.SMITHING, 80)),
    CLAWS("Adamant claws", 2, ItemID.ADAMANT_CLAWS, 1, InterfaceID.Smithing.CLAWS, new SkillRequirement(Skill.SMITHING, 83), new QuestRequirement(Quest.DEATH_PLATEAU), new WorldRequirement(true)),
    CHAINBODY("Adamant chainbody", 3, ItemID.ADAMANT_CHAINBODY, 1, InterfaceID.Smithing.CHAINBODY, new SkillRequirement(Skill.SMITHING, 81)),
    PLATELEGS("Adamant platelegs", 3, ItemID.ADAMANT_PLATELEGS, 1, InterfaceID.Smithing.PLATELEGS, new SkillRequirement(Skill.SMITHING, 86)),
    PLATESKIRT("Adamant plateskirt", 3, ItemID.ADAMANT_PLATESKIRT, 1, InterfaceID.Smithing.PLATESKIRT, new SkillRequirement(Skill.SMITHING, 86)),
    PLATEBODY("Adamant platebody", 5, ItemID.ADAMANT_PLATEBODY, 1, InterfaceID.Smithing.PLATEBODY, new SkillRequirement(Skill.SMITHING, 88)),
    NAILS("Adamant nails", 1, ItemID.NAILS_ADAMANT, 15, InterfaceID.Smithing.NAILS, new SkillRequirement(Skill.SMITHING, 74), new WorldRequirement(true)),
    MED_HELM("Adamant med helm", 1, ItemID.ADAMANT_MED_HELM, 1, InterfaceID.Smithing.MEDHELM, new SkillRequirement(Skill.SMITHING, 73)),
    FULL_HELM("Adamant full helm", 2, ItemID.ADAMANT_FULL_HELM, 1, InterfaceID.Smithing.FULLHELM, new SkillRequirement(Skill.SMITHING, 77)),
    SQ_SHIELD("Adamant sq shield", 2, ItemID.ADAMANT_SQ_SHIELD, 1, InterfaceID.Smithing.SQUARESHIELD, new SkillRequirement(Skill.SMITHING, 78)),
    KITESHIELD("Adamant kiteshield", 3, ItemID.ADAMANT_KITESHIELD, 1, InterfaceID.Smithing.KITESHIELD, new SkillRequirement(Skill.SMITHING, 82)),
    THROWING_KNIVES("Adamant throwing knives", 1, ItemID.ADAMANT_KNIFE, 5, InterfaceID.Smithing.KNIVES, new SkillRequirement(Skill.SMITHING, 77), new WorldRequirement(true)),
    DART_TIPS("Adamant dart tip", 1, ItemID.ADAMANT_DART_TIP, 10, InterfaceID.Smithing.DARTTIPS, new SkillRequirement(Skill.SMITHING, 74), new QuestRequirement(Quest.THE_TOURIST_TRAP), new WorldRequirement(true)),
    ARROWHEADS("Adamant arrowtips", 1, ItemID.ADAMANT_ARROWHEADS, 15, InterfaceID.Smithing.ARROWHEADS, new SkillRequirement(Skill.SMITHING, 75), new WorldRequirement(true)),
    BOLTS("Adamant bolts (unf)", 1, ItemID.XBOWS_CROSSBOW_BOLTS_ADAMANTITE_UNFEATHERED, 10, InterfaceID.Smithing.BOLTS, new SkillRequirement(Skill.SMITHING, 73), new WorldRequirement(true)),
    LIMBS("Adamant limbs", 1, ItemID.XBOWS_CROSSBOW_LIMBS_ADAMANTITE, 1, InterfaceID.Smithing.LIMBS, new SkillRequirement(Skill.SMITHING, 76), new WorldRequirement(true)),
    JAVELIN_TIPS("Adamant javelin tips", 1, ItemID.ADAMANT_JAVELIN_HEAD, 5, InterfaceID.Smithing.OTHER_1, new SkillRequirement(Skill.SMITHING, 76), new WorldRequirement(true)),
    ;

    private final String displayName;
    private final int barCount;
    private final int outputId;
    private final int outputQuantity;
    private final int interfaceID;
    private final Requirements requirements;

    AdamantItem(String displayName, int barCount, int outputId, int outputQuantity, int interfaceID, Requirement... reqs) {
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
