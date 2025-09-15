# External Plugin Development
This document provides guidelines and best practices for developing external plugins for VitaLite.

## Getting Started

### Needed Imports
To get the current VitaLite version, look in the vitalite main module gradle.
```kt
val runeLiteVersion = "latest.release"
val vitaVersion = "xx.xx.xx"
compileOnly("net.runelite:client:$runeLiteVersion")
compileOnly("com.tonic:base-api:$vitaVersion")
compileOnly("com.tonic:api:$vitaVersion")
```

for repositories {} section of your gradle, you'll want these:
```kt
repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.runelite.net")
        content {
            includeGroupByRegex("net\\.runelite.*")
        }
    }
    mavenCentral()
}
```

### Plugin Structure
Plgins can be written in the normal way extending runelites Plugin class and so on. However there is another enanced Plugin supertype that VitaLite supports `VitaPlugin`. 

Sample:
```java
@PluginDescriptor(
        name = "Sample Plugin", 
        description = "A sample VitaPlugin", 
        tags = {"vita", "sample"}
)
public class SampleVitaPlugin extends VitaPlugin
{
    @Override
    public void loop() throws Exception
    {
    }
}
```

`VitaPlugin` extends `Plugin` and adds the feature of providing the overridable `loop()` method:
```java
/**
 * Overridable loop() method. It is safe to sleep in, but as a result is
 * not thread safe, so you must use invoke()'s to do operations that require
 * thread safety. It is started from the start of a gametick.
 * @throws Exception exception
 */
public void loop() throws Exception
{
}
```

The `loop()` method is called at the start of every game tick. It is safe to sleep in, but as a result is not thread safe, so you must use `invoke()`'s to do operations that require thread safety. It is a mechanism by which to simplify automation. There is a special package of the api specifically designed for use in threaded/looped automation at `com.tonic.api.threaded.*`.