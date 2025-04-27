package olspy.protocol

import io.ktor.http.Url
import io.ktor.util.decodeString
import kotlinx.datetime.Instant
import olspy.BinaryFormatException
import olspy.support.*
import java.nio.ByteBuffer
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import olspy.HttpContentException
import java.io.ByteArrayInputStream
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

val RPC_JOIN_PROJECT = "joinProjectResponse"
val RPC_JOIN_DOCUMENT = "joinDoc"
val RPC_LEAVE_DOCUMENT = "leaveDoc"

enum class Opcode
{
	DISCONNECT,
	CONNECT,
	HEARTBEAT,
	MESSAGE,
	JSON,
	EVENT,
	ACK,
	ERROR,
	NOOP
}

/*
	Overleaf's socket.io version (0.9 maybe?) is so outdated I couldn't find any implementations or even documentation on it.
	the packet structure is inferred from here:
	https://github.com/overleaf/socket.io/blob/ddbee7b2d0427d4e4954cf9761abc8053c290292/lib/parser.js
*/

/** reads a packet from a byte buffer
 * The result aliases into `data`
 */
fun readPacket(data : ByteBuffer) : Packet
{
	data.run {
		val opc = Opcode.entries[getAscii('0' .. '8') - '0']
		getAscii(':')

		var id = getDecimal()
		var awd = getAsciiIf { it == '+' } !== null

		getAscii(':')

		val ep = span { it != ':' }.decodeString(Charsets.UTF_8)

		getAsciiIf { it == ':' }

		if(opc == Opcode.ACK)
		{
			if(id !== null || awd)
				throw BinaryFormatException("ACK packet with message ID")

			id = getDecimal() ?: throw BinaryFormatException("ACK packet without referenced ID")
			awd = getAsciiIf { it == '+' } !== null
		}

		return Packet(opc, id, awd, ep, data.decodeString(Charsets.UTF_8))
	}
}

@Serializable
data class ErrorPayload(val reason : String, val advice : String)

@Serializable
data class EventPayload(val name : String, val args : JsonArray)

data class Packet(
	val opcode: Opcode,
	val id : Int?,
	val ackWithData : Boolean,
	val endpoint : String,
	val payload : String,
) {

	val shouldAcknowledge : Boolean
		get() = id !== null && id > 0

	@OptIn(ExperimentalSerializationApi::class)
	inline fun<reified T> jsonPayload() : T
		= Json.decodeFromString(payload)

	val eventPayload : EventPayload
		get() = jsonPayload()

	val errorPayload : ErrorPayload
		get() = payload.split('+', limit = 2).let { (x,y) ->
			ErrorPayload(x,y)
		}
}


@Serializable
data class UserInfo(
	@SerialName("_id")
	val id : String,
	@SerialName("first_name")
	val firstName : String,
	val email : String,
	val privileges : String,
	val signUpDate : Instant,
	@SerialName("last_name")
	val lastName : String = "",
)

/** Misc. feature of a project, largely undocumented */
@Serializable
data class ProjectFeatures(
	/** Observed values: -1 */
	val collaborators : Int,
	val versioning : Boolean,
	val dropbox : Boolean,
	val gitBridge : Boolean,
	val github : Boolean,
	/** Maximum number of seconds for a compile run */
	val compileTimeout : Int,
	/** Observed values: standard */
	val compileGroup : String,
	val templates : Boolean,
	val references : Boolean,
	val trackChanges : Boolean,
	val referencesSearch : Boolean,
	val mendeley : Boolean,
	val trackChangesVisible : Boolean,
	val symbolPalette : Boolean,
)

@Serializable
data class PlainID(
	@SerialName("_id")
	val id : String
)

@Serializable
data class ProjectInfo(
	@SerialName("_id")
	val id : String,
	val name : String,
	@SerialName("rootDoc_id")
	val rootDocID : String,
	val rootFolder : List<FolderInfo>,
	@SerialName("publicAccesLevel") // sic
	val publicAccessLevel : String,
	val dropboxEnabled : Boolean,
	val compiler : String,
	val description : String,
	val spellCheckLanguage : String,
	val deletedByExternalDataSource : Boolean,
	val deletedDocs : List<DeletedFile>,
	val members : List<UserInfo>, // FIXME: aka user_info
	/** texlive image used by the server. Observed values: "texlive-full:2020.1" */
	val imageName : String,
	val invites : List<JsonElement>,
	val owner : PlainID,
	val features : ProjectFeatures,
	val trackChangesState : Boolean,
)

/** Received when a project is joined */
@Serializable
data class JoinProjectArgs(
	val publicId : String,
	val project : ProjectInfo,
	val permissionsLevel : String,
	val protocolVersion : Int
)

/** Undoes the content encoding overleaf performs
 * They do (in JS) `unescape(encodeUriComponent(x))`, so this corresponds to `decodeUriComponent(escape(x))`
 */
fun unMangle(mangled : String) : String
	= StringBuilder().apply {
		for(c in mangled)
		{
			when(c)
			{
				else if c.isLetterOrDigit() -> append(c)
				'@', '*', '_', '+', '-', '.', '/' -> append(c)

				else -> {
					append('%');

					if(c.code < 256)
						append("%02X".format(c.code))
					else
					{
						append('u')
						append("%04X".format(c.code))
					}
				}
			}

		}
	}.toString().let { URLDecoder.decode(it, Charsets.UTF_8) }
