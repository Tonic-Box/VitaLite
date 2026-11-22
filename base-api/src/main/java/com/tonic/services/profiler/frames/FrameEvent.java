package com.tonic.services.profiler.frames;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

/**
 * A Flight Recorder event spanning one client frame, from BeforeRender to BeforeRender, written only while a recording enables it.
 */
@Name(FrameProfiler.EVENT_NAME)
@Label("Client Frame")
@Category("VitaLite")
@Description("One client frame, from BeforeRender to BeforeRender")
@StackTrace(false)
final class FrameEvent extends Event
{
    @Label("Frame Number")
    long number;
}
