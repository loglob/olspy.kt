package olspy

sealed class OverleafException : Exception
{
	constructor() : super()
	constructor(message: String) : super(message)
	constructor(message: String, cause: Throwable) : super(message, cause)
}

class HttpContentException(msg : String) : OverleafException(msg)