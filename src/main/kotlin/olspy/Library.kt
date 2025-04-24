package olspy

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.ProxyConfig
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking

class Library {
    fun someLibraryMethod(): Boolean {
        return true
    }

    fun httpRequestTemplate(pr : ProxyConfig? = null) : String
        = runBlocking {
        HttpClient(CIO) {
            engine {
                proxy = pr
            }
        }.run {
            get("http://httpbin.org/get?foo=bar").bodyAsText(Charsets.UTF_8)
        }
    }
}
