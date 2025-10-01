package com.tonic.vitalite;

import com.tonic.Logger;
import com.tonic.VitaLite;
import com.tonic.VitaLiteOptions;
import com.tonic.bootstrap.RLUpdater;
import com.tonic.classloader.RLClassLoader;
import com.tonic.injector.util.MappingProvider;
import com.tonic.injector.util.SignerMapper;
import com.tonic.runelite.Install;
import com.tonic.runelite.jvm.JvmParams;
import com.tonic.injector.Injector;
import com.tonic.injector.RLInjector;
import com.tonic.model.Libs;
import com.tonic.services.CatFacts;
import com.tonic.services.proxy.ProxyManager;

import javax.swing.*;
import java.io.File;
import java.net.URL;
import java.nio.file.Path;

import static com.tonic.vitalite.Versioning.isRunningFromShadedJar;

public class Main {
    public static final Path RUNELITE_REPOSITORY_DIR = Path.of(System.getProperty("user.home"), ".runelite", "repository2");
    public static final VitaLiteOptions optionsParser = new VitaLiteOptions();
    private static URL[] URLS = null;
    public static Libs LIBS;
    public static RLClassLoader CLASSLOADER;
    public static RLClassLoader CTX_CLASSLOADER;

    public static void main(String[] args) throws Exception
    {
        args = optionsParser.parse(args);
        if(!optionsParser.isSafeLaunch())
        {
            System.err.println("Safe launch not satisfied, VitaLite will not start.");
            System.exit(0);
        }
        if(optionsParser.getProxy() != null)
        {
            System.out.println("Using Proxy: " + optionsParser.getProxy());
            ProxyManager.process(optionsParser.getProxy());
        }
        JvmParams.set();
        RLUpdater.run();
        loadArtifacts();
        SignerMapper.map();
        loadClassLoader();
        Injector.patch();
        RLInjector.patch();
        MappingProvider.getMappings().clear();
        CLASSLOADER.launch(args);
        Install.install();
        Logger.norm("VitaLite started. - Did you know... " + CatFacts.get(-1));
    }

    private static void loadArtifacts()
    {
        try
        {
            File[] jarfiles = RUNELITE_REPOSITORY_DIR.toFile().listFiles(f ->
                    f.getName().endsWith(".jar") &&
                            !f.getName().contains("guice") &&
                            !f.getName().contains("javax") &&
                            !f.getName().contains("guava") &&
                            !f.getName().contains("logback-core") &&
                            !f.getName().contains("logback-classic") &&
                            !f.getName().contains("slf4j-api")
            );
            if(jarfiles == null)
                throw new Exception();
            URLS = new URL[jarfiles.length];
            for (int i = 0; i < jarfiles.length; i++)
            {
                URLS[i] = jarfiles[i].toURI().toURL();
            }

            LIBS = new Libs(URLS);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    private static void loadClassLoader() {
        CLASSLOADER = new RLClassLoader(URLS);
        CTX_CLASSLOADER = new RLClassLoader(URLS);
        if(!isRunningFromShadedJar())
            UIManager.put("ClassLoader", CLASSLOADER);
        Thread.currentThread().setContextClassLoader(CLASSLOADER);
    }
}