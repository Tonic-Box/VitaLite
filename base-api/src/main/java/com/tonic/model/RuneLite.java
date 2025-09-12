package com.tonic.model;

import com.google.inject.Injector;

import com.tonic.util.ReflectUtil;
import com.tonic.util.RuneliteConfigUtil;
import lombok.Getter;

@Getter
public class RuneLite
{
    private final Class<?> runeLiteMain;
    private final Guice injector;
    private final PluginManager pluginManager;
    private final RLClientThread clientThread;
    private final RLEventBus eventBus;
    private final RLClientUI clientUI;
    private final GameApplet gameApplet = new GameApplet();
    private final String USER_AGENT;
    private final String version = RuneliteConfigUtil.getTagValueFromURL("release");

    public RuneLite(Class<?> runeLiteMain) throws Exception {
        this.runeLiteMain = runeLiteMain;
        this.injector = new Guice((Injector) ReflectUtil.getStaticField(runeLiteMain, "injector"));
        this.pluginManager = new PluginManager(injector);
        this.clientThread = new RLClientThread(runeLiteMain);
        this.eventBus = new RLEventBus(injector);
        this.clientUI = new RLClientUI(runeLiteMain);
        this.USER_AGENT = (String) ReflectUtil.getStaticField(runeLiteMain, "USER_AGENT");
    }
}
