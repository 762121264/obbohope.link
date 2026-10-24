import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.tasks.Task

/**
 * Small Firebase Authentication wrapper for an Android app.
 *
 * This class intentionally contains no UI and no secrets. Use Credential Manager
 * in the Activity/Compose UI to obtain a Google ID token, then pass it here.
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun createAccount(
        email: String,
        password: String,
        onResult: (Result<FirebaseUser>) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task -> deliverUser(task, onResult) }
    }

    fun signInWithEmail(
        email: String,
        password: String,
        onResult: (Result<FirebaseUser>) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task -> deliverUser(task, onResult) }
    }

    /** Call with a Google ID token returned by Android Credential Manager. */
    fun signInWithGoogleIdToken(
        idToken: String,
        onResult: (Result<FirebaseUser>) -> Unit
    ) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task -> deliverUser(task, onResult) }
    }

    fun sendPasswordReset(
        email: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        auth.sendPasswordResetEmail(email.trim()).addOnCompleteListener { task ->
            if (task.isSuccessful) onResult(Result.success(Unit))
            else onResult(Result.failure(task.exception ?: IllegalStateException("Password reset failed")))
        }
    }

    fun sendEmailVerification(
        onResult: (Result<Unit>) -> Unit
    ) {
        val user = auth.currentUser
        if (user == null) {
            onResult(Result.failure(IllegalStateException("No signed-in user")))
            return
        }

        user.sendEmailVerification().addOnCompleteListener { task ->
            if (task.isSuccessful) onResult(Result.success(Unit))
            else onResult(Result.failure(task.exception ?: IllegalStateException("Verification email failed")))
        }
    }

    fun signOut() {
        auth.signOut()
    }

    private fun deliverUser(
        task: Task<AuthResult>,
        onResult: (Result<FirebaseUser>) -> Unit
    ) {
        if (!task.isSuccessful) {
            onResult(Result.failure(task.exception ?: IllegalStateException("Authentication failed")))
            return
        }

        val user = auth.currentUser
        if (user != null) onResult(Result.success(user))
        else onResult(Result.failure(IllegalStateException("Authentication succeeded but no user was returned")))
    }
}
