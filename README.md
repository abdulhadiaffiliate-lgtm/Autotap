# AutoTapper — Build Your APK From Your Phone

This is a complete Android Studio project for an auto-tap app:
- Uses Android's **Accessibility Service** to dispatch real taps system-wide (the standard, legitimate mechanism Android provides for this)
- Configurable **interval (ms)**, **random jitter %**, **tap count or infinite**
- **Draggable floating bubble** to start/stop without opening the app
- **Crosshair picker** to set exactly where it taps
- Modern dark UI with cards and gradient buttons

I can't compile this into a finished .apk myself (no Android SDK access in my sandbox), but you can get a finished APK without a PC by letting GitHub's free build servers do it. Takes about 10 minutes, one time.

## Steps (all on your phone)

### 1. Create a GitHub account
If you don't have one: go to github.com in your phone browser → Sign up. Free.

### 2. Create a new empty repository
- Tap **+** → **New repository**
- Name it `autotapper` (anything works)
- Set to **Public** (required for free Actions minutes) or Private (also free, just slower queue)
- Don't add a README/gitignore — leave it empty
- Tap **Create repository**

### 3. Upload these files
GitHub's mobile web UI lets you upload files/folders directly:
- Open your new repo → tap **Add file** → **Upload files**
- Upload the entire project folder I'm giving you, preserving the folder structure (drag-drop the whole zip's extracted contents, or upload the zip and use GitHub's "upload" which supports folders on most mobile browsers)
- Commit directly to the `main` branch

**If your mobile browser won't let you upload nested folders:** install the **GitHub mobile app** instead — it has limited file upload too, so the most reliable phone method is actually using a file manager app that supports "extract zip" + a browser that allows folder upload (Chrome on Android supports folder upload in repo file upload). Alternatively use **Termux** (free terminal app) with `git`:
```
pkg install git
git clone https://github.com/YOUR_USERNAME/autotapper.git
cd autotapper
# copy/extract the project files here
git add .
git commit -m "add project"
git push
```

### 4. Let GitHub Actions build it
The moment you push, the workflow in `.github/workflows/build.yml` runs automatically:
- Go to your repo → **Actions** tab
- You'll see a "Build APK" run in progress (takes ~3-5 min)
- When it's green ✅, tap into the run → scroll to **Artifacts** → download `AutoTapper-debug-apk`
- It downloads as a `.zip` — open it with any file manager / zip app, extract `app-debug.apk`

### 5. Install it
- Tap the extracted `.apk` file
- Android will warn about "install unknown apps" — allow it for your browser/file manager (Settings prompt appears automatically)
- Install

### 6. First-time setup in the app
1. Open AutoTapper
2. Tap **Enable Accessibility Service** → find AutoTapper in the list → toggle on → confirm
3. Tap **Allow Overlay Permission** → toggle on
4. Optionally tap **Set Tap Point** and drag the crosshair to where you want taps to land
5. Adjust interval / jitter / tap count sliders
6. Tap **Start** — a small floating bubble appears; tap it to pause/resume, drag it anywhere

## Notes
- This is a debug build (unsigned with a debug key) — totally fine for personal installs, just can't go on the Play Store as-is.
- The accessibility service only dispatches gestures — it doesn't read screen content, so it has minimal permissions.
- I did not build in any detection-evasion features (see note in chat) — this is a straightforward, legitimate automation tool.
