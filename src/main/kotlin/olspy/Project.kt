package olspy

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.ProxyConfig
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
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

/** HTTP header set on all requests after the initial */
private val CSRF_HEADER = "X-CSRF-TOKEN"

/** Accesses the cookie that contains the overleaf session ID */
private val List<Cookie>.sessionToken : String?
	get() = (get("overleaf.sid") ?: get("sharelatex.sid"))?.value

/** Checks that a URL is normal HTTP(S) */
private val Url.isValidScheme
	get() = protocol == URLProtocol.HTTP || protocol == URLProtocol.HTTPS

/** initializes the HTTP client for communicating with overleaf */
private fun initClient(baseUri : Url, conf : ProjectConfig)
	= HttpClient(CIO) {
		engine {
			proxy = conf.proxy
		}

		install(HttpCookies)
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
			pingInterval = conf.timeout
		}
		install(ContentNegotiation) {
			json()
		}

		defaultRequest {
			url {
				takeFrom(baseUri)
			}
		}

		expectSuccess = true
	}

/** HTTP configuration underlying the connection to Overleaf */
class ProjectConfig(val timeout : Duration? = 15.seconds, val proxy : ProxyConfig? = null, val debug : Boolean = false)

/** Support class for encoding requests to share link grant pages */
@Suppress("PropertyName")
@Serializable
private data class GrantData(val _csrf : String, val confirmedByUser : Boolean = false)

/** An overleaf project */
class Project private constructor(val id : String, private val client : HttpClient)
{
	companion object {
		/** Opens an overleaf project via a share link
		 * @param shareLink The link to open
		 * @param conf The HTTP configuration to use
		 * @throws IllegalArgumentException If the link is malformed
		 * @throws HttpContentException When the server's responses weren't formatted as expected
		 * */
		suspend fun open(shareLink : Url, conf : ProjectConfig = ProjectConfig()) : Project
		{
			require(shareLink.isValidScheme) {
				"Illegal URL protocol: ${shareLink.protocol}"
			}
			require(shareLink.parameters.isEmpty()) {
				"Share link must not have query or fragment"
			}
			require(shareLink.isAbsolutePath) {
				"Share link must be absolute"
			}

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

			val loginResp = client.get(shareLink)

			if(client.cookies(shareLink).sessionToken === null)
				throw HttpContentException("Did not receive a session cookie from share link")
			val csrf = CSRF_TAG.find(loginResp.bodyAsText())?.groupValues?.get(1)
					?: throw HttpContentException("Did not receive a CSRF tag from the share link")

			// FIXME: I'm pretty sure this gets completely ignored
			client.config {
				headers {
					append(CSRF_HEADER, csrf)
				}
			}

			val grantUrl = URLBuilder(shareLink).apply {
				pathSegments = pathSegments.plus("grant")
			}.build()
			val grantResp : Map<String,String> = client.post(grantUrl) {
				contentType(ContentType.Application.Json)
				setBody(GrantData(csrf))
			}.body()

			val redir = grantResp["redirect"] ?: throw HttpContentException("Join grant did not contain a project redirect URL")
			val vv = Regex("/project/([0-9a-fA-F]+)$").find(redir)

			if(vv === null)
				throw HttpContentException("Project redirect URL from join grant was not in the expected format")

			return Project(vv.groupValues[1], client)
		}
	}
}