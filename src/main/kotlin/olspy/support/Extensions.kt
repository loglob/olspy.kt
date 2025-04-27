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

private fun Iterable<Byte>.fmt(truncated : Boolean = false) : String = StringBuilder().run {
	var n = 0
	for(c in this@fmt)
	{
		append("%02x".format(c))

		if(++n % 4 == 0)
			append(' ')
	}

	if(truncated)
		append("…")

	append(" (")

	for(c in this@fmt)
		append(if(c in 32..126) c else '·')


	if(truncated)
		append("…")
	append(")")
}.toString()

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

private fun ord(x : Byte) = (x.toInt() and 0xFF).toChar()

fun ByteBuffer.peek() = if(position() < capacity()) get(position()) else null

fun ByteBuffer.getAscii() = ord(get())
fun ByteBuffer.peekAscii() = peek()?.let(::ord)
fun ByteBuffer.getAsciiIf(pred : (x : Char) -> Boolean) : Char?
{
	val p = peekAscii()

	return if(p !== null && pred(p)) {
		getAscii()
		p
	}
	else
		null
}

fun ByteBuffer.getAscii(r : CharRange)
	= getAscii().also { if(it !in r) throw BinaryFormatException("Invalid byte format") }
fun ByteBuffer.getAscii(pred : (x : Char) -> Boolean)
	= getAscii().also { if(!pred(it)) throw BinaryFormatException("Invalid byte format") }
fun ByteBuffer.getAscii(ch : Char)
	= getAscii(ch .. ch)

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

fun<R> ByteBuffer.delta(body : ByteBuffer.() -> R) : Pair<Boolean, R>
{
	val p0 = position()
	val r = body()
	return Pair(position() != p0, r)
}

fun ByteBuffer.toStream() : InputStream
	= ByteArrayInputStream(array(), position(), remaining())
