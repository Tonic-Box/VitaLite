package com.tonic.injector.util;

import com.tonic.model.ConditionType;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.function.Consumer;

import static org.objectweb.asm.Opcodes.*;

/**
 * A simple bytecode builder for generating method bodies.
 */
public class BytecodeBuilder {
    public static BytecodeBuilder create() {
        return new BytecodeBuilder();
    }
    private final InsnList insnList;
    private final Map<Integer, LabelNode> labels;

    /**
     * We keep a list of try/catch blocks that we've created in this builder.
     * The user can retrieve them later and attach to a MethodNode.
     */
    private final List<TryCatchBlockNode> tryCatchBlocks;

    public BytecodeBuilder() {
        this.insnList = new InsnList();
        this.labels = new HashMap<>();
        this.tryCatchBlocks = new ArrayList<>();
    }

    /**
     * Builds the InsnList.
     * @return the InsnList
     */
    public InsnList build() {
        return insnList;
    }

    /**
     * Pushes an integer constant onto the stack.
     * @param value the integer value
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder pushInt(int value) {
        if (value >= -1 && value <= 5) {
            insnList.add(new InsnNode(ICONST_0 + value));
        } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            insnList.add(new IntInsnNode(BIPUSH, value));
        } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            insnList.add(new IntInsnNode(SIPUSH, value));
        } else {
            insnList.add(new LdcInsnNode(value));
        }
        return this;
    }

    /**
     * Pushes a double onto the stack.
     * @param value the double value
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder pushDouble(double value) {
        if (value == 0.0) {
            insnList.add(new InsnNode(DCONST_0));
        } else if (value == 1.0) {
            insnList.add(new InsnNode(DCONST_1));
        } else {
            insnList.add(new LdcInsnNode(value));
        }
        return this;
    }

    /**
     * Pushes a float onto the stack.
     * @param value the float value
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder pushFloat(float value) {
        insnList.add(new LdcInsnNode(value));
        return this;
    }

    /**
     * Pushes a double onto the stack.
     * @param value the double value
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder pushLong(long value) {
        insnList.add(new LdcInsnNode(value));
        return this;
    }

    /**
     * Pushes a string onto the stack.
     * @param value the string value
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder pushString(String value) {
        insnList.add(new LdcInsnNode(value));
        return this;
    }

    /**
     * Pushes a null onto the stack.
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder pushNull() {
        insnList.add(new InsnNode(ACONST_NULL));
        return this;
    }

    /**
     * Pushes 'this' onto the stack.
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder pushThis() {
        insnList.add(new VarInsnNode(ALOAD, 0));
        return this;
    }

    public BytecodeBuilder pushLocal(int index) {
        insnList.add(new VarInsnNode(ALOAD, index));
        return this;
    }

    /**
     * Pushes a local variable onto the stack.
     * @param index the local variable index
     * @param type the local variable type
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder storeLocal(int index, int type) {
        insnList.add(new VarInsnNode(type, index));
        return this;
    }

    /**
     * Loads a local variable from the stack.
     * @param index the local variable index
     * @param type the local variable type
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder loadLocal(int index, int type) {
        insnList.add(new VarInsnNode(type, index));
        return this;
    }

    /**
     * Invokes a dynamic method.
     * @param name the method name
     * @param descriptor the method descriptor
     * @param bootstrapMethodOwner the bootstrap method owner
     * @param bootstrapMethodName the bootstrap method name
     * @param bootstrapMethodDescriptor the bootstrap method descriptor
     * @param bootstrapArgs the bootstrap method arguments
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder invokeDynamic(
            String name,
            String descriptor,
            String bootstrapMethodOwner,
            String bootstrapMethodName,
            String bootstrapMethodDescriptor,
            Object... bootstrapArgs
    ) {
        Handle bootstrapHandle = new Handle(
                H_INVOKESTATIC,
                bootstrapMethodOwner,
                bootstrapMethodName,
                bootstrapMethodDescriptor,
                false
        );

        insnList.add(new InvokeDynamicInsnNode(name, descriptor, bootstrapHandle, bootstrapArgs));
        return this;
    }

    /**
     * Invokes static a method.
     * @param owner the method owner
     * @param name the method name
     * @param desc the method descriptor
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder invokeStatic(String owner, String name, String desc) {
        insnList.add(new MethodInsnNode(INVOKESTATIC, owner, name, desc, false));
        return this;
    }

    /**
     * Invokes a virtual method.
     * @param owner the method owner
     * @param name the method name
     * @param desc the method descriptor
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder invokeVirtual(String owner, String name, String desc) {
        insnList.add(new MethodInsnNode(INVOKEVIRTUAL, owner, name, desc, false));
        return this;
    }

    /**
     * Invokes a special method.
     * @param owner the method owner
     * @param name the method name
     * @param desc the method descriptor
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder invokeSpecial(String owner, String name, String desc) {
        insnList.add(new MethodInsnNode(INVOKESPECIAL, owner, name, desc, false));
        return this;
    }

    /**
     * Invokes an interface method.
     * @param owner the method owner
     * @param name the method name
     * @param desc the method descriptor
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder invokeInterface(String owner, String name, String desc) {
        insnList.add(new MethodInsnNode(INVOKEINTERFACE, owner, name, desc, true)); // 'true' for interface
        return this;
    }

    /**
     * Returns void.
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder returnVoid() {
        insnList.add(new InsnNode(RETURN));
        return this;
    }

    /**
     * Returns a value.
     * @param type the return type
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder returnValue(int type) {
        insnList.add(new InsnNode(type));
        return this;
    }

    public BytecodeBuilder randomIMathInst()
    {
        int random = new Random().nextInt(5);
        switch (random)
        {
            case 0:
                add();
                break;
            case 1:
                sub();
                break;
            case 2:
                mul();
                break;
            case 3:
                div();
                break;
            case 4:
                mod();
                break;
        }
        return this;
    }

    /**
     * Adds two integers.
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder add() {
        insnList.add(new InsnNode(IADD));
        return this;
    }

    /**
     * Subtracts two integers.
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder sub() {
        insnList.add(new InsnNode(ISUB));
        return this;
    }

    /**
     * Multiplies two integers.
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder mul() {
        insnList.add(new InsnNode(IMUL));
        return this;
    }

    /**
     * Divides two integers.
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder div() {
        insnList.add(new InsnNode(IDIV));
        return this;
    }

    /**
     * Modulus of two integers.
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder mod() {
        insnList.add(new InsnNode(IREM));
        return this;
    }

    /**
     * Bitwise AND of two integers.
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder and() {
        insnList.add(new InsnNode(IAND));
        return this;
    }

    /**
     * Bitwise OR of two integers.
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder or() {
        insnList.add(new InsnNode(IOR));
        return this;
    }

    /**
     * Bitwise XOR of two integers.
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder xor() {
        insnList.add(new InsnNode(IXOR));
        return this;
    }

    /**
     * Shift left of two integers.
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder shl() {
        insnList.add(new InsnNode(ISHL));
        return this;
    }

    /**
     * Shift right of two integers.
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder shr() {
        insnList.add(new InsnNode(ISHR));
        return this;
    }

    /**
     * Unsigned shift right of two integers.
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder ushr() {
        insnList.add(new InsnNode(IUSHR));
        return this;
    }

    /**
     * Negates an integer.
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder neg() {
        insnList.add(new InsnNode(INEG));
        return this;
    }

    /**
     * Logical NOT of an integer.
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder not() {
        insnList.add(new InsnNode(ICONST_M1));
        insnList.add(new InsnNode(IXOR));
        return this;
    }

    /**
     * Get the value of a field and push it onto the stack.
     * @param owner the field owner
     * @param name the field name
     * @param desc the field descriptor
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder getField(String owner, String name, String desc) {
        insnList.add(new FieldInsnNode(GETFIELD, owner, name, desc));
        return this;
    }

    /**
     * Set the value of a field.
     * @param owner the field owner
     * @param name the field name
     * @param desc the field descriptor
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder putField(String owner, String name, String desc) {
        insnList.add(new FieldInsnNode(PUTFIELD, owner, name, desc));
        return this;
    }

    /**
     * Get the value of a static field and push it onto the stack.
     * @param owner the field owner
     * @param name the field name
     * @param desc the field descriptor
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder getStaticField(String owner, String name, String desc) {
        insnList.add(new FieldInsnNode(GETSTATIC, owner, name, desc));
        return this;
    }

    /**
     * Set the value of a static field.
     * @param owner the field owner
     * @param name the field name
     * @param desc the field descriptor
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder putStaticField(String owner, String name, String desc) {
        insnList.add(new FieldInsnNode(PUTSTATIC, owner, name, desc));
        return this;
    }

    /**
     * Create a new label.
     * @param name the label name
     * @return the LabelNode
     */
    public LabelNode createLabel(String name) {
        LabelNode label = new LabelNode();
        labels.putIfAbsent(name.hashCode(), label);
        return label;
    }

