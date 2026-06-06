# PhishTrack — App Improvement Suggestions

This document outlines key suggestions to improve the user experience, architecture, security, and feature set of the PhishTrack application. These ideas are categorized to help prioritize future development.

---

## 🎨 UI/UX Enhancements (Frontend)

1. **Dark/Light Mode Support**
   - *Current State:* The app is hardcoded with a dark cyber-theme.
   - *Improvement:* Introduce dynamic theming so users can switch between Dark, Light, and System Default themes. Ensure MapLibre styles change dynamically with the theme.

2. **Animations & Micro-interactions**
   - *Improvement:* Add Lottie animations for loading states (e.g., a radar spinning while fetching data). Add subtle slide/fade transitions when navigating between screens using Navigation Compose.

3. **Offline Mode & Caching**
   - *Current State:* The app requires a constant internet connection to view cases and the dashboard.
   - *Improvement:* Implement **Room Database** to cache the latest dashboard data and the case list. Use a `SwipeRefreshLayout` or Compose `pullRefresh` to sync with the server when back online.

4. **Skeleton Loaders**
   - *Improvement:* Replace the basic circular progress indicators with shimmer/skeleton loaders matching the shape of the UI components (like the heatmap or list cards) for a smoother perceived load time.

---

## 🚀 Backend & Architecture Improvements

1. **Pagination & Infinite Scrolling**
   - *Current State:* If the SOC handles thousands of cases, loading all of them at once will crash the app or slow down the API.
   - *Improvement:* Implement cursor-based or offset-based pagination in the `getAnalysisList` endpoint and handle infinite scrolling in Compose using `LazyColumn` state.

2. **WebSocket Real-time Updates**
   - *Current State:* The dashboard and case lists only update when the app fetches the data.
   - *Improvement:* Introduce WebSockets or Server-Sent Events (SSE) so that if a new phishing campaign is detected by the backend, it instantly pops up on the analyst's dashboard without a manual refresh.

3. **Background Sync Worker**
   - *Improvement:* Use Android `WorkManager` to quietly fetch new threat intel in the background every few hours and trigger a local notification if a high-severity threat appears.

---

## 🔒 Security & Privacy Upgrades

1. **Encrypted Shared Preferences / DataStore**
   - *Current State:* Tokens and PINs are stored in regular `SharedPreferences`.
   - *Improvement:* Migrate from `SharedPreferences` to **Jetpack DataStore** or use `EncryptedSharedPreferences` to ensure JWTs and sensitive user data cannot be extracted on rooted devices.

2. **Session Expiry Handling**
   - *Improvement:* The app should gracefully handle 401 Unauthorized errors from the backend by automatically clearing the token and redirecting the user to the Login Screen with a "Session Expired" toast message.

3. **Root Detection & App Shielding**
   - *Improvement:* Since this is a cybersecurity tool handling forensic data, implement root detection (e.g., using Google Play Integrity API) to prevent the app from running on compromised devices.

---

## 💡 New Feature Ideas

1. **PDF Report Export**
   - Allow analysts to click a "Download Report" button on a specific case, which generates a professional PDF containing the threat score, chain of custody, and registrar details, which they can email or share.

2. **Bulk Actions**
   - Allow analysts to long-press on items in the Cases List to select multiple URLs and update their status (e.g., mark 5 URLs as "False Positive" at once).

3. **Scanner Input Integration**
   - Let analysts scan QR codes containing suspicious links directly from the app using the device camera, rather than having to type or paste the URL manually.
