package olspy.protocol

import olspy.BinaryFormatException
import olspy.support.*
import java.nio.ByteBuffer

enum class Opcode
{
	DISCONNECT,
	CONNECT,
	HEARTBEAT,
	MESSAGE,
	JSON,
	EVENT,
	ACK,
	ERROR,
	NOOP
}

data class Packet(
	val opcode: Opcode,
	val id : UInt?,
	val ackWithData : Boolean,
	val endpoint : ByteBuffer,
	val payload : ByteBuffer,
) {
	companion object {
		/*
			Overleaf's socket.io version (0.9 maybe?) is so outdated I couldn't find any implementations or even documentation on it.
			the packet structure is inferred from here:
			https://github.com/overleaf/socket.io/blob/ddbee7b2d0427d4e4954cf9761abc8053c290292/lib/parser.js
		*/

		fun fromBytes(data : ByteBuffer)
		{
			data.run {
				val opc = Opcode.entries[getAscii('0' .. '8') - '0']
				getAscii(':')

				var (gotId, id) = delta { getDecimal() }
				val awd = getAsciiIf { it == '+' } !== null

				getAscii(':')

				val ep = span { it != ':' }

				getAsciiIf { it == ':' }

				if(opc == Opcode.ACK)
				{
					if(gotId || awd)
						throw BinaryFormatException("ACK packet with message ID")

					id = getDecimal()
				}

				TODO()
			}
		}
	}
}