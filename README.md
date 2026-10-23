# Mini FadFed Android Client

A native Android client for **Mini FadFed**, implementing random stranger matching, throwaway anonymous signup, mutual accept gating, and real-time conversation against the live Mini FadFed backend (`https://dev.wefaaq.net/api/@fadfedx` and `wss://dev.wefaaq.net/@fadfedx`).

---

## 1. Project Overview & Features

### Core Flows Implemented
1. **Anonymous Identity & REST Authentication**:
   - Generates persistent UUID `devid` (`udid = devid`).
   - Dynamic session management with `session-id`.
   - HMAC-MD5 token computation (`HMAC-MD5(key = session-id, message = devid) -> 32 lowercase hex characters`).
   - Anonymous signup via `PUT /api/@fadfedx/auth` followed by `PUT /api/@fadfedx/user`.
   - Graceful handling of `406 bad_name` and `429 name_cooldown`.

2. **WebSocket Engine & Lifecycle**:
   - Connects to `wss://dev.wefaaq.net/@fadfedx` using required subprotocol `v2_2.fadfedly.com` and User-Agent `Fadfed/2.4.3 Android/14`.
   - 2-element wire format: `["event_name", { ... }]`.
   - **READY Gate**: Application frames are rejected and not sent before `["session", {"state": "ready"}]`.
   - Automatic feature initialization: Immediately sends `["set", {"features": {"accept": true}}]` upon reaching `ready`.
   - Clean terminal handling for `session:replaced` and unexpected disconnects.

3. **Matchmaking & Mutual Acceptance Gate**:
   - Three search filters: **Both (Free)**, **Male (Paid)**, **Female (Paid)**.
   - **Free Search Pricing Compliance**: For `Both`, the `gender` key is completely omitted from the payload (the backend keys pricing on key presence, not value).
   - Unique request ID per search (`UUID.randomUUID()`).
   - Live radar animation during matchmaking with cancellation support (`["leave", {}]`).
   - `insufficient_coins` error handling: Stops search and returns to Home with clear user feedback.
   - Profile sync (`["sync", {"dataset": "$profile", ...}]`) to update peer name and gender.
   - **Mutual Acceptance Gate**: Conversation launches **only** when both parties accept (`youAccepted == true && peerAccepted == true`).
   - Peer skip / leave handling: Automatically restarts search when peer skips.

4. **Real-Time Conversation & Reliability**:
   - Full bidirectional real-time messaging using `["send", { ... }]`.
   - **4 Message States**:
     - `SENDING`: Created locally, not yet written to open socket (Clock icon).
     - `SENT`: Frame written to open WebSocket (One tick).
     - `READ`: Peer read acknowledgement received (Two ticks).
     - `FAILED`: Unacknowledged after 5s timeout and 3 bounded retries (Warning indicator with tap-to-retry).
   - **Exact Server-Rewritten ID Preservation (§8.6)**: Peer message IDs received as `<by>:<original id>` are acknowledged verbatim.
   - **Acknowledgements**:
     - `status: "received"` sent when incoming message is delivered.
     - `status: "read"` sent when message is actually rendered/visible.
   - Deduplication against duplicate peer retries and timestamp ordering for out-of-order frames.
   - Stale conversation protection: Messages from previous chat sessions are discarded.
   - Clean teardown on peer leave or user manual exit (`["leave", {"chatId": "..."}]`).

---

## 2. Tech Stack & Architecture

- **Language**: Kotlin 2.0.21
- **UI Toolkit**: Jetpack Compose with Material 3
- **Architecture**: MVVM + Clean Architecture (Separation of Transport, Repository/Domain, and UI Presentation)
- **Networking**: OkHttp 4.12.0 (REST client & WebSocket engine)
- **Serialization**: Google Gson 2.11.0
- **Asynchronous / Reactive**: Kotlin Coroutines & StateFlow / SharedFlow
- **Testing**: JUnit 4, Kotlinx Coroutines Test 1.9.0, OkHttp MockWebServer 4.12.0

### Architecture Layers
```
┌──────────────────────────────────────────────────────────┐
│                   Jetpack Compose UI                     │
│  (SignupScreen, HomeScreen, SearchingScreen,             │
│   MatchFoundScreen, ConversationScreen, MatchRootScreen) │
└────────────────────────────┬─────────────────────────────┘
                             │ StateFlow / Events
┌────────────────────────────▼─────────────────────────────┐
│                 ViewModels (MVVM)                        │
│         (SignupViewModel, MatchViewModel)                │
└────────────────────────────┬─────────────────────────────┘
                             │
┌────────────────────────────▼─────────────────────────────┐
│                 Repository / Domain                      │
│   (AuthRepository, MatchSessionManager,                  │
│    ConversationManager, IdentityManager)                 │
└────────────────────────────┬─────────────────────────────┘
                             │
┌────────────────────────────▼─────────────────────────────┐
│                 Transport / Remote Data                  │
│  (FadfedRestClient, FadfedWebSocketClient, FrameParser)  │
└──────────────────────────────────────────────────────────┘
```

---

## 3. Prerequisites & Build Setup

