package com.tonic.queries;

import com.tonic.Static;
import com.tonic.queries.abstractions.AbstractQuery;
import com.tonic.services.GameManager;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import org.apache.commons.lang3.ArrayUtils;

import java.util.*;

/**
 * A query class to filter and find widgets in the game client.
 */
public class WidgetQuery extends AbstractQuery<Widget, WidgetQuery>
{
    /**
     * Initializes the query with all widgets from the client.
     */
    public WidgetQuery() {
        super(GameManager.widgetList());
    }

    public WidgetQuery(Widget... roots) {
        super(GameManager.widgetList(roots));
    }

    public WidgetQuery(int... roots) {
        super(GameManager.widgetList(roots));
    }

    /**
     * Initializes the query with a provided collection of widgets.
     * @param cache A collection of widgets to initialize the query with.
     */
    public WidgetQuery(List<Widget> cache) {
        super(new ArrayList<>(cache));
    }

    /**
     * Initializes the query with a provided set of widgets.
     * @param cache A set of widgets to initialize the query with.
     */
    public WidgetQuery(HashSet<Widget> cache) {
        super(new ArrayList<>(cache));
    }

    public WidgetQuery withName(String name)
    {
        return removeIf(w -> w.getName() == null || !w.getName().equalsIgnoreCase(name));
    }

    /**
     * Filters widgets by their IDs.
     * @param id One or more widget IDs to filter by.
     * @return WidgetQuery
     */
    public WidgetQuery withId(int... id)
    {
        return removeIf(w -> !ArrayUtils.contains(id, w.getId()));
    }

    /**
     * Filters widgets by their item IDs.
     * @param itemId One or more item IDs to filter by.
     * @return WidgetQuery
     */
    public WidgetQuery withItemId(int... itemId) {
        return removeIf(w -> !ArrayUtils.contains(itemId, w.getItemId()));
    }

    /**
     * Filters widgets by their parent IDs.
     * @param itemId One or more parent IDs to filter by.
     * @return WidgetQuery
     */
    public WidgetQuery withParentId(int... itemId) {
        return removeIf(w -> !ArrayUtils.contains(itemId, w.getParentId()));
    }

    /**
     * Filters widgets by their text, ignoring case.
     * @param text The text to filter by.
     * @return WidgetQuery
     */
    public WidgetQuery withText(String text)
    {
        return removeIf(w -> w.getText() == null || !w.getText().equalsIgnoreCase(text));
    }

    /**
     * Filters widgets that contain any of the specified texts, ignoring case.
     * @param texts One or more texts to check for containment.
     * @return WidgetQuery
     */
    public WidgetQuery withTextContains(String... texts)
    {
        return removeIf(w -> w.getText() == null || Arrays.stream(texts).noneMatch(t -> w.getText().toLowerCase().contains(t.toLowerCase())));
    }

    /**
     * Filters widgets by their actions, ignoring case.
     * @param actions One or more actions to filter by.
     * @return WidgetQuery
     */
    public WidgetQuery withActions(String... actions)
    {
        return removeIf(w -> w.getActions() == null || Arrays.stream(actions).noneMatch(a -> Arrays.stream(w.getActions()).anyMatch(wa -> wa != null && wa.equalsIgnoreCase(a))));
    }

    /**
     * Filters widgets that are visible (not hidden).
     * @return WidgetQuery
     */
    public WidgetQuery isVisible()
    {
        return removeIf(Widget::isHidden);
    }

    /**
     * Filters widgets that are hidden.
     * @return WidgetQuery
     */
    public WidgetQuery isHidden()
    {
        return keepIf(Widget::isHidden);
    }

    /**
     * Filters widgets that are self-visible (not self-hidden).
     * @return WidgetQuery
     */
    public WidgetQuery isSelfVisible()
    {
        return removeIf(Widget::isSelfHidden);
    }

    /**
     * Filters widgets that are self-hidden.
     * @return WidgetQuery
     */
    public WidgetQuery isSelfHidden()
    {
        return keepIf(Widget::isSelfHidden);
    }

    /**
     * Filters widgets by their types.
     * @param types One or more widget types to filter by.
     * @return WidgetQuery
     */
    public WidgetQuery withType(int... types)
    {
        return removeIf(w -> !ArrayUtils.contains(types, w.getType()));
    }

    /**
     * Filters widgets that have children.
     * @return WidgetQuery
     */
    public WidgetQuery withChildren()
    {
        return removeIf(w -> w.getChildren() == null || w.getChildren().length == 0);
    }

    /**
     * Filters widgets that do not have children.
     * @return WidgetQuery
     */
    public WidgetQuery withNoChildren()
    {
        return keepIf(w -> w.getChildren() == null || w.getChildren().length == 0);
    }

    /**
     * Filters widgets by their model IDs.
     * @param modelIds One or more model IDs to filter by.
     * @return WidgetQuery
     */
    public WidgetQuery withModelId(int... modelIds)
    {
        return removeIf(w -> !ArrayUtils.contains(modelIds, w.getModelId()));
    }

    /**
     * Filters widgets by their item quantities, keeping those with quantities greater than the specified value.
     * @param quantity The minimum item quantity (exclusive).
     * @return WidgetQuery
     */
    public WidgetQuery withQuantityGreaterThan(int quantity)
    {
        return removeIf(w -> w.getItemQuantity() <= quantity);
    }

    /**
     * Filters widgets by their item quantities, keeping those with quantities less than the specified value.
     * @param quantity The maximum item quantity (exclusive).
     * @return WidgetQuery
     */
    public WidgetQuery withQuantityLessThan(int quantity)
    {
        return removeIf(w -> w.getItemQuantity() >= quantity);
    }

    /**
     * Filters widgets by their item quantities, keeping those with quantities equal to the specified value.
     * @param quantity The exact item quantity to filter by.
     * @return WidgetQuery
     */
    public WidgetQuery withQuantity(int quantity)
    {
        return removeIf(w -> w.getItemQuantity() != quantity);
    }

    /**
     * Filters widgets by exact reference match.
     * @param widgets One or more widget references to filter by.
     * @return WidgetQuery
     */
    public WidgetQuery withWidget(Widget... widgets)
    {
        return removeIf(w -> !ArrayUtils.contains(widgets, w));
    }
}
