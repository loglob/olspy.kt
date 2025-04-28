package olspy

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import olspy.protocol.*
import olspy.protocol.CompileInfo
import olspy.protocol.OutputFile
import olspy.protocol.Update
import olspy.protocol.WrappedUpdates
import olspy.support.Pat
import olspy.support.postJson
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Pattern that matches path components of read-only share links */
private val READ_ONLY_LINK : Pat.List<String>
	= Pat.List(Pat.Many(), Pat.Eq("read"), Pat.Regex("^[a-z]{12}$"))

/** Pattern that matches path components of read/write share links */
private val READ_WRITE_LINK : Pat.List<String>
	= Pat.List(Pat.Many(), Pat.Regex("^[0-9]+[a-z]{6,12}$"))

/** Regex that matches the CSRF meta tag inserted on grant pages
 * Its first capture group contains the CSRF token itself
 */
private val CSRF_TAG : Regex
	= Regex("<meta +name=\"ol-csrfToken\" *content=\"([^\"]+)\" *>")

/** Accesses the cookie that contains the overleaf session ID */
private val List<Cookie>.sessionToken : String?
	get() = (get("overleaf.sid") ?: get("sharelatex.sid"))?.value

/** initializes the HTTP client for communicating with overleaf */
private fun initClient(baseUri : Url, conf : ProjectConfig, extra : HttpClientConfig<CIOEngineConfig>.() -> Unit = {}) : HttpClient
{
	// this MUST be declared here because the config lambda gets captured and otherwise all cookies would get deleted by re-configuring
	val cookies = AcceptAllCookiesStorage()
	return HttpClient(CIO) {
		engine {
			proxy = conf.proxy
		}

		install(HttpCookies) {
			storage = cookies
		}
		install(DefaultRequest)

		if(conf.debug)
		{
			install(Logging) {
				logger = Logger.SIMPLE
				level = LogLevel.ALL
			}
		}

		install(HttpTimeout) {
			requestTimeoutMillis = conf.timeout?.inWholeMilliseconds
		}
		install(WebSockets) {
			pingInterval = 3.seconds
		}
		install(ContentNegotiation) {
			json()
		}



		defaultRequest {
			url {
				takeFrom(baseUri)
			}
		}

		expectSuccess = false

		extra()
	}
}

/** HTTP configuration underlying the connection to Overleaf */
class ProjectConfig(val timeout : Duration? = 15.seconds, val proxy : ProxyConfig? = null, val debug : Boolean = false)

private suspend fun HttpResponse.getCSRF() : String
	= CSRF_TAG.find(bodyAsText())?.groupValues?.get(1)
		?: throw HttpContentException("Response did not include a CSRF token")


/** Ensures a URL has the correct format for Overleaf links */
private fun checkURL(url : Url)
{
	require(url.protocol == URLProtocol.HTTP || url.protocol == URLProtocol.HTTPS) {
		"illegal protocol: ${url.protocol}"
	}
	require(url.isAbsolutePath) { "Overleaf URL must be absolute" }
	require(url.parameters.isEmpty()) { "Overleaf URL must not have query parameters" }
}

/** Creates an updated http client with a CSRF header */
private fun HttpClient.plusCSRF(csrf : String) : HttpClient
	= config {
		defaultRequest {
			header("X-CSRF-TOKEN", csrf)
		}
	}

/** An overleaf project
 * @param id The unique ID of this project
 * */
