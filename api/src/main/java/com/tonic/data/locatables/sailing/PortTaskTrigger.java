/*
 * Copyright (c) 2025, nucleon <https://github.com/nucleon>
 * Copyright (c) 2025, Cooper Morris <https://github.com/coopermor>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tonic.data.locatables.sailing;

import lombok.Getter;
import net.runelite.api.gameval.VarbitID;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum PortTaskTrigger
{
    TASK_SLOT_0_ID(VarbitID.PORT_TASK_SLOT_0_ID, TaskType.ID, 0),
    TASK_SLOT_0_TAKEN(VarbitID.PORT_TASK_SLOT_0_CARGO_TAKEN,  TaskType.TAKEN, 0),
    TASK_SLOT_0_DELIVERED(VarbitID.PORT_TASK_SLOT_0_CARGO_DELIVERED, TaskType.DELIVERED, 0),
    TASK_SLOT_1_ID(VarbitID.PORT_TASK_SLOT_1_ID, TaskType.ID, 1),
    TASK_SLOT_1_TAKEN(VarbitID.PORT_TASK_SLOT_1_CARGO_TAKEN,  TaskType.TAKEN, 1),
    TASK_SLOT_1_DELIVERED(VarbitID.PORT_TASK_SLOT_1_CARGO_DELIVERED, TaskType.DELIVERED, 1),
    TASK_SLOT_2_ID(VarbitID.PORT_TASK_SLOT_2_ID,  TaskType.ID, 2),
    TASK_SLOT_2_TAKEN(VarbitID.PORT_TASK_SLOT_2_CARGO_TAKEN, TaskType.TAKEN, 2),
    TASK_SLOT_2_DELIVERED(VarbitID.PORT_TASK_SLOT_2_CARGO_DELIVERED, TaskType.DELIVERED, 2),
    TASK_SLOT_3_ID(VarbitID.PORT_TASK_SLOT_3_ID, TaskType.ID, 3),
    TASK_SLOT_3_TAKEN(VarbitID.PORT_TASK_SLOT_3_CARGO_TAKEN,  TaskType.TAKEN, 3),
    TASK_SLOT_3_DELIVERED(VarbitID.PORT_TASK_SLOT_3_CARGO_DELIVERED,  TaskType.DELIVERED, 3),
    TASK_SLOT_4_ID(VarbitID.PORT_TASK_SLOT_4_ID, TaskType.ID, 4),
    TASK_SLOT_4_TAKEN(VarbitID.PORT_TASK_SLOT_4_CARGO_TAKEN, TaskType.TAKEN, 4),
    TASK_SLOT_4_DELIVERED(VarbitID.PORT_TASK_SLOT_4_CARGO_DELIVERED, TaskType.DELIVERED, 4),
    LAST_CARGO_TAKEN(VarbitID.PORT_TASK_LAST_CARGO_TAKEN, TaskType.OTHER, -1);

    private final int id;
    private final TaskType type;
    private final int slot;

    public enum TaskType
    {
        ID, TAKEN, DELIVERED, OTHER
    }

    PortTaskTrigger(int id, TaskType type, int slot)
    {
        this.id = id;
        this.type = type;
        this.slot = slot;
    }

    private static final Map<Integer, PortTaskTrigger> lookup = new HashMap<>();
    static
    {
        for (PortTaskTrigger v : values())
        {
            lookup.put(v.id, v);
        }
    }

    public static PortTaskTrigger fromId(int id)
    {
        return lookup.get(id);
    }

    public static boolean contains(int id)
    {
        return lookup.containsKey(id);
    }

    @Override
    public String toString()
    {
        return String.format("%s (Type: %s, Slot: %d)", name(), type, slot);
    }

}