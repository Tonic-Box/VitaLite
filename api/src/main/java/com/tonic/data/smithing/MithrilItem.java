package com.tonic.data.smithing;

import com.tonic.services.pathfinder.requirements.QuestRequirement;
import com.tonic.services.pathfinder.requirements.Requirement;
import com.tonic.services.pathfinder.requirements.Requirements;
import com.tonic.services.pathfinder.requirements.WorldRequirement;
import com.tonic.services.pathfinder.requirements.SkillRequirement;

import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;

/**
 * Mithril smithable items.
 */
public enum MithrilItem implements SmithableItem {
    DAGGER("Mithril dagger", 1, ItemID.MITHRIL_DAGGER, 1, InterfaceID.Smithing.DAGGER, new SkillRequirement(Skill.SMITHING, 50)),
    SWORD("Mithril sword", 1, ItemID.MITHRIL_SWORD, 1, InterfaceID.Smithing.SWORD, new SkillRequirement(Skill.SMITHING, 54)),
    SCIMITAR("Mithril scimitar", 2, ItemID.MITHRIL_SCIMITAR, 1, InterfaceID.Smithing.SCIMITAR, new SkillRequirement(Skill.SMITHING, 55)),
    LONGSWORD("Mithril longsword", 2, ItemID.MITHRIL_LONGSWORD, 1, InterfaceID.Smithing.LONGSWORD, new SkillRequirement(Skill.SMITHING, 56)),
    TWO_HANDED_SWORD("Mithril 2h sword", 3, ItemID.MITHRIL_2H_SWORD, 1, InterfaceID.Smithing._2H, new SkillRequirement(Skill.SMITHING, 64)),
    AXE("Mithril axe", 1, ItemID.MITHRIL_AXE, 1, InterfaceID.Smithing.AXE, new SkillRequirement(Skill.SMITHING, 51)),
    MACE("Mithril mace", 1, ItemID.MITHRIL_MACE, 1, InterfaceID.Smithing.MACE, new SkillRequirement(Skill.SMITHING, 52)),
    WARHAMMER("Mithril warhammer", 3, ItemID.MITHRIL_WARHAMMER, 1, InterfaceID.Smithing.WARHAMMER, new SkillRequirement(Skill.SMITHING, 59)),
    BATTLEAXE("Mithril battleaxe", 3, ItemID.MITHRIL_BATTLEAXE, 1, InterfaceID.Smithing.BATTLEAXE, new SkillRequirement(Skill.SMITHING, 60)),
    CLAWS("Mithril claws", 2, ItemID.MITHRIL_CLAWS, 1, InterfaceID.Smithing.CLAWS, new SkillRequirement(Skill.SMITHING, 63), new QuestRequirement(Quest.DEATH_PLATEAU), new WorldRequirement(true)),
    CHAINBODY("Mithril chainbody", 3, ItemID.MITHRIL_CHAINBODY, 1, InterfaceID.Smithing.CHAINBODY, new SkillRequirement(Skill.SMITHING, 61)),
    PLATELEGS("Mithril platelegs", 3, ItemID.MITHRIL_PLATELEGS, 1, InterfaceID.Smithing.PLATELEGS, new SkillRequirement(Skill.SMITHING, 66)),
    PLATESKIRT("Mithril plateskirt", 3, ItemID.MITHRIL_PLATESKIRT, 1, InterfaceID.Smithing.PLATESKIRT, new SkillRequirement(Skill.SMITHING, 66)),
    PLATEBODY("Mithril platebody", 5, ItemID.MITHRIL_PLATEBODY, 1, InterfaceID.Smithing.PLATEBODY, new SkillRequirement(Skill.SMITHING, 68)),
    NAILS("Mithril nails", 1, ItemID.NAILS_MITHRIL, 15, InterfaceID.Smithing.NAILS, new SkillRequirement(Skill.SMITHING, 54), new WorldRequirement(true)),
    MED_HELM("Mithril med helm", 1, ItemID.MITHRIL_MED_HELM, 1, InterfaceID.Smithing.MEDHELM, new SkillRequirement(Skill.SMITHING, 53)),
    FULL_HELM("Mithril full helm", 2, ItemID.MITHRIL_FULL_HELM, 1, InterfaceID.Smithing.FULLHELM, new SkillRequirement(Skill.SMITHING, 57)),
    SQ_SHIELD("Mithril sq shield", 2, ItemID.MITHRIL_SQ_SHIELD, 1, InterfaceID.Smithing.SQUARESHIELD, new SkillRequirement(Skill.SMITHING, 58)),
    KITESHIELD("Mithril kiteshield", 3, ItemID.MITHRIL_KITESHIELD, 1, InterfaceID.Smithing.KITESHIELD, new SkillRequirement(Skill.SMITHING, 62)),
    THROWING_KNIVES("Mithril throwing knives", 1, ItemID.MITHRIL_KNIFE, 5, InterfaceID.Smithing.KNIVES, new SkillRequirement(Skill.SMITHING, 57), new WorldRequirement(true)),
    DART_TIPS("Mithril dart tip", 1, ItemID.MITHRIL_DART_TIP, 10, InterfaceID.Smithing.DARTTIPS, new SkillRequirement(Skill.SMITHING, 54), new QuestRequirement(Quest.THE_TOURIST_TRAP), new WorldRequirement(true)),
    ARROWHEADS("Mithril arrowtips", 1, ItemID.MITHRIL_ARROWHEADS, 15, InterfaceID.Smithing.ARROWHEADS, new SkillRequirement(Skill.SMITHING, 55), new WorldRequirement(true)),
    BOLTS("Mithril bolts (unf)", 1, ItemID.XBOWS_CROSSBOW_BOLTS_MITHRIL_UNFEATHERED, 10, InterfaceID.Smithing.BOLTS, new SkillRequirement(Skill.SMITHING, 53), new WorldRequirement(true)),
    LIMBS("Mithril limbs", 1, ItemID.XBOWS_CROSSBOW_LIMBS_MITHRIL, 1, InterfaceID.Smithing.LIMBS, new SkillRequirement(Skill.SMITHING, 56), new WorldRequirement(true)),
    GRAPPLE_TIP("Mithril grapple tip", 1, ItemID.XBOWS_GRAPPLE_TIP_MITHRIL, 1, InterfaceID.Smithing.OTHER_1, new SkillRequirement(Skill.SMITHING, 59), new WorldRequirement(true)),
    JAVELIN_TIPS("Mithril javelin tips", 1, ItemID.MITHRIL_JAVELIN_HEAD, 5, InterfaceID.Smithing.OTHER_3, new SkillRequirement(Skill.SMITHING, 56), new WorldRequirement(true)),
    ;

    private final String displayName;
    private final int barCount;
    private final int outputId;
    private final int outputQuantity;
    private final int interfaceID;
    private final Requirements requirements;

    MithrilItem(String displayName, int barCount, int outputId, int outputQuantity, int interfaceID, Requirement... reqs) {
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
