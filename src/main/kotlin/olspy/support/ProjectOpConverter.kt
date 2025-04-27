package olspy.support

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import olspy.HttpContentException
import olspy.protocol.http.AddFile
import olspy.protocol.http.ProjectOp
import olspy.protocol.http.RemoveFile
import olspy.protocol.http.RenameFile

@Serializable
private data class Arg(
	val pathname : String
)
@Serializable
private data class RenameArg(
	val pathname : String,
	val newPathname : String
)
@Serializable
private data class Surrogate(
	val atV : Int,
	val add : Arg? = null,
	val remove : Arg? = null,
	val rename : RenameArg? = null
)

class ProjectOpConverter() : KSerializer<ProjectOp>
{
	override val descriptor : SerialDescriptor = SerialDescriptor("olspy.support.ProjectOp", Surrogate.serializer().descriptor)

	override fun serialize(encoder : Encoder, value : ProjectOp)
	{
		val surrogate = value.run {
			when(this) {
				is AddFile -> Surrogate(atV, add = Arg(path))
				is RemoveFile -> Surrogate(atV, remove = Arg(path))
				is RenameFile -> Surrogate(atV, rename = RenameArg(path, newPath))
			}
		}
		encoder.encodeSerializableValue(Surrogate.serializer(), surrogate)
	}

	override fun deserialize(decoder : Decoder) : ProjectOp
	 = decoder.decodeSerializableValue(Surrogate.serializer()).run {
		when {
			add !== null && remove === null && rename === null -> AddFile(atV, add.pathname)
			add === null && remove !== null && rename === null -> RemoveFile(atV, remove.pathname)
			add === null && remove === null && rename !== null -> RenameFile(atV, rename.pathname, rename.newPathname )
			else -> throw HttpContentException("Malformed project op, must encode exactly one action")
		}
	}
}