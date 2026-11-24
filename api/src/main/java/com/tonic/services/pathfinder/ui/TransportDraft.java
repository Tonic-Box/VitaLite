package com.tonic.services.pathfinder.ui;

import com.tonic.services.pathfinder.requirements.Requirements;
import com.tonic.services.pathfinder.transports.Transport;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The editable fields of one transport as the detail form holds them, with the rules they must meet to become one; blank dialogue lines are dropped.
 */
final class TransportDraft
{
    String name = "";
    boolean object = true;
    int id;
    int option = 1;
    int sourceX;
    int sourceY;
    int sourcePlane;
    int destinationX;
    int destinationY;
    int destinationPlane;
    int duration = 1;
    List<String> dialogueOptions = new ArrayList<>();
    Requirements requirements = new Requirements();

    TransportDraft()
    {
    }

    TransportDraft(Transport transport)
    {
        name = transport.getName() == null ? "" : transport.getName();
        object = transport.isObject();
        id = transport.getId();
        option = transport.getOption();
        WorldPoint source = transport.getLocation().toWorldPoint();
        WorldPoint destination = transport.getDestination().toWorldPoint();
        sourceX = source.getX();
        sourceY = source.getY();
        sourcePlane = source.getPlane();
        destinationX = destination.getX();
        destinationY = destination.getY();
        destinationPlane = destination.getPlane();
        duration = transport.getDuration();
        if (transport.getDialogueOptions() != null)
        {
            dialogueOptions.addAll(Arrays.asList(transport.getDialogueOptions()));
        }
        requirements = copy(transport.getRequirements());
    }

    static TransportDraft at(WorldPoint tile)
    {
        TransportDraft draft = new TransportDraft();
        draft.name = "New transport";
        if (tile != null)
        {
            draft.sourceX = tile.getX();
            draft.sourceY = tile.getY();
            draft.sourcePlane = tile.getPlane();
            draft.destinationX = tile.getX();
            draft.destinationY = tile.getY();
            draft.destinationPlane = tile.getPlane();
        }
        return draft;
    }

    List<String> validate()
    {
        List<String> errors = new ArrayList<>();
        if (name == null || name.trim().isEmpty())
        {
            errors.add("Name must not be empty.");
        }
        if (id < 0)
        {
            errors.add("Target id must be 0 or more.");
        }
        if (option < 1 || option > 5)
        {
            errors.add("Option must be 1 to 5.");
        }
        if (sourcePlane < 0 || sourcePlane > 3)
        {
            errors.add("Source plane must be 0 to 3.");
        }
        if (destinationPlane < 0 || destinationPlane > 3)
        {
            errors.add("Destination plane must be 0 to 3.");
        }
        if (sourceX < 0 || sourceY < 0 || destinationX < 0 || destinationY < 0)
        {
            errors.add("Coordinates must be 0 or more.");
        }
        if (duration < 1)
        {
            errors.add("Duration must be at least 1 tick.");
        }
        return errors;
    }

    Transport build()
    {
        List<String> lines = new ArrayList<>();
        for (String line : dialogueOptions)
        {
            if (line != null && !line.trim().isEmpty())
            {
                lines.add(line.trim());
            }
        }

        WorldPoint source = new WorldPoint(sourceX, sourceY, sourcePlane);
        WorldPoint destination = new WorldPoint(destinationX, destinationY, destinationPlane);
        return new Transport(name.trim(), id, object, source, destination, option, lines.toArray(new String[0]), duration, requirements.getAll().isEmpty() ? null : copy(requirements));
    }

    static Requirements copy(Requirements source)
    {
        Requirements copy = new Requirements();
        if (source != null)
        {
            copy.addRequirements(source.getAll());
        }
        return copy;
    }
}
