# MechVac Task: Android app

This app opens your MechVac Task website inside a proper Android app and adds what a website cannot do on free hosting:

- **Instant notifications, even when the app is closed**: new tasks, approvals, comments, deadline reminders, announcements and chat arrive within seconds through Firebase (free, by Google), the same way WhatsApp and Gmail deliver theirs. **No permanent notification** stays on the phone.
- **Silent backup check every 15 minutes**, so nothing is missed. It starts again by itself after the phone restarts.
- Without Firebase the app still works: it checks every 15 minutes. If your hosting cannot reach Google, the super admin can switch on an always-on check in Company settings (that one shows a small permanent notification, which Android requires).
- Photos and files can be attached, and downloads are saved to the phone's Downloads folder.

Your website must run the latest MechVac Task version (with `app_api.php`).

---

## Step 1: Set your details (2 minutes)

Open `gradle.properties` with Notepad and change:

| Setting | What to put |
|---|---|
| `APP_URL` | Your MechVac Task address, e.g. `https://task.yourcompany.com` (include the folder if it is in one) |
| `APP_NAME` | Name under the icon, e.g. `MechVac Task` |
| `APP_ID` | Keep `com.mechvac.task`. **Never change this after you share the app.** |

## Step 2: Set up instant notifications with Firebase (one time, about 10 minutes, free)

1. Open https://console.firebase.google.com and sign in with a Google account.
2. Click **Create a project** (or **Add project**), name it e.g. `MechVac Task`, turn **off** Google Analytics, and click **Create project**.
3. On the project page click the **Android** icon (**Add app**).
   - **Android package name:** `com.mechvac.task` (the same as `APP_ID` in `gradle.properties`).
   - Click **Register app**, then **Download google-services.json**. Skip the remaining steps (click Next / Continue to console).
   - You do **not** put this file into the app. It goes to your website instead (step 5).
4. Click the **gear icon > Project settings > Service accounts**, then **Generate new private key > Generate key**. A second `.json` file downloads. Keep it private: it lets your server send notifications.
5. Sign in to MechVac Task as super admin, open **Company settings > Phone app: instant notifications**, choose both files, and click **Save**. It should say **Ready**.
6. After installing the app on your phone and signing in, click **Send test to my phone** there.

If it says the server **could not reach Google**, your hosting blocks outgoing connections. Then either move to hosting that allows them, or tick **Always-on check without Firebase** (phones then show a small permanent notification).

## Step 3: Build the APK for free on GitHub

1. Sign in at https://github.com.
2. Click **New repository**, give it a name (e.g. `mechvac-task-android`), select **Private** (important: it contains your signing key), and click **Create**.
3. Click **uploading an existing file**, drag in **all files and folders from this project** (including the hidden `.github` folder), then click **Commit changes**.
   - On Windows, turn on View > Show > **Hidden items** to see `.github`.
4. Open the **Actions** tab. A build named **Build MechVac Task APK** starts by itself (about 5–8 minutes).
5. When it shows a green tick, open it and scroll to **Artifacts**. Download **MechVacTask-APK** (a zip containing `MechVacTask-1.0.apk`).

Every time you change a file and commit, GitHub builds a new APK.

## Step 4: Share with staff

- Send the APK on WhatsApp or email.
- On the phone: tap the file → allow **Install unknown apps** when asked → **Install**. If Play Protect warns, tap **More details → Install anyway**.
- Open the app and sign in. The app asks, one by one:
  - Notifications → **Allow**
  - Battery → **Allow** (so the phone does not stop notifications)
  - Autostart (Xiaomi, Oppo, Vivo, Realme, Samsung…) → switch **on** for this app
- Check: **My profile → Notifications on this device** should say **"On, instant"**. Tap **Send a test notification**.

### Phones that stop apps in the background
- **Xiaomi / Redmi / POCO:** Settings → Apps → the app → Autostart **on**, Battery saver **No restrictions**
- **Oppo / Realme / Vivo / iQOO:** Settings → Battery → the app → Allow background activity / **Don’t optimise**
- **Samsung:** Settings → Battery → Background usage limits → add the app to **Never sleeping apps**
- **OnePlus:** Settings → Battery → Battery optimisation → the app → **Don’t optimise**

## Updating the app
Changes to the website appear in the app immediately, with no new APK needed.
Only for a new name, icon or address: edit `gradle.properties`, raise `VERSION_CODE` by 1 (and `VERSION_NAME`), commit, and share the new APK. It installs over the old one.

## Keep these safe
`mechvac-task-release.jks` and `keystore.properties` are the app's signing key. Keep the repository **private** and keep a backup copy. Without them, phones cannot update the app.
