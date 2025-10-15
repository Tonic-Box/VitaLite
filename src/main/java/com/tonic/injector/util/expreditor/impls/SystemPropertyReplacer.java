package com.tonic.injector.util.expreditor.impls;

import com.tonic.bootstrap.JdkVersionUtil;
import com.tonic.injector.util.expreditor.ExprEditor;
import com.tonic.injector.util.expreditor.MethodCall;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;

public class SystemPropertyReplacer extends ExprEditor {
    
    @Override
    public void edit(MethodCall call) {
        if (call.getMethodOwner().equals("java/lang/System") && 
            call.getMethodName().equals("getProperty") && 
            call.getMethodDesc().equals("(Ljava/lang/String;)Ljava/lang/String;")) {
            String propertyKey = getPropertyKey(call);
            
            if ("java.vendor".equals(propertyKey)) {
                replaceWithHardcodedValue(call, "Eclipse Adoptium");
            } else if ("java.version".equals(propertyKey)) {
                replaceWithHardcodedValue(call, JdkVersionUtil.calculateJreVersion());
            }
        }
    }
    
    private String getPropertyKey(MethodCall call) {
        AbstractInsnNode current = call.getInstruction().getPrevious();
        while (current != null) {
            if (current instanceof LdcInsnNode) {
                LdcInsnNode ldcInsn = (LdcInsnNode) current;
                if (ldcInsn.cst instanceof String) {
                    return (String) ldcInsn.cst;
                }
            } else if (current.getType() == AbstractInsnNode.LABEL ||
                       current.getType() == AbstractInsnNode.LINE ||
                       current.getType() == AbstractInsnNode.FRAME) {
                current = current.getPrevious();
                continue;
            } else {
                break;
            }
            current = current.getPrevious();
        }
        
        return null;
    }
    
    private void replaceWithHardcodedValue(MethodCall call, String hardcodedValue) {
        AbstractInsnNode ldcInsn = findAndRemovePropertyKeyLdc(call);
        
        if (ldcInsn != null) {
            InsnList replacement = new InsnList();
            replacement.add(new LdcInsnNode(hardcodedValue));
            call.replace(replacement);
        }
    }
    
    private AbstractInsnNode findAndRemovePropertyKeyLdc(MethodCall call) {
        AbstractInsnNode current = call.getInstruction().getPrevious();
        
        while (current != null) {
            if (current instanceof LdcInsnNode) {
                LdcInsnNode ldcInsn = (LdcInsnNode) current;
                if (ldcInsn.cst instanceof String) {
                    call.getMethod().instructions.remove(current);
                    return current;
                }
            } else if (current.getType() == AbstractInsnNode.LABEL ||
                       current.getType() == AbstractInsnNode.LINE ||
                       current.getType() == AbstractInsnNode.FRAME) {
                current = current.getPrevious();
                continue;
            } else {
                break;
            }
            current = current.getPrevious();
        }
        
        return null;
    }
}