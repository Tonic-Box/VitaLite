package com.tonic;

import com.tonic.vitalite.Main;
import com.tonic.vitalite.SelfUpdate;
import com.tonic.vitalite.Versioning;
import java.io.File;
import java.net.URISyntaxException;
import static com.tonic.util.JVMLauncher.launchInNewJVM;
import static com.tonic.vitalite.Versioning.getLiveRuneliteVersion;
import static com.tonic.vitalite.Versioning.getVitaLiteVersion;

public class VitaLite {
    public static void main(String[] args) {
        try {
            String currentVersion = getVitaLiteVersion();
            if(Versioning.isRunningFromShadedJar())
            {
                String liveRlVersion = getLiveRuneliteVersion();
                if(!currentVersion.startsWith(liveRlVersion))
                {
                    System.out.println("VitaLite version " + currentVersion + " is out of date. Latest version is " + liveRlVersion + ".");
                    Main.optionsParser.parse(args);
                    new SelfUpdate().checkAndUpdate();
                    System.err.println("Warning: You are running VitaLite version " + currentVersion + " but the latest version is " + liveRlVersion + ". Please update to the latest version.");
                    return;
                }
                String latestVitaRelease = Versioning.getLatestVitaLiteReleaseTag();
                if(!currentVersion.equals(latestVitaRelease))
                {
                    System.out.println("VitaLite version " + currentVersion + " is out of date. Latest version is " + latestVitaRelease + ".");
                    Main.optionsParser.parse(args);
                    SelfUpdate.showUpdateAvailableDialog(currentVersion, latestVitaRelease, true);
                    System.err.println("Warning: You are running VitaLite version " + currentVersion + " but the latest version is " + latestVitaRelease + ". Please update to the latest version.");
                    return;
                }
            }
            System.out.println("VitaLite version " + currentVersion + " is up to date.");
            String[] newArgs = new String[args.length + 1];
            newArgs[0] = "-safeLaunch";
            System.arraycopy(args, 0, newArgs, 1, args.length);
            launchInNewJVM("com.tonic.vitalite.Main", buildFullClasspath(), newArgs);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String buildFullClasspath() throws URISyntaxException {
        String currentClasspath = System.getProperty("java.class.path");
        File sourceLocation = new File(VitaLite.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI());
        String myLocation = sourceLocation.getAbsolutePath();
        if (!currentClasspath.contains(myLocation)) {
            return myLocation + File.pathSeparator + currentClasspath;
        }
        return currentClasspath;
    }
}
