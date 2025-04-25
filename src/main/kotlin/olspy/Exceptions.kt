package olspy

import io.ktor.client.statement.*
import io.ktor.http.*

sealed class OverleafException : Exception
{
	constructor() : super()
	constructor(message: String) : super(message)
	constructor(message: String, cause: Throwable) : super(message, cause)
}

class BinaryFormatException(msg : String) : OverleafException(msg)

class HttpStatusException(val status : HttpStatusCode, msg : String)
	: OverleafException("$msg: ${status.value} (${status.description})")

class HttpContentException(msg : String) : OverleafException(msg)

fun HttpResponse.throwUnlessSuccess(msg : String = "Received invalid status code") : HttpResponse
{
	if(! status.isSuccess())
		throw HttpStatusException(status, msg)

	return this
}