package olspy

import io.ktor.client.statement.*
import io.ktor.http.*

/** Superclass for olspy-specific exceptions */
sealed class OverleafException : Exception
{
	constructor() : super()
	constructor(message: String) : super(message)
	constructor(message: String, cause: Throwable) : super(message, cause)
}

/** Thrown when a websocket packet is malformed */
class BinaryFormatException(msg : String) : OverleafException(msg)

/** Thrown when an overleaf HTTP endpoint returns a non-success status code */
class HttpStatusException(val status : HttpStatusCode, msg : String)
	: OverleafException("$msg: ${status.value} (${status.description})")

/** Thrown when an overleaf HTTP endpoint returns malformed content */
class HttpContentException(msg : String) : OverleafException(msg)

/** @throws HttpStatusException Unless this response is successful */
fun HttpResponse.throwUnlessSuccess(msg : String = "Received invalid status code") : HttpResponse
{
	if(! status.isSuccess())
		throw HttpStatusException(status, msg)

	return this
}