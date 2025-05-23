package olspy.support

import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Serializer that formats an Instant as number of milliseconds since UNIX epoch */
class EpochMillisSerializer(): KSerializer<Instant>
{
	override val descriptor : SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.LONG)

	override fun serialize(encoder : Encoder, value : Instant)
		= encoder.encodeLong(value.toEpochMilliseconds())

	override fun deserialize(decoder : Decoder) : Instant
		= Instant.fromEpochMilliseconds(decoder.decodeLong())
}