### Prerequisites
- **JDK**: Java 21 (or Android Studio bundled JBR 21)
- **Android Studio**: Android Studio Ladybug / Koala / Hedgehog or newer
- **Android SDK**: `compileSdk = 36`, `minSdk = 24`, `targetSdk = 36`
- **Gradle**: 9.1.0 (included via Gradle Wrapper `gradlew.bat` / `./gradlew`)

### Build & Run Instructions

1. **Clone the repository**:
   ```bash
   git clone <repository_url>
   cd minifadfedandroid
   ```

2. **Run All Unit Tests**:
   - Windows (PowerShell / Command Prompt):
     ```powershell
     .\gradlew.bat test
     ```
   - macOS / Linux:
     ```bash
     ./gradlew test
     ```

3. **Build the Debug APK**:
   - Windows:
     ```powershell
     .\gradlew.bat assembleDebug
     ```
   - Output APK location: `app/build/outputs/apk/debug/app-debug.apk`

4. **Install on Device or Emulator**:
   ```powershell
   .\gradlew.bat installDebug
   ```

---

## 4. Backend Configuration & Known Limitations

### Backend Endpoints
- **REST Base URL**: `https://dev.wefaaq.net/api/@fadfedx`
- **WebSocket URL**: `wss://dev.wefaaq.net/@fadfedx`
- **Tenant**: `fadfedx`
- **Subprotocol**: `v2_2.fadfedly.com`
- **User-Agent**: `Fadfed/2.4.3 Android/14`

### Known Backend Behaviors & Limitations
- **No Outgoing Delivery Receipt**: The FadFed WebSocket backend does not send an acknowledgement when an outgoing message arrives at the server. `SENT` is optimistically confirmed upon writing to an open socket. `READ` confirmation depends on the peer client acknowledging the message.
- **Shared Live Backend**: The dev backend is a live, shared environment. When queuing for matches, you may match with other live testers.
- **Paid Filters on Fresh Accounts**: Fresh test accounts start with 0 coins. Searching for `Male` or `Female` triggers `insufficient_coins`. `Both` search is completely free.
- **Omitted Features (Out of Scope by assignment design)**:
  - Voice / video calls
  - Typing indicators (type event)
  - Image attachments
  - Social login / phone verification
  - Friends / contacts list

---

## 5. Manual Dual-Client Testing Guide

To test the entire flow end-to-end between two clients:

### Setup
Run two independent clients with different `devid` values:
- **Client A**: Real Android device or Android Emulator A (e.g. Name: `Alice`, Gender: `Female`).
- **Client B**: Android Emulator B or a desktop browser / companion client (e.g. Name: `Bob`, Gender: `Male`).

### Test Procedure
1. **Signup**:
   - Enter name and select gender on Client A and Client B.
   - Tap **Continue**. Verify profile created via REST API and persisted.
2. **Both (Free) Search**:
   - On Client A, select **Both (Free)** and tap **Start Matching**. Verify searching radar animation.
   - On Client B, select **Both (Free)** and tap **Start Matching**.
3. **Match Found**:
   - The server matches Client A and Client B.
   - Client A displays `Bob` (with Male indicator).
   - Client B displays `Alice` (with Female indicator).
4. **Mutual Acceptance Gate**:
   - Client A taps **Accept**: Client A status shows *"Waiting for peer..."*.
   - Verify neither client enters chat yet.
   - Client B taps **Accept**: Mutual acceptance gate unlocks. Both clients navigate into the conversation screen simultaneously.
5. **Real-time Messaging & Ticks**:
   - Client A sends `"Hello Bob!"`:
     - Message displays with a clock (`SENDING`) for an instant, then transitions to **one tick** (`SENT`).
   - Client B receives `"Hello Bob!"` instantly.
   - As Client B views the message, Client B sends a `read` ack.
   - Client A's message tick transitions to **two ticks** (`READ`, in primary accent color).
   - Client B sends `"Hi Alice, how are you?"`:
     - Message appears on Client A, and Client B sees double ticks.
6. **Timeout & Retries**:
   - Put Client B into Airplane Mode.
   - Client A sends `"Are you still there?"`.
   - Message remains at **one tick** (`SENT`).
   - Client A retries after 5s, 10s, and 15s.
   - At 20s (after 3 retries), the message transitions to **Failed** (red warning indicator) with *"Failed to send. Tap to retry"*.
7. **Leave Conversation**:
   - Client A taps the **X** (Leave) button and confirms.
   - Client A returns to the Home screen.
   - Client B receives peer leave notification and returns to Home.

---

## 6. AI Tools Usage Disclosure

In compliance with the assignment instructions regarding transparency:
- **AI Tool**: Antigravity AI Coding Assistant (by Google DeepMind).
- **Usage**:
  - Assisted in designing the HMAC-MD5 token generator and verifying test vectors against the assignment specification.
  - Formulated the WebSocket frame parser and sealed event hierarchies.
  - Scaffolded Jetpack Compose screens (Signup, Home, Searching radar animation, Match, and Chat UI).
  - Drafted unit test suites across all milestones for REST, WebSocket lifecycle, mutual acceptance, message state transitions, and retry loops.
  - Every line of code, protocol payload, and test outcome was audited, run, and verified against the real project gradle wrapper and assignment PDF.
