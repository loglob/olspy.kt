package olspy

import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.select
import kotlinx.serialization.json.*
import olspy.protocol.*
import olspy.protocol.Opcode.*
import olspy.support.WriteOnce
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.coroutineContext

/** A websocket session for an overleaf project */
class ProjectSession private constructor(val project: Project)
{
	/** async thread that moves data between `receiveMap`, `sendQueue` and the socket */
	private lateinit var socketThread : Job

	/** Structure used to pass return values between the receiver thread and callers */
	private val receiveMap = ConcurrentHashMap<Int, WriteOnce<JsonArray>>()
	/** Queue of websocket packets to send. Drops packets to avoid blocking, else the socket thread could lock up  */
	private val sendQueue = Channel<Frame>(64, BufferOverflow.DROP_OLDEST)
	/** The arguments received on project join */
	private val joinArgs = WriteOnce<JoinProjectArgs>()
	/** Number of sent packets */
	private val packetNumber = AtomicInteger(0)

	private suspend fun socketLoop(incoming : ReceiveChannel<Frame>, outgoing : SendChannel<Frame>)
	{
		var stop = false
		while(! stop)
		{
			select {
				sendQueue.onReceiveCatching { result ->
					when
					{
						result.isClosed -> stop = true
						result.isFailure -> TODO()
						result.isSuccess -> outgoing.send(result.getOrThrow())
					}
				}
				incoming.onReceiveCatching { result ->
					when
					{
						result.isClosed -> stop = true
						result.isFailure -> TODO()
						result.isSuccess ->
						{
							val frame = result.getOrThrow()
							val pkt = readPacket(frame.buffer)

							when(pkt.opcode)
							{
								CONNECT -> {}

								HEARTBEAT ->
								{
									sendQueue.send(frame)
								}

								EVENT ->
								{
									val pl = pkt.eventPayload

									// these events are sent when other users edit the same documents
									if(pl.name.startsWith("clientTracking."))
										return@onReceiveCatching
									if(pl.name != RPC_JOIN_PROJECT)
										throw HttpContentException("Unexpected server-side EVENT '${pl.name}'")

									joinArgs.set(Json.decodeFromJsonElement<JoinProjectArgs>(pl.args[0]))
								}

								ACK ->
								{
									val answers = pkt.id!!
									val data = pkt.jsonPayload<JsonArray>()
									// FIXME: we should log or something on dropped response packets
									receiveMap.remove(answers)?.set(data)
								}

								DISCONNECT -> stop = true

								else -> throw HttpContentException("Unexpected opcode: ${pkt.opcode}")
							}
						}
					}
				}
			}
		}
	}

	private suspend fun sendRPC(kind : String, args : List<JsonElement>) : JsonArray
	{
		val n = packetNumber.getAndIncrement()
		val payload = EventPayload(kind, JsonArray(args))

		val result = WriteOnce<JsonArray>()
		if(receiveMap.putIfAbsent(n, result) !== null)
			throw IllegalStateException("Duplicate packet number")

		val data = "${'0' + EVENT.ordinal}:$n+::${Json.encodeToString(payload)}".toByteArray(Charsets.UTF_8)
		sendQueue.send(Frame.Text(true, data))

		return withTimeout(3_000) {
			 result.get()
		}
	}

	/** Closes the session and waits for its socket thread to terminate */
	suspend fun close()
	{
		sendQueue.close()
		socketThread.join()
	}

	/** Retrieves misc. project information */
	suspend fun getProjectInfo()
		= joinArgs.get()

	/** Retrieves a document via its ID
	 * @return the document's lines
	 * */
	suspend fun getDocument(id : String) : List<String>
	{
		// tuple of error message and result
		val data = sendRPC(RPC_JOIN_DOCUMENT, listOf(
			JsonPrimitive(id),
			JsonObject(mapOf( "encodeRanges" to JsonPrimitive(true) ))
		))

		if(data[0] !is JsonNull)
			throw Exception("Failed document ID lookup: ${data[0]}")

		sendRPC(RPC_LEAVE_DOCUMENT, listOf( JsonPrimitive(id) ))

		return data[1].jsonArray.map { unMangle(it.jsonPrimitive.content) }
	}

	companion object
	{
		/** Opens a websocket and starts a managing thread */
		internal suspend fun start(project: Project, key : String) : ProjectSession
		{
			val session = ProjectSession(project)

			session.socketThread = CoroutineScope(coroutineContext).launch {
				project.client.webSocket("socket.io/1/websocket/${key}?projectId=${project.id}") {
					try
					{
						session.socketLoop(incoming, outgoing)
					}
					finally
					{
						session.sendQueue.close()
					}
				}
			}

			return session
		}
	}
}