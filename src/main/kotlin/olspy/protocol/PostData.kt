@file:Suppress("PropertyName")

package olspy.protocol

import kotlinx.serialization.Serializable

/** Sent on grant endpoints for share links */
@Serializable
data class GrantData(val _csrf : String, val confirmedByUser : Boolean = false)

/** Sent to login endpoints */
@Serializable
data class LoginData(val _csrf : String, val email : String, val password : String)

@Serializable
data class CompileData(val rootDoc_id : String?, val draft : Boolean, val check : String,
                       val incrementalCompilesEnabled : Boolean, val stopOnFirstError : Boolean)