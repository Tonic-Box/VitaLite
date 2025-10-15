package com.tonic.mixins;

import com.tonic.Static;
import com.tonic.api.TBuffer;
import com.tonic.injector.annotations.*;
import com.tonic.injector.util.BytecodeBuilder;
import com.tonic.packets.PacketBuffer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import java.util.*;

@Mixin("PlatformInfo")
public class TPlatformInfoMixin
{
    @Inject
    private static Map<Integer,String> fieldNames;

    static
    {
        int index = 0;
        fieldNames = new HashMap<>();
        fieldNames.put(index++, "osType");
        fieldNames.put(index++, "osVersion");
        fieldNames.put(index++, "vendor");
        fieldNames.put(index++, "javaMajor");
        fieldNames.put(index++, "javaMinor");
        fieldNames.put(index++, "javaPatch");
        fieldNames.put(index++, "maxMemory");
        fieldNames.put(index++, "cpuCores");
        fieldNames.put(index++, "systemMemory");
        fieldNames.put(index++, "clockSpeed");

        fieldNames.put(index++, "Unknown_3");
        fieldNames.put(index++, "Unknown_4");
        fieldNames.put(index++, "process");
        fieldNames.put(index++, "parentProcess");

        fieldNames.put(index++, "Unknown_7");
        fieldNames.put(index++, "Unknown_8");
        fieldNames.put(index++, "Unknown_9");
        fieldNames.put(index++, "Unknown_10");

        fieldNames.put(index++, "Unknown_11");
        fieldNames.put(index++, "Unknown_12");

        fieldNames.put(index++, "Unknown_13");
        fieldNames.put(index++, "clientName");
        fieldNames.put(index++, "agent");

    }

