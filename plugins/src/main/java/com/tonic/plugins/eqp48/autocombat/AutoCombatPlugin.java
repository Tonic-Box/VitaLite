package com.tonic.plugins.eqp48.autocombat;

import com.google.inject.Provides;
import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.entities.NpcAPI;
import com.tonic.api.game.CombatAPI;
import com.tonic.api.game.SkillAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.data.wrappers.ActorEx;
import com.tonic.data.wrappers.ItemEx;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.data.wrappers.TileItemEx;
import com.tonic.plugins.eqp48.autocombat.enums.LootMode;
import com.tonic.plugins.eqp48.autocombat.enums.ValueType;
import com.tonic.queries.TileItemQuery;
import com.tonic.util.Distance;
import com.tonic.util.VitaPlugin;
import com.tonic.plugins.eqp48.autocombat.utils.Filters;
import com.tonic.plugins.eqp48.autocombat.utils.LootValue;
import com.tonic.plugins.eqp48.autocombat.utils.LootValues;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.TileItem;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@PluginDescriptor(
	name = "# Auto Combat",
	description = "A simple combat plugin with target marking.",
	tags = {"combat", "looting", "eqp48"}
)
public class AutoCombatPlugin extends VitaPlugin
{
	private static final String MARK_OPTION = "Mark target";
	private static final String UNMARK_OPTION = "Unmark target";
	private static final String MARK_OPTION_COLORED = "<col=0044bb>Mark target</col>";
	private static final String UNMARK_OPTION_COLORED = "<col=0044bb>Unmark target</col>";

	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private AutoCombatHullOverlay hullOverlay;

	@Inject
	private AutoCombatConfig config;

	private final Set<Integer> markedNpcIndexes = ConcurrentHashMap.newKeySet();
	private volatile boolean stop;
	private boolean clearingBonesAshes;

	@Getter
	private String status = "Idle";

	@Getter
	private String targetName = "";

