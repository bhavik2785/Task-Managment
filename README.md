# MechVac Task: Android app

This builds a real Android app (APK) for your MechVac Task website, on GitHub, for free.
The app opens your site full screen like any installed app, stays live like the website,
and receives notifications for tasks, approvals, reminders, announcements and chat **even when it is closed**.

Requirements: your website must open with **https://**, phones need Android 7 or newer with Google Chrome installed (almost all phones in use today).

## Step 1: Put this folder on GitHub (one time)
1. Create a free account at https://github.com and sign in.
2. Click **+** (top right), then **New repository**. Name it `mechvac-task-app`, choose **Private**, and click **Create repository**.
3. On the new page click **uploading an existing file**. Open this folder on your computer, select **everything inside it**, drag it into the page, and click **Commit changes**.
   - The hidden folder `.github` must be uploaded too. If your computer hides it (Mac: press Cmd+Shift+. in Finder; Windows: View > Show > Hidden items), or if it does not appear in the repository afterwards, click **Add file > Create new file**, type the name `.github/workflows/build-android.yml`, paste the content of that file from this folder, and commit.

## Step 2: Set your website address
1. In the repository click `app-config.properties`, then the pencil icon (Edit).
2. Change `siteUrl=https://your-website.com` to your real address, for example `siteUrl=https://task.mechvac.com` (include a folder if the app is in one).
3. Click **Commit changes**. The build starts by itself.

## Step 3: Download the app
1. Open the **Actions** tab. Wait until "Build Android app" shows a green tick (about 5-8 minutes).
2. Click the run, scroll to **Artifacts**, and download **MechVacTask-android** (a zip).
3. Inside: `MechVacTask.apk` (install this on phones), `MechVacTask-PlayStore.aab` (only for the Play Store), `WEBSITE-LINK.txt`, `assetlinks.json`, and on the first build `SAVE-THIS-KEY.txt`.

## Step 4: Save the signing key (first build only, very important)
Open `SAVE-THIS-KEY.txt` and follow it: in the repository go to **Settings > Secrets and variables > Actions > New repository secret** and add `KEYSTORE_BASE64` and `KEYSTORE_PASSWORD`.
Without this, every build gets a new key and phones will refuse to update the app.

## Step 5: Link the app to your website (removes the address bar)
Open `WEBSITE-LINK.txt`. In MechVac Task go to **Company settings > Android app**, paste the package name and the SHA-256 fingerprint, and save.
(Or upload `assetlinks.json` to `https://your-site/.well-known/assetlinks.json` yourself.)
Until this is done the app still works, but shows a small address bar at the top.

## Step 6: Install on phones
Send `MechVacTask.apk` to staff (WhatsApp, email, or Drive). On the phone tap it, allow **Install unknown apps** when asked, and install.
Open the app, sign in, and tap **Turn on** for notifications.

## Updating the app later
Most changes need no new app: anything you change on the website appears in the app immediately.
Only rebuild if you change the name, icon or address: edit `app-config.properties`, raise `versionCode` by 1, and commit. Install the new APK over the old one.

## Play Store (optional)
Upload `MechVacTask-PlayStore.aab` in Google Play Console (one-time USD 25 developer fee). Play will show you a second fingerprint (App signing key); add it in Company settings too, one per line.

## iPhone
Apple does not allow installing apps outside the App Store, and building for it needs a paid Apple developer account.
iPhone users can open the website in Safari, tap **Share > Add to Home Screen**, and open it from the icon. It looks and behaves like an app and gets notifications (iOS 16.4 or newer).
