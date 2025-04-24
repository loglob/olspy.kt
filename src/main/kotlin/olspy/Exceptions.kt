package olspy

import io.ktor.client.statement.HttpResponse
import io.ktor.client.utils.EmptyContent.status
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess

sealed class OverleafException : Exception
{
	constructor() : super()
	constructor(message: String) : super(message)
	constructor(message: String, cause: Throwable) : super(message, cause)
}

class HttpStatusException(val status : HttpStatusCode, msg : String)
	: OverleafException("$msg: ${status.value} (${status.description})")

class HttpContentException(msg : String) : OverleafException(msg)

fun HttpResponse.throwUnlessSuccess(msg : String = "Received invalid status code") : HttpResponse
{
	if(! status.isSuccess())
		throw HttpStatusException(status, msg)

	return this
}