	@Provides
	AutoCombatConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AutoCombatConfig.class);
	}

	@Override
	protected void startUp()
	{
		stop = false;
		reset();
		overlayManager.add(hullOverlay);
	}

	@Override
	protected void shutDown()
	{
		stop = true;
		overlayManager.remove(hullOverlay);
		haltLoop(() -> stop = true);
		reset();
	}

	@Override
	public void loop()
	{
		try
		{
			runMainLoop();
		}
		catch (Throwable t)
		{
			Logger.error(t, "[AutoCombat] Unhandled error: %e");
			setStatus("Error", "");
		}
	}

	private void runMainLoop()
	{
		if (stop)
		{
			return;
		}

		if (!isReady())
		{
			reset();
			return;
		}

		setStatus("Idle", "");
		boolean handled = false;

		if (tryHealing())
		{
			handled = true;
		}

		boolean wantsToClearBones = configClearBonesEnabled() && InventoryAPI.isFull() && hasBonesOrAshes();
		boolean shouldClear = clearingBonesAshes || wantsToClearBones;
		if (shouldClear)
		{
			if (!handled && tryBonesAndAshes())
			{
				clearingBonesAshes = hasBonesOrAshes();
				return;
			}

			if (!hasBonesOrAshes())
			{
				clearingBonesAshes = false;
			}
			else if (!handled)
			{
				clearingBonesAshes = true;
				return;
			}
		}

		if (config.prioritizeLooting())
		{
			if (tryLooting())
			{
				return;
			}

			if (tryFighting())
			{
				return;
			}
		}
		else
		{
			if (tryFighting())
			{
				return;
			}

			if (tryLooting())
			{
				return;
			}
		}

		if (!handled)
		{
			tryBonesAndAshes();
		}
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			reset();
		}
	}

	@Subscribe
	private void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!isReady())
		{
			return;
		}

		MenuAction action = MenuAction.of(event.getType());
		if (action != MenuAction.EXAMINE_NPC)
		{
			return;
		}

		NPC npc = getNpcFromIdentifier(event.getIdentifier());
		if (npc == null || !isTargetableNpc(npc))
		{
			return;
		}

		boolean isMarked = isMarked(npc);
		MenuEntry markEntry = client.createMenuEntry(client.getMenuEntries().length)
			.setOption(isMarked ? UNMARK_OPTION_COLORED : MARK_OPTION_COLORED)
			.setTarget(event.getTarget())
			.setIdentifier(event.getIdentifier())
			.setParam0(event.getActionParam0())
			.setParam1(event.getActionParam1())
			.setType(MenuAction.RUNELITE)
			.setDeprioritized(true);

		MenuEntry[] entries = client.getMenuEntries();
		if (entries.length > 0 && entries[entries.length - 1] != markEntry)
		{
			List<MenuEntry> reordered = new java.util.ArrayList<>(List.of(entries));
			reordered.remove(markEntry);
			reordered.add(markEntry);
			client.setMenuEntries(reordered.toArray(new MenuEntry[0]));
		}
	}

	@Subscribe
	private void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (isMarkEntry(event))
		{
			event.consume();
			NPC npc = getNpcFromIdentifier(event.getId());
			toggleMark(npc);
		}
	}

	List<NPC> getMarkedNpcs()
	{
		return client.getNpcs().stream()
			.filter(this::isMarked)
			.collect(Collectors.toList());
	}

	private boolean tryHealing()
	{
		if (config.eatAtHpPercent() > 0)
		{
			Double hpPercent = getPercent(
				SkillAPI.getBoostedLevel(Skill.HITPOINTS),
				SkillAPI.getLevel(Skill.HITPOINTS));
			if (hpPercent != null && hpPercent <= config.eatAtHpPercent())
			{
				ItemEx food = findInventoryItem(Filters.fromCsv(config.foodNames()), "Eat");
				if (food != null)
				{
					food.interact("Eat");
					setStatus("Healing (eat)", food.getName());
					return true;
				}
			}
		}

		if (config.restorePrayer() && config.prayerAtPercent() > 0)
		{
			Double prayerPercent = getPercent(
				SkillAPI.getBoostedLevel(Skill.PRAYER),
				SkillAPI.getLevel(Skill.PRAYER));
			if (prayerPercent != null && prayerPercent <= config.prayerAtPercent())
			{
				ItemEx potion = InventoryAPI.search()
					.withAction("Drink")
					.keepIf(item -> isPrayerRestoreName(item.getName()))
					.first();
				if (potion != null)
				{
					potion.interact("Drink");
					setStatus("Healing (prayer)", potion.getName());
					return true;
				}
			}
		}

		return false;
	}

	private boolean tryFighting()
	{
		Filters npcFilters = Filters.fromCsv(config.npcNames());
		if (npcFilters.isEmpty())
		{
			return false;
		}

		Player local = Static.invoke(() -> client.getLocalPlayer());
		if (local == null)
		{
			return false;
		}

		WorldPoint playerWp = Static.invoke(local::getWorldLocation);
		List<NpcEx> candidates = NpcAPI.search()
			.keepIf(n -> markedNpcIndexes.contains(n.getIndex()))
			.keepIf(npcFilters::matches)
			.keepIf(n -> !n.isDead())
			.keepIf(n -> !config.avoidTargetsInCombat() || isAttackable(n))
			.sortNearest(playerWp)
			.collect();

		if (candidates.isEmpty())
		{
			return false;
		}

		NpcEx target = candidates.get(0);
		if (target == null)
		{
			return false;
		}

		if (isAlreadyTargeting(target))
		{
			return false;
		}

		if (maybeToggleSpec())
		{
			setStatus("Enable special attack", "Special attack");
			return true;
		}

		target.interact("Attack");
		setStatus("Fighting", target.getName());
		return true;
	}

	private boolean tryLooting()
	{
		if (!config.prioritizeLooting() && isPlayerInCombat())
		{
			return false;
		}

		Player local = Static.invoke(() -> client.getLocalPlayer());
		if (local == null)
		{
			return false;
		}

		WorldPoint playerWp = Static.invoke(local::getWorldLocation);
		if (playerWp == null)
		{
			return false;
		}

		if (config.lootMode() == LootMode.BY_NAME)
		{
			return tryLootByName(playerWp);
		}

		return tryLootByValue(playerWp);
	}

	private boolean tryLootByName(WorldPoint playerWp)
	{
		Filters lootFilters = Filters.fromCsv(config.lootNames());
		if (lootFilters.isEmpty())
		{
			return false;
		}

		TileItemEx target = new TileItemQuery()
			.keepIf(this::passesOwnershipFilter)
			.keepIf(lootFilters::matches)
			.keepIf(item -> withinLootDistance(playerWp, item.getWorldPoint()))
			.sort((a, b) -> Integer.compare(
				Distance.chebyshev(playerWp, a.getWorldPoint()),
				Distance.chebyshev(playerWp, b.getWorldPoint())))
			.first();

		return loot(target, "Looting (by name)");
	}

	private boolean tryLootByValue(WorldPoint playerWp)
	{
		int minValue = config.minValue();
		ValueType valueType = config.valueType();

		List<TileItemEx> candidates = new TileItemQuery()
			.keepIf(this::passesOwnershipFilter)
			.keepIf(item -> withinLootDistance(playerWp, item.getWorldPoint()))
			.collect();

		TileItemEx target = candidates.stream()
			.map(item -> new LootValue(item, LootValues.compute(item, valueType)))
			.filter(pair -> pair.getValue() >= minValue)
			.max(Comparator
				.comparingLong(LootValue::getValue)
				.thenComparingInt(pair -> Distance.chebyshev(playerWp, pair.getItem().getWorldPoint())))
			.map(LootValue::getItem)
			.orElse(null);

		return loot(target, "Looting (by value)");
	}

	private boolean loot(TileItemEx target, String statusLabel)
	{
		if (target == null)
		{
			return false;
		}

		target.interact("Take");
		setStatus(statusLabel, target.getName());
		return true;
	}

	private boolean tryBonesAndAshes()
	{
		if (config.buryBones())
		{
			ItemEx bones = InventoryAPI.search()
				.withAction("Bury")
				.first();
			if (bones != null)
			{
				bones.interact("Bury");
				setStatus("Bury bones", bones.getName());
				return true;
			}
		}

		if (config.scatterAshes())
		{
			ItemEx ashes = InventoryAPI.search()
				.withAction("Scatter")
				.first();
			if (ashes != null)
			{
				ashes.interact("Scatter");
				setStatus("Scatter ashes", ashes.getName());
				return true;
			}
		}

		return false;
	}

	private boolean isReady()
	{
		return client != null
			&& Static.invoke(() -> client.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null);
	}

	private boolean withinLootDistance(WorldPoint player, WorldPoint target)
	{
		if (player == null || target == null || player.getPlane() != target.getPlane())
		{
			return false;
		}

		return Distance.chebyshev(player, target) <= config.lootDistance();
	}

	private boolean isTargetableNpc(NPC npc)
	{
		Filters npcFilters = Filters.fromCsv(config.npcNames());
		if (npcFilters.isEmpty())
		{
			return false;
		}

		return npc.getName() != null && npcFilters.matches(new NpcEx(npc));
	}

	private boolean isMarked(NPC npc)
	{
		return npc != null && markedNpcIndexes.contains(npc.getIndex());
	}

	private void toggleMark(NPC npc)
	{
		if (npc == null)
		{
			return;
		}

		int index = npc.getIndex();
		if (index < 0)
		{
			return;
		}

		if (markedNpcIndexes.contains(index))
		{
			markedNpcIndexes.remove(index);
		}
		else
		{
			markedNpcIndexes.add(index);
		}
	}

	private NPC getNpcFromIdentifier(int identifier)
	{
		return Static.invoke(() -> client.getNpcs().stream()
			.filter(n -> n != null && n.getIndex() == identifier)
			.findFirst()
			.orElse(null));
	}

	private boolean isMarkEntry(MenuOptionClicked event)
	{
		if (event.getMenuAction() != MenuAction.RUNELITE)
		{
			return false;
		}

		String option = event.getMenuOption();
		if (option == null)
		{
			return false;
		}

		option = Text.removeTags(option);
		return MARK_OPTION.equals(option) || UNMARK_OPTION.equals(option);
	}

	private boolean isPlayerInCombat()
	{
		return Static.invoke(() -> {
			Player lp = client.getLocalPlayer();
			return lp != null && lp.getInteracting() != null;
		});
	}

	private boolean hasBonesOrAshes()
	{
		return InventoryAPI.search().withAction("Bury").first() != null
			|| InventoryAPI.search().withAction("Scatter").first() != null;
	}

	private void setStatus(String newStatus, String newTarget)
	{
		status = newStatus;
		targetName = newTarget == null ? "" : Text.removeTags(newTarget);
	}

	private void reset()
	{
		clearingBonesAshes = false;
		markedNpcIndexes.clear();
		setStatus("Idle", "");
	}

	private boolean isAlreadyTargeting(NpcEx target)
	{
		if (target == null)
		{
			return false;
		}

		return Static.invoke(() -> {
			Player lp = client.getLocalPlayer();
			if (lp == null)
			{
				return false;
			}
			return target.getNpc().equals(lp.getInteracting()) && !target.isDead();
		});
	}

	private boolean isAttackable(NpcEx npc)
	{
		ActorEx<?> interacting = npc.getInteracting();
		return Static.invoke(() -> {
			Player lp = client.getLocalPlayer();
			return interacting == null || (lp != null && interacting.getActor() == lp);
		});
	}

	private boolean maybeToggleSpec()
	{
		int threshold = config.specThreshold();
		if (threshold <= 0)
		{
			return false;
		}

		if (CombatAPI.isSpecEnabled())
		{
			return false;
		}

		if (CombatAPI.getSpecEnergy() < threshold)
		{
			return false;
		}

		CombatAPI.toggleSpec();
		return true;
	}

	private ItemEx findInventoryItem(Filters filters, String requiredAction)
	{
		if (filters.isEmpty())
		{
			return null;
		}

		return InventoryAPI.search()
			.withAction(requiredAction)
			.keepIf(filters::matches)
			.first();
	}

	private boolean passesOwnershipFilter(TileItemEx item)
	{
		if (!config.lootOnlyOwned())
		{
			return true;
		}

		TileItem tileItem = item.getItem();
		return tileItem != null && tileItem.getOwnership() == TileItem.OWNERSHIP_SELF;
	}

	private Double getPercent(int current, int max)
	{
		if (max <= 0)
		{
			return null;
		}
		return (current * 100.0d) / max;
	}

	private boolean isPrayerRestoreName(String name)
	{
		if (name == null)
		{
			return false;
		}

		String lower = name.toLowerCase(Locale.ENGLISH);
		return lower.contains("prayer") || lower.contains("restore") || lower.contains("sanfew");
	}

	private boolean configClearBonesEnabled()
	{
		return config.buryBones() || config.scatterAshes();
	}

}
