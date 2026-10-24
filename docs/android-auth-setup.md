# Android authentication setup (Firebase)

This repository originally contained only a README, LICENSE, and .gitignore; it did not contain an Android app or Gradle project. This branch adds a safe integration guide and a reusable Kotlin Firebase Authentication helper. It does **not** create a Firebase project, enable providers, supply OAuth credentials, or make a deployable APK by itself.

## Authentication design

- Firebase Authentication manages passwords and identity-provider tokens; the app must never store raw passwords.
- Email/password supports account creation, sign-in, password-reset email, and email verification.
- Google sign-in should use Android Credential Manager to obtain a Google ID token, then exchange that token for a Firebase credential via `AuthRepository.signInWithGoogleIdToken()`.
- Use Firebase's authenticated user ID (UID) for user-owned data. Enforce authorization in database/storage rules or a trusted backend; hiding a button in the UI is not authorization.
- Never commit `google-services.json`, signing keystores, service-account JSON, OAuth client secrets, access tokens, or passwords. The Android Firebase config contains app/project identifiers and is not a substitute for server-side secrets, but this repository ignores it to avoid accidental configuration disclosure. Add the real file locally through Firebase's official setup flow.

## 1. Create and configure Firebase

1. In the Firebase console, create or select a project you control.
2. Add the Android app using the **actual application ID/package name** from your Android project. This repository currently does not specify one; do not guess it.
3. Download the updated `google-services.json` and place it at the Android app module root (normally `app/google-services.json`). Keep it out of Git.
4. In Firebase Console → Authentication → Sign-in method, enable **Email/Password** and **Google**.
5. Add your app's SHA-1 and SHA-256 certificate fingerprints in Firebase project settings. Use the fingerprints for the signing certificate used by the build you test.
6. Confirm the Google provider has the appropriate project OAuth configuration. Google sign-in uses the **Web application OAuth client ID** as the server client ID for Credential Manager, exposed by the Firebase config as `default_web_client_id`.
7. In Authentication settings, keep email-enumeration protection enabled and set an appropriate password policy. Configure email templates and authorized domains as appropriate.

Official references:
- Email/password: https://firebase.google.com/docs/auth/android/password-auth
- Google sign-in: https://firebase.google.com/docs/auth/android/google-signin
- Credential Manager: https://developer.android.com/identity/sign-in/credential-manager-siwg

## 2. Add Gradle configuration

Use the Google Services Gradle plugin as described in Firebase's current Android setup guide, then add these dependencies to the app module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-auth")

    // Modern Google sign-in with Android Credential Manager
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
}
```

Versions may move forward; check the official Firebase and Android documentation before upgrading. The project currently has no Gradle files, so these entries cannot yet be validated against an existing build.

## 3. Use the helper

The source file `android-auth-sample/AuthRepository.kt` wraps Firebase Authentication operations. Instantiate it from your app and connect callbacks to your UI. Validate email/password inputs in the UI for usability, but treat Firebase as the authority for authentication.

For Google sign-in:
1. Use Credential Manager with `GetGoogleIdOption.Builder().setServerClientId(getString(R.string.default_web_client_id))`.
2. Extract the returned Google ID token using the Credential Manager / Google ID credential flow from the official guide.
3. Pass that ID token to `signInWithGoogleIdToken(idToken)`.
4. Handle cancellation and errors without logging credentials or ID tokens.

## 4. Before release

- Test sign-up, sign-in, wrong password, reset email, verification, Google cancellation, sign-out, and app restart/session persistence.
- Ensure private media, admin actions, purchases, and account data are authorized by server-side rules/backend checks, not just by client UI.
- Do not log passwords, Google ID tokens, Firebase ID tokens, or authorization headers.
- Review Firebase Authentication quotas, email templates, provider configuration, privacy disclosures, and account deletion requirements.
- Run a real Android build and test on a device/emulator after the missing app project files are added. No build or live Firebase sign-in has been performed by this repository-only change.
