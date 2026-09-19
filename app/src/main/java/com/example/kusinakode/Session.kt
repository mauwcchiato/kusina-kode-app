// File: app/src/main/java/com/example/kusinakode/Session.kt
package com.example.kusinakode

object Session {
    var userId: Int? = null
    var displayName: String? = null
    var email: String? = null
    var nickname: String? = null

    /**
     * Bearer token from login/register. The API derives identity from this
     * rather than from a user_id in the request, so without it every
     * account-scoped call is rejected.
     */
    var token: String? = null
}