    /**
     * Place a label.
     * @param label the label
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder placeLabel(LabelNode label) {
        insnList.add(label);
        return this;
    }

    /**
     * Place a label.
     * @param name the label name
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder placeLabel(String name) {
        LabelNode label = labels.get(name.hashCode());
        if(label == null) {
            throw new IllegalArgumentException("Label not found: " + name);
        }
        insnList.add(label);
        return this;
    }

    /**
     * Goto a label.
     * @param label the label
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder gotoLabel(LabelNode label) {
        insnList.add(new JumpInsnNode(GOTO, label));
        return this;
    }

    /**
     * Goto a label.
     * @param name the label name
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder gotoLabel(String name) {
        LabelNode label = labels.get(name.hashCode());
        if(label == null) {
            throw new IllegalArgumentException("Label not found: " + name);
        }
        insnList.add(new JumpInsnNode(GOTO, label));
        return this;
    }

    public BytecodeBuilder dup() {
        insnList.add(new InsnNode(DUP));
        return this;
    }

    public BytecodeBuilder dup2() {
        insnList.add(new InsnNode(DUP2));
        return this;
    }

    public BytecodeBuilder pop() {
        insnList.add(new InsnNode(POP));
        return this;
    }

    public BytecodeBuilder pop2() {
        insnList.add(new InsnNode(POP2));
        return this;
    }

    /**
     * Create a new instance of a class.
     */
    public BytecodeBuilder newInstance(String className) {
        insnList.add(new TypeInsnNode(NEW, className));
        return this;
    }

