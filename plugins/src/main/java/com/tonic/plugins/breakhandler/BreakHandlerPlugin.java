package com.tonic.plugins.breakhandler;

import com.google.inject.Inject;
import com.tonic.api.game.ClientScriptAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.data.LoginMessage;
import com.tonic.data.LoginResponse;
import com.tonic.data.LoginResponseType;
import com.tonic.services.breakhandler.Break;
import com.tonic.services.breakhandler.BreakHandler;
import com.tonic.services.breakhandler.settings.Property;
import com.tonic.plugins.breakhandler.ui.BreakHandlerPanel;
import com.tonic.plugins.profiles.data.ProfileRef;
import com.tonic.services.ConfigManager;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.WorldUtil;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldRegion;
import net.runelite.http.api.worlds.WorldResult;
import net.runelite.http.api.worlds.WorldType;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

@PluginDescriptor(
        name = "# Break Handler",
        description = "Schedules and takes breaks automatically"
)
public class BreakHandlerPlugin extends Plugin
{
    private static final BufferedImage pluginIcon = ImageUtil.loadImageResource(BreakHandlerPlugin.class, "clock.png");

    private static final int LOGIN_ATTEMPT_LIMIT = 5;
    private static final int LOGIN_ATTEMPT_BASE_DELAY = 30;
    private static final int SET_WORLD_BASE_DELAY = 15;
    private static final int LOGOUT_ATTEMPT_LIMIT = 30;
    private static final int LOGOUT_ATTEMPT_DELAY = 10;

    private static final int SWITCH_TAB_SCRIPT_ID = 915;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private ClientThread clientThread;

    @Inject
    private Client client;

    @Inject
    private WorldService worldService;

    private NavigationButton navigationButton;
    private BreakHandlerPanel panel;

    private ScheduledExecutorService exec;
    private ScheduledFuture<?> updater;

    private ConfigManager configManager;
    @Inject
    private BreakHandler breakHandler;
    private State state = State.IDLE;

    private int logoutAttempts = 0;
    private int logoutAttemptDelay = 0;
    private int loginAttempts = 0;
    private int loginAttemptDelay = 0;
    private int targetWorld = -1;
    private BreakAccount account;
    private boolean relogging;
    private String lastLoginMessage;