    @Insert(
            method = "write",
            at = @At(value = AtTarget.RETURN),
            ordinal = -1,
            raw = true
    )
    public static void process(MethodNode method, AbstractInsnNode insnTarget)
    {
        try {
            // Get local variable slot for our map (use next available slot)
            int mapVarIndex = method.maxLocals;
            method.maxLocals++; // Reserve the slot

            // Insert HashMap creation at the beginning of the method
            BytecodeBuilder mapInit = BytecodeBuilder.create();
            mapInit
                    .newInstance("java/util/HashMap")
                    .dup()
                    .invokeSpecial("java/util/HashMap", "<init>", "()V")
                    .storeLocal(mapVarIndex, Opcodes.ASTORE); // Store the map in local variable
            method.instructions.insert(mapInit.build());

            // Perform control flow analysis to get execution order
            List<AbstractInsnNode> executionOrder = analyzeControlFlow(method);

            Map<AbstractInsnNode, FieldInfo> instructions = new HashMap<>();
            int fieldIndex = 0;

            // Iterate through instructions in execution order
            for (AbstractInsnNode insn : executionOrder) {
                if (insn.getOpcode() != Opcodes.GETFIELD)
                    continue;

                FieldInsnNode fin = (FieldInsnNode) insn;
                if (!fin.desc.equals("I") && !fin.desc.equals("Ljava/lang/String;"))
                    continue;

                String name = fin.name;
                AbstractInsnNode target;

                if (insn.getNext() != null && insn.getNext().getOpcode() == Opcodes.IMUL) {
                    target = insn.getNext();
                } else {
                    if(insn.getNext().getNext().getOpcode() == Opcodes.IMUL)
                        target = insn.getNext().getNext();
                    else
                        target = insn;
                }

                String fieldName;
                Type type = fin.desc.equals("I") ? Type.INT : Type.STRING;

                if (fieldNames.containsKey(fieldIndex)) {
                    fieldName = fieldNames.get(fieldIndex);
                    if(fieldName.startsWith("Unknown")) {
                        fieldName += "[" + fin.owner + "." + fin.name + "]";
                    }
                } else {
                    fieldName = "Unknown_" + name + "_" + fieldIndex;
                }

                if(fieldName.startsWith("Unknown"))
                {
                    fieldIndex++;
                    continue;
                }

                instructions.put(target, new FieldInfo(fieldName, type));
                fieldIndex++;
            }

            // Now insert map population instructions
            for (var entry : instructions.entrySet()) {
                AbstractInsnNode injectionPoint = entry.getKey();
                FieldInfo fieldInfo = entry.getValue();
                Type valueType = fieldInfo.type;
                String name = fieldInfo.name;

                BytecodeBuilder builder = BytecodeBuilder.create();

                if (valueType == Type.INT) {
                    // Stack: [..., int]
                    builder
                            .dup()  // [..., int, int]
                            .invokeStatic("java/lang/String", "valueOf", "(I)Ljava/lang/String;")  // [..., int, String]
                            .loadLocal(mapVarIndex, Opcodes.ALOAD)  // [..., int, String, map]
                            .swap()  // [..., int, map, String]
                            .pushString(name)  // [..., int, map, String, key]
                            .swap()  // [..., int, map, key, String]
                            .invokeVirtual("java/util/HashMap", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")  // [..., int, Object]
                            .pop();  // [..., int]
                } else if (valueType == Type.STRING) {
                    // Stack: [..., String]
                    builder
                            .dup()  // [..., String, String]
                            .loadLocal(mapVarIndex, Opcodes.ALOAD)  // [..., String, String, map]
                            .swap()  // [..., String, map, String]
                            .pushString(name)  // [..., String, map, String, key]
                            .swap()  // [..., String, map, key, String]
                            .invokeVirtual("java/util/HashMap", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")  // [..., String, Object]
                            .pop();  // [..., String]
                }

                InsnList loggingInsns = builder.build();
                method.instructions.insert(injectionPoint, loggingInsns);
            }

            InsnList code = BytecodeBuilder.create()
                    .loadLocal(mapVarIndex, Opcodes.ALOAD) // Load the map
                    .invokeStatic(
                            "com/tonic/model/PlatformInfoData",
                            "post",
                            "(Ljava/util/Map;)V"
                    ) // Call your method
                    .build();

            method.instructions.insertBefore(insnTarget, code);

            method.maxStack += 4;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Analyze control flow and return instructions in approximate execution order
     */
    private static List<AbstractInsnNode> analyzeControlFlow(MethodNode method) throws AnalyzerException {
        // Use a simple DFS traversal of the control flow graph
        List<AbstractInsnNode> executionOrder = new ArrayList<>();
        Set<AbstractInsnNode> visited = new HashSet<>();

        // Build a map of labels to their positions
        Map<LabelNode, AbstractInsnNode> labelMap = new HashMap<>();
        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof LabelNode) {
                labelMap.put((LabelNode) insn, insn);
            }
        }

        // Start DFS from the first instruction
        AbstractInsnNode start = method.instructions.getFirst();
        dfsTraversal(start, executionOrder, visited, labelMap, method);

        return executionOrder;
    }

    private static void dfsTraversal(AbstractInsnNode current, List<AbstractInsnNode> order,
                                     Set<AbstractInsnNode> visited, Map<LabelNode, AbstractInsnNode> labelMap,
                                     MethodNode method) {
        if (current == null || visited.contains(current)) {
            return;
        }

        visited.add(current);
        order.add(current);

        int opcode = current.getOpcode();

        // Handle different control flow instructions
        if (current instanceof JumpInsnNode) {
            JumpInsnNode jump = (JumpInsnNode) current;

            // For conditional jumps, explore both paths (false branch first, then true)
            if (opcode != Opcodes.GOTO) {
                // Continue to next instruction (false branch)
                dfsTraversal(current.getNext(), order, visited, labelMap, method);
            }

            // Jump target (true branch or unconditional jump)
            AbstractInsnNode target = labelMap.get(jump.label);
            dfsTraversal(target, order, visited, labelMap, method);

        } else if (current instanceof TableSwitchInsnNode) {
            TableSwitchInsnNode switchInsn = (TableSwitchInsnNode) current;

            // Visit default first
            dfsTraversal(labelMap.get(switchInsn.dflt), order, visited, labelMap, method);

            // Then visit all cases
            for (LabelNode label : switchInsn.labels) {
                dfsTraversal(labelMap.get(label), order, visited, labelMap, method);
            }

        } else if (current instanceof LookupSwitchInsnNode) {
            LookupSwitchInsnNode switchInsn = (LookupSwitchInsnNode) current;

            // Visit default first
            dfsTraversal(labelMap.get(switchInsn.dflt), order, visited, labelMap, method);

            // Then visit all cases
            for (LabelNode label : switchInsn.labels) {
                dfsTraversal(labelMap.get(label), order, visited, labelMap, method);
            }

        } else if (opcode == Opcodes.ATHROW || opcode == Opcodes.RETURN ||
                opcode == Opcodes.ARETURN || opcode == Opcodes.IRETURN ||
                opcode == Opcodes.LRETURN || opcode == Opcodes.FRETURN ||
                opcode == Opcodes.DRETURN) {
            // Terminal instruction - don't continue
            return;

        } else {
            // Regular instruction - continue to next
            dfsTraversal(current.getNext(), order, visited, labelMap, method);
        }
    }

    private static class FieldInfo
    {
        String name;
        Type type;

        public FieldInfo(String name, Type type)
        {
            this.name = name;
            this.type = type;
        }
    }

    private enum Type
    {
        STRING,
        INT
    }
}