    public BytecodeBuilder throwRuntimeException(String message) {
        newInstance("java/lang/RuntimeException");
        dup();
        pushString(message);
        invokeSpecial("java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V");
        insnList.add(new InsnNode(ATHROW));
        return this;
    }

    /**
     * appends a new InsnList to the current InsnList
     * @param insnList the InsnList to append
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder appendInsnList(InsnList insnList) {
        if(insnList != null)
            this.insnList.add(insnList);
        return this;
    }

    /**
     * appends a new AbstractInsnNode to the current InsnList
     * @param insnNode the AbstractInsnNode to append
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder appendInsn(AbstractInsnNode insnNode) {
        this.insnList.add(insnNode);
        return this;
    }

    /**
     * Concatenates multiple values into a single string.
     * @param itemPushes the item pushes (should toString all values)
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder concatenateToString(Consumer<BytecodeBuilder> itemPushes) {
        BytecodeBuilder subBuilder = new BytecodeBuilder();
        itemPushes.accept(subBuilder);
        InsnList subInsnList = subBuilder.build();

        int itemCount = subInsnList.size();
        if (itemCount < 1) {
            throw new IllegalArgumentException("Must concatenate at least one item.");
        }

        InsnUtil.insertStringCasts(subInsnList);
        insnList.add(subInsnList);


        if (itemCount == 1) {
            return this;
        }

        newInstance("java/lang/StringBuilder")
                .dup()
                .invokeSpecial("java/lang/StringBuilder", "<init>", "()V");

        for (int i = 0; i < itemCount; i++) {
            insnList.add(new InsnNode(SWAP));
            insnList.add(new MethodInsnNode(
                    INVOKEVIRTUAL,
                    "java/lang/StringBuilder",
                    "append",
                    "(Ljava/lang/String;)Ljava/lang/StringBuilder;", // Use String instead of Object
                    false
            ));
        }

        invokeVirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
        return this;
    }

    /**
     * Creates a switch statement.
     *
     * @apiNote Only supports integer switch statements.
     *
     * @param selector The consumer that pushes the switch selector onto the stack.
     * @param cases    A map where keys are case values and values are code blocks.
     * @param defaultCase The default case (if present).
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder switchBlock(Consumer<BytecodeBuilder> selector, Map<Integer, Consumer<BytecodeBuilder>> cases, Consumer<BytecodeBuilder> defaultCase) {
        LabelNode defaultLabel = new LabelNode();
        LabelNode endLabel = new LabelNode();
        Map<Integer, LabelNode> caseLabels = new HashMap<>();
        for (Integer key : cases.keySet()) {
            caseLabels.put(key, new LabelNode());
        }
        selector.accept(this);
        int[] keys = caseLabels.keySet().stream().mapToInt(i -> i).sorted().toArray();
        LabelNode[] labels = Arrays.stream(keys).mapToObj(caseLabels::get).toArray(LabelNode[]::new);

        if (keys.length > 0 && keys[keys.length - 1] - keys[0] == keys.length - 1) {
            insnList.add(new TableSwitchInsnNode(keys[0], keys[keys.length - 1], defaultLabel, labels));
        } else {
            insnList.add(new LookupSwitchInsnNode(defaultLabel, keys, labels));
        }
        for (Map.Entry<Integer, Consumer<BytecodeBuilder>> entry : cases.entrySet()) {
            LabelNode caseLabel = caseLabels.get(entry.getKey());
            insnList.add(caseLabel);
            entry.getValue().accept(this);
            insnList.add(new JumpInsnNode(GOTO, endLabel));
        }
        insnList.add(defaultLabel);
        if (defaultCase != null) {
            defaultCase.accept(this);
        }
        insnList.add(endLabel);

        return this;
    }

    /**
     * Creates a do-while block.
     *
     * @param body      The body of the loop.
     * @param condition The condition to check at the end of each iteration.
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder doWhileBlock(Consumer<BytecodeBuilder> body, Consumer<BytecodeBuilder> condition, ConditionType conditionType) {
        LabelNode startLabel = new LabelNode();
        LabelNode endLabel = new LabelNode();

        // Start of the loop
        insnList.add(startLabel);

        // Execute the body
        body.accept(this);

        // Evaluate the condition
        condition.accept(this);

        // Jump back to start if the condition is met
        insnList.add(new JumpInsnNode(conditionType.getOpcode(), startLabel));

        // Exit point
        insnList.add(endLabel);

        return this;
    }

    /**
     * Creates an if block.
     * @param conditionType the condition type
     * @param condition the condition
     * @param body the body
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder ifBlock(ConditionType conditionType, Consumer<BytecodeBuilder> condition, Consumer<BytecodeBuilder> body) {
        LabelNode label = new LabelNode();
        condition.accept(this);
        jumpIf(conditionType, label);
        body.accept(this);
        insnList.add(label);
        return this;
    }

    public BytecodeBuilder jumpIf(ConditionType conditionType, LabelNode labelNode)
    {
        insnList.add(new JumpInsnNode(conditionType.getOpcode(), labelNode));
        return this;
    }

    /**
     * Creates an if-else if-else block.
     *
     * @param conditionsAndBodies A list of condition-body pairs for "if" and "else if" blocks.
     * @param elseBody            The body of the else block (optional, can be null).
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder ifElseBlock(
            Map<ConditionType, Consumer<BytecodeBuilder>> conditionsAndBodies,
            Consumer<BytecodeBuilder> elseBody
    ) {
        LabelNode endLabel = new LabelNode();
        List<LabelNode> elseIfLabels = new ArrayList<>();

        // Create labels for else if blocks
        for (int i = 0; i < conditionsAndBodies.size(); i++) {
            elseIfLabels.add(new LabelNode());
        }

        int index = 0;
        for (Map.Entry<ConditionType, Consumer<BytecodeBuilder>> entry : conditionsAndBodies.entrySet()) {
            LabelNode nextLabel = (index < elseIfLabels.size() - 1) ? elseIfLabels.get(index + 1) : endLabel;
            entry.getValue().accept(this);
            insnList.add(new JumpInsnNode(entry.getKey().getOpcode(), nextLabel));
            entry.getValue().accept(this);
            insnList.add(new JumpInsnNode(GOTO, endLabel));
            insnList.add(elseIfLabels.get(index));
            index++;
        }
        if (elseBody != null) {
            elseBody.accept(this);
        }
        insnList.add(endLabel);
        return this;
    }

    /**
     * Creates a while block.
     * @param conditionType the condition type
     * @param condition the condition
     * @param body the body
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder whileBlock(ConditionType conditionType, Consumer<BytecodeBuilder> condition, Consumer<BytecodeBuilder> body) {
        LabelNode startLabel = new LabelNode();
        LabelNode endLabel = new LabelNode();
        insnList.add(startLabel);
        condition.accept(this);
        insnList.add(new JumpInsnNode(conditionType.getOpcode(), endLabel));
        body.accept(this);
        insnList.add(new JumpInsnNode(GOTO, startLabel));
        insnList.add(endLabel);
        return this;
    }

    /**
     * Creates a for block.
     * @param conditionType the condition type
     * @param init the initialization
     * @param condition the condition
     * @param update the update
     * @param body the body
     * @return the BytecodeBuilder
     */
    public BytecodeBuilder forBlock(ConditionType conditionType, Consumer<BytecodeBuilder> init, Consumer<BytecodeBuilder> condition, Consumer<BytecodeBuilder> update, Consumer<BytecodeBuilder> body) {
        LabelNode startLabel = new LabelNode();
        LabelNode endLabel = new LabelNode();
        init.accept(this);
        insnList.add(startLabel);
        condition.accept(this);
        insnList.add(new JumpInsnNode(conditionType.invert().getOpcode(), endLabel));
        body.accept(this);
        update.accept(this);
        insnList.add(new JumpInsnNode(GOTO, startLabel));
        insnList.add(endLabel);
        return this;
    }

