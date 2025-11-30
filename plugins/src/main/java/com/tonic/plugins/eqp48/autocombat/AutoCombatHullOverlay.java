package com.tonic.plugins.eqp48.autocombat;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.NPC;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

class AutoCombatHullOverlay extends Overlay
{
	private static final Color HULL_FILL = new Color(0, 68, 187, 40);
	private static final Color HULL_OUTLINE = new Color(0, 68, 187, 180);
	private static final BasicStroke HULL_STROKE = new BasicStroke(2f);

	private final AutoCombatPlugin plugin;

	@Inject
	AutoCombatHullOverlay(AutoCombatPlugin plugin)
	{
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(OverlayPriority.HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		for (NPC npc : plugin.getMarkedNpcs())
		{
			if (npc == null || npc.isDead() || npc.getConvexHull() == null)
			{
				continue;
			}

			OverlayUtil.renderPolygon(graphics, npc.getConvexHull(), HULL_OUTLINE, HULL_FILL, HULL_STROKE);
		}

		return null;
	}
}
