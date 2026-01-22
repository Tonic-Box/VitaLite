# Authentication and Login

VitaLite supports several login related workflows:

- Auto login via CLI flags.
- Enhanced Profiles plugin, which can store and apply legacy or Jagex account login state.

## Auto Login via CLI

Two CLI options feed the same internal `AutoLogin` credential store.
Source: `src/main/java/com/tonic/vitalite/Main.java:main` (calls `AutoLogin.setCredentials`)
Source: `base-api/src/main/java/com/tonic/services/AutoLogin.java`

### Legacy accounts (`--legacyLogin`)

Syntax:

- `--legacyLogin user:pass`

The value must split into exactly 2 parts using `:`. If it does, it is stored as is.
Source: `base-api/src/main/java/com/tonic/services/AutoLogin.java:setCredentials`

### Jagex accounts (`--jagexLogin`)

Syntax:

- `--jagexLogin sessionId:characterId:displayName`

If the value splits into exactly 3 parts using `:`, it is stored as is.
Source: `base-api/src/main/java/com/tonic/services/AutoLogin.java:setCredentials`

Alternatively, `--jagexLogin` can be a path to a properties file. If the value does not split into 2 or 3 parts, the code treats it as a file path and loads:

- `JX_SESSION_ID`
- `JX_CHARACTER_ID`
- `JX_DISPLAY_NAME`

Source: `base-api/src/main/java/com/tonic/services/AutoLogin.java:readCredentials`

### When auto login is applied

At runtime, auto login is consumed by `GameManager` and passed into `LoginService.login(...)`.
Source: `api/src/main/java/com/tonic/services/GameManager.java` (see lines around `AutoLogin.getCredentials`)

## Enhanced Profiles Plugin

The built in Enhanced Profiles plugin provides UI and storage for multiple profiles.
Source: `plugins/src/main/java/com/tonic/plugins/profiles/ProfilesPlugin.java`

### Where profiles are stored

Profiles are stored in `${user.home}/.runelite/vitalite/profiles.db`.
Source: `plugins/src/main/java/com/tonic/plugins/profiles/session/ProfilesSession.java` (`DIRECTORY`)

The file is encrypted using an AES key derived from a device ID. Moving `profiles.db` to another machine can make it impossible to decrypt.
Source: `plugins/src/main/java/com/tonic/plugins/profiles/session/ProfilesSession.java:generateAESKey`

### Applying a profile to the login screen

When applying a profile:

- For legacy accounts, the plugin sets username and password and sets the login index to `2`.
  Source: `plugins/src/main/java/com/tonic/plugins/profiles/session/ProfilesSession.java:setLoginWithUsernamePassword`
- For Jagex accounts, the plugin clears username and password, sets account type to Jagex, sets the login index to `10`, and sets session and character fields.
  Source: `plugins/src/main/java/com/tonic/plugins/profiles/session/ProfilesSession.java:setLoginWithJagexAccount`

## Jagex Account OAuth Flow (Profiles Plugin)

The profiles plugin includes a Jagex login helper that opens a browser and waits for an OAuth redirect to localhost.
Source: `plugins/src/main/java/com/tonic/plugins/profiles/jagex/JagexAccountService.java:requestIdToken`

### Operational constraints

- The embedded HTTP server binds to port `80`.
  - On many systems this requires elevated privileges, and it can conflict with other software already listening on port 80.
  - Source: `plugins/src/main/java/com/tonic/plugins/profiles/jagex/JagexHttpServer.java:start`
- The server auto stops after 5 minutes.
  - Source: `plugins/src/main/java/com/tonic/plugins/profiles/jagex/JagexHttpServer.java:start`

### External requests

The plugin makes network requests to Jagex endpoints using OkHttp.
Source: `plugins/src/main/java/com/tonic/plugins/profiles/jagex/JagexTokenExchange.java`