    @Override
    protected void startUp()
    {
        panel = new BreakHandlerPanel();
        navigationButton = NavigationButton.builder()
                .panel(panel)
                .priority(-999)
                .icon(pluginIcon)
                .tooltip("Break Handler")
                .build();

        clientToolbar.addNavigation(navigationButton);

        state = State.IDLE;
        loginAttempts = 0;
        loginAttemptDelay = 0;
        logoutAttempts = 0;
        logoutAttemptDelay = 0;
        targetWorld = -1;
        relogging = false;
        lastLoginMessage = null;
        exec = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Vita-BreakHandler");
            t.setDaemon(true);
            return t;
        });
        updater = exec.scheduleAtFixedRate(() ->
                clientThread.invoke(this::update), 1, 1, TimeUnit.SECONDS);
        configManager = breakHandler.getConfigManager();
        account = new BreakAccount(breakHandler);
        account.migrate();
        checkVmArgs();
    }

    @Override
    protected void shutDown()
    {
        breakHandler.cancel();
        clientToolbar.removeNavigation(navigationButton);
        if (updater != null)
        {
            updater.cancel(true);
            updater = null;
        }

        exec.shutdownNow();
        exec = null;
    }

    private void checkVmArgs()
    {
        String profile = System.getProperty("vProfile", "");

        if (!profile.isEmpty() && !account.chooseByName(profile))
        {
            breakHandler.log("[%s] The launch profile %s is not in Profiles, keeping %s", "Break Handler", profile, account.describe());
        }

        boolean autoLogin = System.getProperty("vAutoLogin") != null;
        this.configManager.setProperty(Property.ACCOUNT_AUTO_LOGIN.key(), autoLogin);
    }

    @Subscribe
    private void onGameTick(GameTick event)
    {
        if (state == State.LOGOUT)
        {
            logout();
        }
    }

    @Subscribe
    private void onGameStateChanged(GameStateChanged event)
    {
        GameState gameState = event.getGameState();

        if (gameState == GameState.LOGGED_IN)
        {
            loginAttempts = 0;
            targetWorld = -1;
            relogging = false;
            lastLoginMessage = null;
        }

        if (gameState == GameState.LOGIN_SCREEN && state == State.LOGOUT)
        {
            logoutAttempts = 0;
            state = State.IDLE;
        }
    }

    private void logout()
    {
        if (logoutAttemptDelay > 0)
        {
            logoutAttemptDelay--;
            return;
        }

        if (logoutAttempts > LOGOUT_ATTEMPT_LIMIT)
        {
            breakHandler.log("[%s] Exceeded max attempts trying to logout, " +
                            "cancelling breaks this session.", "Break Handler");
            return;
        }

        if (client.getVarcIntValue(VarClientID.TOPLEVEL_PANEL) != 10)
        {
            ClientScriptAPI.runScript(SWITCH_TAB_SCRIPT_ID, 10);
            breakHandler.log("[%s] Switching to logout interface tab", "Break Handler");
        }

        if (isLogoutButtonVisible())
        {
            WidgetAPI.interact(1, InterfaceID.Logout.LOGOUT, -1, -1);
            logoutAttempts++;
            loginAttemptDelay = LOGOUT_ATTEMPT_DELAY;
            breakHandler.log("[%s] Logout attempted, attempt %d", "Break Handler", logoutAttempts);
            return;
        }

        if (isLogoutDoorVisible())
        {
            WidgetAPI.interact(1, InterfaceID.Worldswitcher.LOGOUT, -1, -1);
            logoutAttempts++;
            loginAttemptDelay = LOGOUT_ATTEMPT_DELAY;
            breakHandler.log("[%s] Logout attempted, attempt %d", "Break Handler", logoutAttempts);
        }
    }

    private boolean isLogoutButtonVisible()
    {
        Widget logoutButton = client.getWidget(InterfaceID.Logout.LOGOUT);
        return WidgetAPI.isVisible(logoutButton);
    }

    private boolean isLogoutDoorVisible()
    {
        Widget logoutDoorButton = client.getWidget(InterfaceID.Worldswitcher.LOGOUT);
        return WidgetAPI.isVisible(logoutDoorButton);
    }

    private void update()
    {
        updateBreaks();
        SwingUtilities.invokeLater(panel::refreshBreakList);
        updateState();

        if (state != State.LOGIN)
        {
            boolean autoLogin = configManager.getBooleanOrDefault(
                    Property.ACCOUNT_AUTO_LOGIN.key(), false);

            if (autoLogin)
            {
                handleAutoLogin();
            }

            return;
        }

        login();
    }

    private void updateBreaks()
    {
        GameState gameState = client.getGameState();

        if (gameState == GameState.LOGGED_IN)
        {
            breakHandler.notifyLogin();
        }

        if (gameState == GameState.LOGIN_SCREEN)
        {
            breakHandler.notifyLogout();
        }
    }

    private void updateState()
    {
        GameState gameState = client.getGameState();
        List<Break> allBreaks = breakHandler.getAllBreaks();

        if (breakHandler.isReadyToBreak() && gameState == GameState.LOGGED_IN)
        {
            state = State.WAITING_TO_LOGOUT;

            for (Break b : allBreaks)
            {
                if (b.getCanAccess().getAsBoolean()
                        && b.getCanStart().getAsBoolean() && b.isBreakReady())
                {
                    state = State.LOGOUT;
                    break;
                }
            }
            return;
        }

        if (breakHandler.isReadyToLogin() && gameState == GameState.LOGIN_SCREEN)
        {
            state = State.LOGIN;
            return;
        }

        state = State.IDLE;
    }

    private void handleAutoLogin()
    {
        GameState gameState = client.getGameState();

        boolean shouldLogin = gameState == GameState.LOGIN_SCREEN
                && !breakHandler.isBreaking();

        if (shouldLogin)
        {
            if (client.getGameState() == GameState.LOGIN_SCREEN)
            {
                login();
            }
        }
    }

    private void login()
    {
        if (loginAttemptDelay > 0)
        {
            loginAttemptDelay--;
            return;
        }

        GameState gameState = client.getGameState();

        if (gameState == GameState.LOGGING_IN)
        {
            return;
        }

        int world = client.getWorld();

        boolean hop = configManager.getBooleanOrDefault(Property.HOP_ENABLED.key(),
                false);

        if (hop)
        {
            if (targetWorld == -1 || world != targetWorld)
            {
                setWorld();
                loginAttemptDelay = SET_WORLD_BASE_DELAY
                        + ThreadLocalRandom.current().nextInt(0, 15);
                return;
            }
        }

        if (loginAttempts >= LOGIN_ATTEMPT_LIMIT)
        {
            cancel("no login succeeded in " + LOGIN_ATTEMPT_LIMIT + " attempts" + (lastLoginMessage == null ? "" : ", the last said: " + lastLoginMessage));
            return;
        }

        BreakAccount.Result result = account.login();
        switch (result)
        {
            case STARTED:
                loginAttempts++;
                loginAttemptDelay = LOGIN_ATTEMPT_BASE_DELAY
                        + ThreadLocalRandom.current().nextInt(0, 15);
                relogging = true;
                breakHandler.log("[%s] Logging in to %s, attempt %d",
                        "Break Handler", account.describe(), loginAttempts);
                break;
            case NO_ACCOUNT:
                cancel("the client has no account to log back in to; pick a profile under Account in the Break Handler settings");
                break;
            case MISSING_PROFILE:
                cancel("the chosen profile is no longer in Profiles: " + account.describe());
                break;
            default:
                break;
        }
    }

    /**
     * Logs why a login attempt failed, and cancels breaks at once when retrying cannot help: the account is banned, locked or restricted, or its credentials or Jagex session were refused.
     *
     * @param response the client's login response
     */
    @Subscribe
    private void onLoginResponse(LoginResponse response)
    {
        if (!relogging)
        {
            return;
        }
        String message = response.getMessage() == null ? response.name() : response.getMessage();
        breakHandler.log("[%s] Login failed: %s", "Break Handler", message);
        LoginResponseType type = response.getType();
        boolean restricted = type == LoginResponseType.ACCOUNT_RESTRICTION && response != LoginResponse.MEMBERS_ONLY;
        if (type == LoginResponseType.BANNED || type == LoginResponseType.LOCKED || type == LoginResponseType.AUTHENTICATION || restricted)
        {
            ProfileRef chosen = account.chosenProfile();
            boolean legacy = chosen != null && chosen.getKind() == ProfileRef.Kind.LEGACY;
            String hint = type == LoginResponseType.AUTHENTICATION && !legacy ? "; if the Jagex session expired, sign in again in Profiles" : "";
            cancel("the login was refused (" + message + ")" + hint);
        }
    }

    /**
     * Remembers the login screen's message while logging back in, for the log when the attempts run out.
     *
     * @param message the login screen's message
     */
    @Subscribe
    private void onLoginMessage(LoginMessage message)
    {
        if (relogging && !message.toString().isEmpty())
        {
            lastLoginMessage = message.toString();
        }
    }

    private void cancel(String reason)
    {
        relogging = false;
        breakHandler.log("[%s] Cancelling breaks this session: %s", "Break Handler", reason);
        breakHandler.cancel();
    }

    private void setWorld()
    {
        WorldResult worldResult = worldService.getWorlds();
        if (worldResult == null)
        {
            breakHandler.log("[%s] Unable to retrieve worlds, please wait", "Break Handler");
            return;
        }

        World currentWorld = worldResult.findWorld(client.getWorld());

        if (currentWorld == null)
        {
            breakHandler.log("[%s] Unable to find current world, please wait", "Break Handler");
            return;
        }

        List<World> worlds = new ArrayList<>(worldResult.getWorlds());
        EnumSet<WorldRegion> badRegions = badRegions();
        EnumSet<WorldType> badTypes = badTypes();
        worlds.removeIf(w -> w.getTypes().stream().anyMatch(badTypes::contains)
                || badRegions.contains(w.getRegion()));
        // uncomment to use grid master worlds
        worlds.removeIf(w -> w.getActivity().toLowerCase().contains("grid master"));

        int index = ThreadLocalRandom.current().nextInt(0, worlds.size() - 1);
        World target = worlds.get(index);
        targetWorld = target.getId();
        breakHandler.log("[%s] Setting target world to %d", "Break Handler", targetWorld);
        hop(target);
    }

    private void hop(World world)
    {
        final net.runelite.api.World rsWorld = client.createWorld();
        rsWorld.setActivity(world.getActivity());
        rsWorld.setAddress(world.getAddress());
        rsWorld.setId(world.getId());
        rsWorld.setLocation(world.getRegion().getId());
        rsWorld.setTypes(WorldUtil.toWorldTypes(world.getTypes()));

        client.changeWorld(rsWorld);
    }

    private EnumSet<WorldRegion> badRegions()
    {
        boolean disableUs = configManager.getBooleanOrDefault(Property.DISABLE_REGION_US.key(),
                false);
        boolean disableUk = configManager.getBooleanOrDefault(Property.DISABLE_REGION_UK.key(),
                false);
        boolean disableDe = configManager.getBooleanOrDefault(Property.DISABLE_REGION_DE.key(),
                false);
        boolean disableAu = configManager.getBooleanOrDefault(Property.DISABLE_REGION_AU.key(),
                false);

        EnumSet<WorldRegion> regions = EnumSet.noneOf(WorldRegion.class);

        if (disableUs)
        {
            regions.add(WorldRegion.UNITED_STATES_OF_AMERICA);
        }

        if (disableUk)
        {
            regions.add(WorldRegion.UNITED_KINGDOM);
        }

        if (disableDe)
        {
            regions.add(WorldRegion.GERMANY);
        }

        if (disableAu)
        {
            regions.add(WorldRegion.AUSTRALIA);
        }

        return regions;
    }

    private EnumSet<WorldType> badTypes()
    {
        boolean f2pOnly = configManager.getBooleanOrDefault(Property.F2P_ONLY.key(),
                false);

        boolean disableHighRisk = configManager.getBooleanOrDefault(Property.DISABLE_HIGH_RISK.key(),
                true);
        boolean disableLms = configManager.getBooleanOrDefault(Property.DISABLE_LMS.key(),
                false);
        boolean disableSkillTotal = configManager.getBooleanOrDefault(Property.DISABLE_SKILL_TOTAL.key(),
                false);

        EnumSet<WorldType> types = EnumSet.of(WorldType.BETA_WORLD, WorldType.NOSAVE_MODE,
                WorldType.FRESH_START_WORLD, WorldType.SEASONAL, WorldType.DEADMAN,
                WorldType.QUEST_SPEEDRUNNING, WorldType.PVP_ARENA, WorldType.PVP);

        if (f2pOnly)
        {
            types.add(WorldType.MEMBERS);
        }

        if (disableHighRisk)
        {
            types.add(WorldType.HIGH_RISK);
        }

        if (disableSkillTotal)
        {
            types.add(WorldType.SKILL_TOTAL);
        }

        if (disableLms)
        {
            types.add(WorldType.LAST_MAN_STANDING);
        }

        return types;
    }

    private enum State
    {
        IDLE,
        WAITING_TO_LOGOUT,
        LOGOUT,
        LOGIN
    }
}
