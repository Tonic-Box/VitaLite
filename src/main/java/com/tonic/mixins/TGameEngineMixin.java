package com.tonic.mixins;

import com.tonic.Static;
import com.tonic.api.TGameEngine;
import com.tonic.data.LoginMessage;
import com.tonic.data.LoginResponse;
import com.tonic.injector.annotations.*;
import org.slf4j.Logger;

import java.awt.AWTEvent;
import java.awt.EventQueue;

@Mixin("GameEngine")
public class TGameEngineMixin implements TGameEngine
{
    @Shadow("logger")
    public static Logger logger;

    @Shadow("graphicsGuard")
    public static boolean graphicsGuard;

    @FieldHook("graphicsGuard")
    public static boolean onGuardSet(boolean bool)
    {
//        if(Static.isHeadless())
//        {
//            graphicsGuard = false;
//            return false;
//        }
        return true;
    }

    @Disable("processError")
    public static boolean processError(String message, Throwable error)
    {
        Throwable var3 = error;
        if(error instanceof Iterable && "".equals(error.getMessage())) {
            var3 = error.getCause();
        }

        if(message == null) {
            logger.error("Client error", var3);
            com.tonic.Logger.warn("Client error: " + var3.getMessage());
        }
        else {
            logger.error("Client error: {}", message, var3);
            com.tonic.Logger.warn("Client error: " + message);
        }
        return false;
    }

    /**
     * Keeps the CPU renderer's frame loop from sleeping while the event dispatch thread has events waiting, as it already does not with a GPU renderer: the loop slept a millisecond at a time, up to 50 times, so any interface work, the streamer mirror's painting among it, cost game frames. The engine holds several copies of the wait, all redirected.
     *
     * @param queue the event queue
     * @return null, so the loop sees no events waiting
     */
    @Redirect(method = "*", at = @At(value = AtTarget.INVOKE, owner = "java/awt/EventQueue", target = "peekEvent"), all = true)
    public static AWTEvent noEventWait(EventQueue queue)
    {
        return null;
    }

    @MethodHook("getLoginError")
    public static void onGetLoginError(int code)
    {
        Static.post(LoginResponse.fromIndex(code));
    }

    @MethodHook("setLoginResponse")
    public static void onSetLoginResponse(String line1, String line2, String line3)
    {
        Static.post(new LoginMessage(line1, line2, line3));
    }
}
