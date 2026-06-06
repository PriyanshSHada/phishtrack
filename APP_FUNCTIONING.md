# PhishTrack — Application Functioning & Capabilities

PhishTrack is a comprehensive **Phishing Link Investigation & Forensic Reporting Tool** designed for SOC (Security Operations Center) Analysts and cybersecurity professionals. 

It consists of a modern **Android Application** (frontend) built with Jetpack Compose and a robust **Node.js/PostgreSQL Backend** to manage data, authentication, and analysis.

---

## 📱 What Can You Do With PhishTrack?

The application empowers analysts to securely log in, analyze suspicious URLs, track cyber threat trends over time, and maintain a chain of custody for investigated forensic artifacts.

### 1. Secure Authentication & Access Control
- **Login & Registration:** Securely authenticate using an email and password.
- **Two-Factor Authentication (OTP):** An extra layer of security using a time-based verification code.
- **Biometric & PIN Lock:** Enable Fingerprint/Face ID or a custom PIN code to lock the app, ensuring sensitive forensic data remains secure even if the device is unlocked.

### 2. Interactive Analyst Dashboard
The dashboard acts as the command center for the analyst, providing a high-level overview of their recent activities and global threats.
- **Weekly Scan Heatmap:** A GitHub-style interactive heatmap showing the volume of scans performed over the last 28 days. It highlights your active day streaks and total scans.
- **Global Threat Radar:** A dynamic, interactive map powered by **MapLibre** and **Satellite Imagery**. It displays live nodes and global threat hotspots, allowing you to monitor where phishing infrastructure is currently active.

### 3. Phishing Case Management
- **List All Cases:** View a historical list of all investigated URLs, domain names, and IP addresses.
- **Status Tracking:** URLs are categorized strictly by their threat level:
  - 🔴 `Malicious`
  - 🟢 `Benign`
  - 🟡 `Suspicious`
  - ⚪ `False Positive`
  - 🔵 `Unverified`
- **Filtering & Search:** Easily filter cases by their status or search for specific domains.

### 4. Deep Forensic Reports
Clicking on any specific case opens a detailed forensic investigation view.
- **Threat Scores:** View the calculated risk score of the domain.
- **Registrar & IP Info:** See where the server is hosted and who registered the domain.
- **Chain of Custody:** Track exactly *who* modified the case and *when*, maintaining a legally viable forensic trail for investigations.
- **Update Case Status:** Analysts can manually override and update the threat status (e.g., marking a blocked site as a "False Positive").

---

## 🛠️ How It Works (Technical Architecture)

### Frontend (Android)
- **UI Framework:** Built entirely with **Jetpack Compose** for a modern, declarative, and high-performance user interface.
- **Architecture:** Follows the MVVM (Model-View-ViewModel) pattern, using **Hilt** for Dependency Injection.
- **Networking:** Communicates with the backend using **Retrofit** and **OkHttp**.
- **Mapping:** Integrates **MapLibre GL** for rendering high-resolution, satellite-based threat maps without relying on Google Play Services.

### Backend (Node.js)
- **Server:** Built on **Express.js** to handle REST API requests from the Android app.
- **Database:** Uses **PostgreSQL**, structured and queried via the **Prisma ORM**.
- **Security:** Implements JWT (JSON Web Tokens) for session management and strict database-level Enum constraints to ensure data integrity.

---

## 🚀 Future Capabilities (Roadmap)
While the app is currently a powerful tracking tool, future expansions could include:
1. **Automated URL Sandboxing:** Automatically opening the submitted URL in a headless browser to capture a screenshot of the phishing site.
2. **VirusTotal / URLScan Integration:** Fetching live API data from external threat intelligence sources when a new link is submitted.
3. **Push Notifications:** Alerting analysts when a high-priority, targeted phishing campaign is detected against their organization.