class Project private constructor(val id : String, internal val client : HttpClient)
{
	companion object {
		/** Opens an overleaf project via a share link
		 * @param shareLink The link to open
		 * @param conf The HTTP configuration to use
		 * @throws IllegalArgumentException If the link is malformed
		 * @throws HttpContentException When the server's responses weren't formatted as expected
		 * @throws HttpStatusException When the server responds with a failed status code
		 * */
		suspend fun open(shareLink : Url, conf : ProjectConfig = ProjectConfig()) : Project
		{
			checkURL(shareLink)

			// number of segments that aren't part of the base URL
			val omit = when(shareLink.rawSegments) {
				in READ_ONLY_LINK -> 2
				in READ_WRITE_LINK -> 1
				else -> throw IllegalArgumentException("Invalid share link format")
			}

			val baseUri = URLBuilder(shareLink).apply {
					pathSegments = pathSegments.dropLast(omit)
				}.build()

			val client = initClient(baseUri, conf)
			val loginResp = client.get(shareLink).throwUnlessSuccess()

			if(client.cookies(shareLink).sessionToken === null)
				throw HttpContentException("Did not receive a session cookie from share link")
			val csrf =  CSRF_TAG.find(loginResp.bodyAsText())?.groupValues?.get(1)
					?: throw HttpContentException("Did not receive a CSRF tag from the share link")

			val grantUrl = URLBuilder(shareLink).apply {
				pathSegments = pathSegments.plus("grant")
			}.build()
			val grantResp : Map<String,String> = client.postJson(grantUrl, GrantData(csrf))
				.throwUnlessSuccess().body()

			val redir = grantResp["redirect"] ?: throw HttpContentException("Join grant did not contain a project redirect URL")
			val id = Regex("/project/([0-9a-fA-F]+)$").find(redir)

			if(id === null)
				throw HttpContentException("Project redirect URL from join grant was not in the expected format")

			return Project(id.groupValues[1], client.plusCSRF(csrf))
		}

		/** Opens a project with a user's login credentials
		 * @param host The base URL of the overleaf server
		 * @param id The project ID
		 * @param email The user's email
		 * @param password The user's password
		 * @param conf The HTTP configuration to use
		 *
		 * @throws IllegalArgumentException If the host URL or project id are malformed
		 * @throws HttpContentException When the server's responses weren't formatted as expected
		 * @throws HttpStatusException When the server responds with a failed status code
		 */
		suspend fun open(host : Url, id : String, email : String, password : String, conf : ProjectConfig = ProjectConfig()) : Project
		{
			checkURL(host)
			require( Regex("^[0-9a-fA-F]+$").matches(id)) { "Invalid project ID" }

			val client = initClient(host, conf)

			val csrf = client.get("login")
				.throwUnlessSuccess("Failed to GET login page (is the host url correct?)")
				.getCSRF()

			client.postJson("login", LoginData(csrf, email, password))
				.throwUnlessSuccess("Failed to log in (are the credentials correct?)")

			if(client.cookies(host).sessionToken === null)
				throw HttpContentException("Did not receive a session cookie after login")

			return Project(id, client.plusCSRF(csrf))
		}
	}

	/** Requests a compilation
	 * @param rootDoc A file ID to use as main document
	 * @param draft Whether to run a draft (faster,lower quality) compile
	 * @param check Overleaf always sets this so "silent" (?)
	 * @param incremental Whether to run an incremental compile
	 * @param stopOnFirstError Whether to stop on error or continue compiling
	 * */
	suspend fun compile(rootDoc : String? = null, draft : Boolean = false, check : String = "silent",
	                    incremental : Boolean = true, stopOnFirstError : Boolean = false) : CompileInfo
	{
		val result = client.postJson("project/$id/compile", CompileData(
			rootDoc, draft, check, incremental, stopOnFirstError
		)).throwUnlessSuccess("Failed to compile project")

		return result.body()
	}

	/** Initializes a websocket instance for this project */
	suspend fun join() : ProjectSession
	{
		val t = Clock.System.now().toEpochMilliseconds()
		val key = client.get("socket.io/1/?projectId=$id&t=$t")
			.throwUnlessSuccess()
			.bodyAsText()
			.split(':')[0]

		return ProjectSession.start(this, key)
	}

	/** Retrieves a file produced by compilation */
	suspend fun getOutFile(file : OutputFile) : HttpResponse
		= client.get("project/$id/build/${file.build}/output/${file.path}").throwUnlessSuccess()

	/** Retrieves the project history */
	suspend fun getUpdateHistory() : List<Update>
		= client.get("project/$id/updates").throwUnlessSuccess().body<WrappedUpdates>().updates

	/** The state during log parsing */
	private enum class LogState
	{
		/** No context */
		INITIAL,
		/** Last line began with '!' */
		GOT_BANG,
		/** Last line was '<inserted text>' or a subsequent indented line */
		INSERTED_TEXT
	}

	suspend inline fun<reified R> withSession(crossinline body : suspend ProjectSession.() -> R) : R
	{
		val s = join()
		val res = s.body()
		s.close()

		return res
	}

	/** Reads compile logs and extracts error messages
	 * @param comp A failed compilation
	 */
	fun readLogs(comp : CompileInfo) : Flow<String> = flow {
		val log = comp.outputFiles.firstOrNull { it.path.endsWith(".log") } ?: return@flow
		val data = getOutFile(log).bodyAsChannel()
		val buf = StringBuilder()
		var state = LogState.INITIAL

		while(true)
		{
			val line = data.readUTF8Line()?.trimEnd()

			if(line === null)
				break

			when(state)
			{
				LogState.INITIAL -> {
					if(line.startsWith('!'))
					{
						buf.clear()
						buf.append(line.slice(1 until line.length))
						state = LogState.GOT_BANG
					}
				}
				LogState.GOT_BANG, LogState.INSERTED_TEXT -> when {
					state == LogState.INSERTED_TEXT && line.isNotEmpty() && line[0].isWhitespace() -> {}
					line ==  "<inserted text>" -> state = LogState.INSERTED_TEXT
					line.isEmpty() -> {
						state = LogState.INITIAL
						emit(buf.toString())
					}
					else -> buf.append('\n', line)
				}
			}
		}

	}
}