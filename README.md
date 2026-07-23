# NepSecure Portfolio Tracker

NepSecure Portfolio Tracker is an advanced Android application for tracking your share portfolio and market index data directly synced with your Google Sheets.

---

## 📊 Public Master Data Sheet
The centralized public Master Data Sheet provides live NEPSE stock prices (`stocks` tab) and market holiday schedules (`market holiday` tab) for all users:

*   **Public Master Sheet ID:** `1dNE3DhX2d0DGcP8GFpe2bSwHAXZvOt7l4C6QSPVffbc`
*   **Public Master Sheet Link:** [NepSecure Master Data Sheet](https://docs.google.com/spreadsheets/d/1dNE3DhX2d0DGcP8GFpe2bSwHAXZvOt7l4C6QSPVffbc/edit)

---

## 📲 Installation Guide

Follow these steps to install the application on your Android device:

1.  **Download the APK:**
    Download the compiled APK directly from the project root:
    👉 **[Download NepSecure Portfolio Tracker.apk](./NepSecure%20Portfolio%20Tracker.apk)**

2.  **Enable Unknown Sources:**
    *   On your Android device, go to **Settings** > **Security** (or **Apps & Notifications** > **Special App Access**).
    *   Enable **Install Unknown Apps** or **Allow from this source** for your browser or file manager.

3.  **Install the APK:**
    *   Open your device's **Downloads** folder or file manager.
    *   Tap on `NepSecure Portfolio Tracker.apk` and follow the prompts to complete the installation.

4.  **Set Up Spreadsheet Sync:**
    *   Click **[Make a Copy of Portfolio Template Sheet](https://docs.google.com/spreadsheets/d/1dNE3DhX2d0DGcP8GFpe2bSwHAXZvOt7l4C6QSPVffbc/copy)** to create your personal portfolio sheet in your Google Drive with 1 click.
    *   Open the NepSecure app and navigate to the **Settings** tab.
    *   Input your Google Spreadsheet ID (found in the URL of your new portfolio sheet).
    *   Click **Fetch & Refresh Prices** to perform the initial sync.

---

## 💻 Run and Deploy Locally

To run the project locally in development mode:

**Prerequisites:** [Android Studio](https://developer.android.com/studio)

1. Open Android Studio.
2. Select **Open** and choose this project directory.
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` (see `.env.example` for details).
5. Run the app on an emulator or physical device.
