package com.tonic.bootstrap.beans;

import lombok.Data;

import java.util.Map;

@Data
public class Bootstrap
{
    private String version;
    private Artifact[] artifacts;

    private String[] clientJvm9Arguments;

    private String[] clientJvm17Arguments;
    private String[] clientJvm17WindowsArguments;
    private String[] clientJvm17MacArguments;

    private String[] launcherJvm11WindowsArguments;
    private String[] launcherJvm11MacArguments;
    private String[] launcherJvm11Arguments;

    private String[] launcherJvm17WindowsArguments;
    private String[] launcherJvm17MacArguments;
    private String[] launcherJvm17Arguments;

    private String requiredLauncherVersion;
    private String requiredJVMVersion;

    private Map<String, String> launcherWindowsEnv;
    private Map<String, String> launcherMacEnv;
    private Map<String, String> launcherLinuxEnv;

    private Update[] updates;
}