package com.tonic.model;

import com.tonic.util.ArtifactReader;
import lombok.Getter;
import lombok.Setter;

import java.net.URL;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class Libs
{
    private final Artifact other = new Artifact();
    private final Artifact runelite = new Artifact();
    private final Artifact gamepack = new Artifact();
    private final Artifact gamepackClean = new Artifact();
    private final Map<String, Certificate[]> classCerts = new HashMap<>();
    private final Map<String, URL> urls = new HashMap<>();

    public Libs(URL[] urls) throws Exception {
        ArtifactReader.read(this, urls);
    }

    public synchronized byte[] gamepackByName(String name)
    {
        return gamepack.classes.get(name);
    }

    public synchronized byte[] classByName(String className) {
        byte[] classBytes = runelite.classes.get(className);
        if (classBytes != null)
        {
            return classBytes;
        }
        return other.classes.get(className);
    }

    /**
     * Drops the bytes of a gamepack or RuneLite class once it has been defined, as they are never read again.
     *
     * @param className the class name
     */
    public synchronized void release(String className)
    {
        gamepack.classes.remove(className);
        runelite.classes.remove(className);
    }

    /**
     * Drops the classes of the other jars, which are read only to map signer checks at startup; class
     * loading reads those jars from disk.
     */
    public synchronized void releaseOther()
    {
        other.classes.clear();
    }
}
