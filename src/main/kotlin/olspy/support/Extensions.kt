package olspy.support

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Url
import io.ktor.http.contentType
import olspy.BinaryFormatException
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.ByteBuffer

/** Sends JSON formatted data to an HTTP endpoint */
suspend fun HttpClient.postJson(url : Url, data : Any) : HttpResponse
		= post(url) {
	contentType(ContentType.Application.Json)
	setBody(data)
}
suspend fun HttpClient.postJson(path : String, data: Any) : HttpResponse
// NOTE: postJson(Url(path)) has COMPLETELY different semantics
		= post(path) {
	contentType(ContentType.Application.Json)
	setBody(data)
}

/** converts byte to char */
private fun ord(x : Byte) = (x.toInt() and 0xFF).toChar()

/** Inspects the next byte to be returned by get() without updating position */
fun ByteBuffer.peek() = if(position() < capacity()) get(position()) else null

/** Gets the next byte and converts it to ASCII char */
fun ByteBuffer.getAscii() = ord(get())
/** @returns the next value to be returned by `getAscii()` without updating state */
fun ByteBuffer.peekAscii() = peek()?.let(::ord)
/** Looks at the next byte and consumes it iff the `pred` is true
 * @returns The consumed character, if the predicate matched
 */
inline fun ByteBuffer.getAsciiIf(crossinline predicate : (x : Char) -> Boolean) : Char?
{
	val p = peekAscii()

	return if(p !== null && predicate(p)) {
		getAscii()
		p
	}
	else
		null
}

/** Looks at the next byte, consumes it, and asserts that it is inside a range
 * @throws BinaryFormatException If the byte isn't in the range
 */
fun ByteBuffer.getAscii(r : CharRange)
	= getAscii().also { if(it !in r) throw BinaryFormatException("Invalid byte format") }

/** Looks at the next byte, consumes it, and asserts that it is equal to the given char
 * @throws BinaryFormatException If the byte has another value
 */
fun ByteBuffer.getAscii(ch : Char)
	= getAscii(ch .. ch)

/** Reads a positive decimal formatted number from the buffer
 * @returns null if no digits were present
 * */
fun ByteBuffer.getDecimal() : Int?
{
	var x = 0
	var any = false

	while(true)
	{
		val next = getAsciiIf { it in '0'..'9' }

		if(next === null)
			break

		x = 10*x + (next - '0')
		any = true
	}

	return if(any) x else null
}

/** Creates an aliasing sub-buffer of the longest prefix under the given predicate */
fun ByteBuffer.span(pred : (x : Char) -> Boolean) : ByteBuffer
{
	val p0 = position()

	while(true)
	{
		if(getAsciiIf(pred) === null)
			break
	}

	return ByteBuffer.wrap(array(), p0, position() - p0)
}