    public BytecodeBuilder swap()
    {
        insnList.add(new InsnNode(SWAP));
        return this;
    }

    /**
     * Returns any try/catch blocks that were created in this builder.
     * You must attach them to a MethodNode's 'tryCatchBlocks' list.
     */
    public List<TryCatchBlockNode> getTryCatchBlocks() {
        return tryCatchBlocks;
    }

    /**
     * Inserts a try/catch structure:
     *
     * try {
     *    [tryBlock code...]
     * } catch (ExceptionType e) {
     *    [catchBlock code...]
     * }
     *
     * - exceptionInternalName is the internal JVM name, e.g. "java/lang/Exception".
     * - tryBlock and catchBlock each receive a BytecodeBuilder to append instructions.
     *
     * The code flow is:
     *   startLabel:
     *     (tryBlock)
     *   endLabel:
     *     GOTO afterCatch
     *   handlerLabel:
     *     (catchBlock)
     *   afterCatch:
     *     ...
     */
    public BytecodeBuilder tryCatch(
            String exceptionInternalName,
            Consumer<BytecodeBuilder> tryBlock,
            Consumer<BytecodeBuilder> catchBlock
    ) {
        LabelNode startLabel   = new LabelNode(new Label());
        LabelNode endLabel     = new LabelNode(new Label());
        LabelNode handlerLabel = new LabelNode(new Label());
        LabelNode afterCatch   = new LabelNode(new Label());

        // Mark start of 'try' region
        insnList.add(startLabel);

        // Generate the "try" block code
        tryBlock.accept(this);

        // Mark end of 'try' region
        insnList.add(endLabel);

        // Normal flow jumps beyond the catch
        insnList.add(new JumpInsnNode(GOTO, afterCatch));

        // The "catch" handler label
        insnList.add(handlerLabel);

        // Generate the "catch" block code
        catchBlock.accept(this);

        // Place the "afterCatch" label
        insnList.add(afterCatch);

        // Record the try/catch block in our list
        //   The 'type' is the internal name of the caught exception,
        //   e.g. "java/lang/Exception".
        tryCatchBlocks.add(new TryCatchBlockNode(
                startLabel,  // start
                endLabel,    // end
                handlerLabel,// handler
                exceptionInternalName
        ));

        return this;
    }

