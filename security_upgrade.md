# Security Upgrade Summary (Encrypted SharedPreferences)

## What changed

We upgraded local storage for login/session data from plaintext `SharedPreferences` to
`EncryptedSharedPreferences` with a safe migration that **does not log users out**.

Key goals:
- Protect sensitive data at rest (device storage) using Android Keystore–backed encryption.
- Preserve existing behavior and avoid breaking login flows.
- Handle keystore failures safely (fallback to logged-out state, no crash).

### Files touched
- `app/build.gradle`
- `app/src/main/java/com/ft/ftchinese/store/SessionManager.kt`
- `app/src/main/java/com/ft/ftchinese/store/TokenManager.kt`
- `app/src/main/java/com/ft/ftchinese/store/SecurePrefs.kt` (new)
- `app/src/main/java/com/ft/ftchinese/store/PrefsMigration.kt` (new)

### Dependency added
- `androidx.security:security-crypto:1.0.0` (stable)

---

## Design decisions (why this approach)

1. **Different filenames for encrypted prefs**
   - Old plaintext files must **not** be opened by `EncryptedSharedPreferences`.
   - Used new names to avoid file-format collision:
     - `account` → `account_secure`
     - `device_token` → `device_token_secure`

2. **Sentinel-key migration**
   - We check for a sentinel key in the encrypted prefs before migrating.
   - This prevents "zombie" empty encrypted files from causing false positives.
   - Sentinel for session: `PREF_IS_LOGGED_IN` (exists even when logged out).

3. **Safe migration order**
   - Read old prefs → write encrypted prefs → `commit()` → clear old prefs.
   - Old data is only cleared if encrypted write succeeded.

4. **Keystore failure handling**
   - If encrypted prefs cannot be created, we return `null`.
   - In `SessionManager`, this triggers a safe logout behavior (no crash).

5. **Lazy initialization**
   - Encrypted prefs are created lazily to reduce startup cost.

---

## Implementation details

### 1) Encrypted prefs helper
**`SecurePrefs.kt`** provides `EncryptedSharedPreferences` with a Keystore `MasterKey`:

```kotlin
object SecurePrefs {
    fun encrypted(context: Context, name: String): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            name,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
```

### 2) Migration helper
**`PrefsMigration.kt`** migrates plaintext prefs into encrypted prefs safely, using a sentinel key:

```kotlin
object PrefsMigration {
    fun migrateIfNeeded(
        context: Context,
        oldFileName: String,
        newFileName: String,
        sentinelKey: String,
        initSentinel: ((SharedPreferences.Editor) -> Unit)? = null
    ): SharedPreferences? {
        val newPrefs = try {
            SecurePrefs.encrypted(context, newFileName)
        } catch (e: Exception) {
            return null
        }

        if (newPrefs.contains(sentinelKey)) {
            return newPrefs
        }

        val oldPrefs = context.getSharedPreferences(oldFileName, Context.MODE_PRIVATE)
        if (oldPrefs.all.isNotEmpty()) {
            val editor = newPrefs.edit()
            copyAll(oldPrefs, editor)
            val success = editor.commit()
            if (success) {
                oldPrefs.edit().clear().commit()
            }
        } else if (initSentinel != null) {
            val editor = newPrefs.edit()
            initSentinel(editor)
            editor.commit()
        }

        return newPrefs
    }
}
```

### 3) SessionManager changes
- Old name: `account`
- New name: `account_secure`
- Sentinel: `PREF_IS_LOGGED_IN`
- If encrypted prefs fail, session is treated as logged-out and old data is cleared once.

### 4) TokenManager changes
- Old name: `device_token`
- New name: `device_token_secure`
- Sentinel: `token`
- If encrypted prefs fail, token is generated in-memory for this run.

---

## QA checklist (how to debug/verify)

### A) Upgrade path (migration)
1. Install old build and log in.
2. Update to new build.
3. Verify:
   - User stays logged in.
   - New encrypted prefs file has data.
   - Old plaintext prefs file is cleared.

### B) Fresh install path
1. Install new build fresh.
2. Log in.
3. Verify encrypted prefs file is created and populated.

### C) Keystore invalidation path
1. Log in, then change device PIN/lock method (invalidates Keystore keys).
2. Open app.
3. Verify:
   - App does **not** crash.
   - User is logged out cleanly.

### D) Manual inspection (debug builds)
- Inspect shared prefs in app data using Android Studio Device File Explorer:
  - `shared_prefs/account.xml` should be empty after migration.
  - `shared_prefs/account_secure.xml` should exist and contain encrypted data.

---

## Known limitations
- This improves **device-at-rest** security only.
- Stolen tokens are still valid on server side; no server changes are made.

---

## Quick reference: pref names
- Session (old): `account`
- Session (new): `account_secure`
- Token (old): `device_token`
- Token (new): `device_token_secure`

---

## Future optional hardening (not implemented)
- Add backup exclusions for old plaintext prefs.
- Use `MasterKey.Builder.setUserAuthenticationRequired(...)` for biometric-gated keys (trade-off: requires user presence).
- Reduce stored fields (store only login identifiers and avoid profile data if not needed).

