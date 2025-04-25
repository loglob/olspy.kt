package olspy

import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import olspy.support.WriteOnce
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue


class ProjectSession private constructor(val project: Project)
{
	private lateinit var socketThread : Job
	private lateinit var sendThread : Job
	private lateinit var receiveThread : Job
	private val receiveMap = ConcurrentHashMap<Int, WriteOnce<Frame>>()
	private val sendQueue = ConcurrentLinkedQueue<Frame>()

	private suspend fun sendLoop(ch : SendChannel<Frame>)
	{

	}

	private suspend fun receiveLoop(ch : ReceiveChannel<Frame>)
	{
		while(true)
		{
			val rec = ch.receiveCatching()

			if(rec.isClosed)
				break

			when(val frame = rec.getOrThrow())
			{
				is Frame.Text -> {}
				is Frame.Binary -> {}
				else -> {}
			}
		}
	}

	companion object
	{
		internal suspend fun start(project: Project, key : String) : ProjectSession
		{
			val sess = ProjectSession(project)

			sess.socketThread = coroutineScope {
				launch {
					project.client.webSocket("socket.io/1/websocket/${key}?projectId=${project.id}") {
						coroutineScope {
							sess.sendThread = launch {
								sess.sendLoop(this@webSocket.outgoing)
							}
							sess.receiveThread = launch {
								sess.receiveLoop(this@webSocket.incoming)
							}
						}
						sess.sendThread.join()
						sess.receiveThread.join()
					}
				}
			}

			return TODO()
		}
	}
}