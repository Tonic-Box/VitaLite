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
 * Iron smithable items.
 */
public enum IronItem implements SmithableItem {
    DAGGER("Iron dagger", 1, ItemID.IRON_DAGGER, 1, InterfaceID.Smithing.DAGGER, new SkillRequirement(Skill.SMITHING, 15)),
    SWORD("Iron sword", 1, ItemID.IRON_SWORD, 1, InterfaceID.Smithing.SWORD, new SkillRequirement(Skill.SMITHING, 19)),
    SCIMITAR("Iron scimitar", 2, ItemID.IRON_SCIMITAR, 1, InterfaceID.Smithing.SCIMITAR, new SkillRequirement(Skill.SMITHING, 20)),
    LONGSWORD("Iron longsword", 2, ItemID.IRON_LONGSWORD, 1, InterfaceID.Smithing.LONGSWORD, new SkillRequirement(Skill.SMITHING, 21)),
    TWO_HANDED_SWORD("Iron 2h sword", 3, ItemID.IRON_2H_SWORD, 1, InterfaceID.Smithing._2H, new SkillRequirement(Skill.SMITHING, 29)),
    AXE("Iron axe", 1, ItemID.IRON_AXE, 1, InterfaceID.Smithing.AXE, new SkillRequirement(Skill.SMITHING, 16)),
    MACE("Iron mace", 1, ItemID.IRON_MACE, 1, InterfaceID.Smithing.MACE, new SkillRequirement(Skill.SMITHING, 17)),
    WARHAMMER("Iron warhammer", 3, ItemID.IRON_WARHAMMER, 1, InterfaceID.Smithing.WARHAMMER, new SkillRequirement(Skill.SMITHING, 24)),
    BATTLEAXE("Iron battleaxe", 3, ItemID.IRON_BATTLEAXE, 1, InterfaceID.Smithing.BATTLEAXE, new SkillRequirement(Skill.SMITHING, 25)),
    CLAWS("Iron claws", 2, ItemID.IRON_CLAWS, 1, InterfaceID.Smithing.CLAWS, new SkillRequirement(Skill.SMITHING, 28), new QuestRequirement(Quest.DEATH_PLATEAU), new WorldRequirement(true)),
    CHAINBODY("Iron chainbody", 3, ItemID.IRON_CHAINBODY, 1, InterfaceID.Smithing.CHAINBODY, new SkillRequirement(Skill.SMITHING, 26)),
    PLATELEGS("Iron platelegs", 3, ItemID.IRON_PLATELEGS, 1, InterfaceID.Smithing.PLATELEGS, new SkillRequirement(Skill.SMITHING, 31)),
    PLATESKIRT("Iron plateskirt", 3, ItemID.IRON_PLATESKIRT, 1, InterfaceID.Smithing.PLATESKIRT, new SkillRequirement(Skill.SMITHING, 31)),
    PLATEBODY("Iron platebody", 5, ItemID.IRON_PLATEBODY, 1, InterfaceID.Smithing.PLATEBODY, new SkillRequirement(Skill.SMITHING, 33)),
    NAILS("Iron nails", 1, ItemID.NAILS_IRON, 15, InterfaceID.Smithing.NAILS, new SkillRequirement(Skill.SMITHING, 19), new WorldRequirement(true)),
    MED_HELM("Iron med helm", 1, ItemID.IRON_MED_HELM, 1, InterfaceID.Smithing.MEDHELM, new SkillRequirement(Skill.SMITHING, 18)),
    FULL_HELM("Iron full helm", 2, ItemID.IRON_FULL_HELM, 1, InterfaceID.Smithing.FULLHELM, new SkillRequirement(Skill.SMITHING, 22)),
    SQ_SHIELD("Iron sq shield", 2, ItemID.IRON_SQ_SHIELD, 1, InterfaceID.Smithing.SQUARESHIELD, new SkillRequirement(Skill.SMITHING, 23)),
    KITESHIELD("Iron kiteshield", 3, ItemID.IRON_KITESHIELD, 1, InterfaceID.Smithing.KITESHIELD, new SkillRequirement(Skill.SMITHING, 27)),
    THROWING_KNIVES("Iron throwing knives", 1, ItemID.IRON_KNIFE, 5, InterfaceID.Smithing.KNIVES, new SkillRequirement(Skill.SMITHING, 22), new WorldRequirement(true)),
    DART_TIPS("Iron dart tip", 1, ItemID.IRON_DART_TIP, 10, InterfaceID.Smithing.DARTTIPS, new SkillRequirement(Skill.SMITHING, 19), new QuestRequirement(Quest.THE_TOURIST_TRAP), new WorldRequirement(true)),
    ARROWHEADS("Iron arrowtips", 1, ItemID.IRON_ARROWHEADS, 15, InterfaceID.Smithing.ARROWHEADS, new SkillRequirement(Skill.SMITHING, 20), new WorldRequirement(true)),
    BOLTS("Iron bolts (unf)", 1, ItemID.XBOWS_CROSSBOW_BOLTS_IRON_UNFEATHERED, 10, InterfaceID.Smithing.BOLTS, new SkillRequirement(Skill.SMITHING, 18), new WorldRequirement(true)),
    LIMBS("Iron limbs", 1, ItemID.XBOWS_CROSSBOW_LIMBS_IRON, 1, InterfaceID.Smithing.LIMBS, new SkillRequirement(Skill.SMITHING, 23), new WorldRequirement(true)),
    JAVELIN_TIPS("Iron javelin tips", 1, ItemID.IRON_JAVELIN_HEAD, 5, InterfaceID.Smithing.OTHER_3, new SkillRequirement(Skill.SMITHING, 21), new WorldRequirement(true)),
    IRON_SPIT("Iron spit", 1, ItemID.SPIT_IRON, 1, InterfaceID.Smithing.OTHER_1, new SkillRequirement(Skill.SMITHING, 17), new WorldRequirement(true)),
    LAMP_FRAME("Lamp frame", 1, ItemID.OIL_LANTERN_FRAME, 1, InterfaceID.Smithing.OTHER_2, new SkillRequirement(Skill.SMITHING, 26), new WorldRequirement(true)),
    ;

    private final String displayName;
    private final int barCount;
    private final int outputId;
    private final int outputQuantity;
    private final int interfaceID;
    private final Requirements requirements;

    IronItem(String displayName, int barCount, int outputId, int outputQuantity, int interfaceID, Requirement... reqs) {
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