    public BytecodeBuilder lmul()
    {
        insnList.add(new InsnNode(LMUL));
        return this;
    }

    public BytecodeBuilder castToType(String internalName) {
        insnList.add(new TypeInsnNode(CHECKCAST, internalName));
        return this;
    }

    public BytecodeBuilder intToLong() {
        insnList.add(new InsnNode(I2L));
        return this;
    }

    public BytecodeBuilder longToInt() {
        insnList.add(new InsnNode(L2I));
        return this;
    }

    public BytecodeBuilder intToDouble() {
        insnList.add(new InsnNode(I2D));
        return this;
    }

    public BytecodeBuilder doubleToInt() {
        insnList.add(new InsnNode(D2I));
        return this;
    }

    public BytecodeBuilder intToFloat() {
        insnList.add(new InsnNode(I2F));
        return this;
    }

    public BytecodeBuilder floatToInt() {
        insnList.add(new InsnNode(F2I));
        return this;
    }

    public BytecodeBuilder longToDouble() {
        insnList.add(new InsnNode(L2D));
        return this;
    }

    public BytecodeBuilder doubleToLong() {
        insnList.add(new InsnNode(D2L));
        return this;
    }

    public BytecodeBuilder intToByte() {
        insnList.add(new InsnNode(I2B));
        return this;
    }

    public BytecodeBuilder intToChar() {
        insnList.add(new InsnNode(I2C));
        return this;
    }

    public BytecodeBuilder iinc(int index, int number)
    {
        insnList.add(new IincInsnNode(index, number));
        return this;
    }

    public BytecodeBuilder ifNull(LabelNode labelNode)
    {
        insnList.add(new JumpInsnNode(IFNULL, labelNode));
        return this;
    }

    public BytecodeBuilder pushClass(String internalName) {
        insnList.add(new LdcInsnNode(
                Type.getObjectType(internalName)
        ));
        return this;
    }

    public BytecodeBuilder newArray(String type) {
        insnList.add(new TypeInsnNode(ANEWARRAY, type));
        return this;
    }

    public BytecodeBuilder arrayLoad(int type) {
        insnList.add(new InsnNode(type));
        return this;
    }
}