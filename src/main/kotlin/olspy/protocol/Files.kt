@file:OptIn(ExperimentalSerializationApi::class)

package olspy.protocol

import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

/** A document, binary file or folder  */
@Serializable
sealed class FileInfo
{
	/** Project-wide unique ID for this file */
	abstract val id : String
	/** Human-readable filename */
	abstract val name : String
}

/** A plain document */
@Serializable
class Document(
	@SerialName("_id")
	override val id : String,
	override val name : String
) : FileInfo()

@Serializable
data class DeletedFile(
	@SerialName("_id")
	override val id : String,
	override val name : String,
	/** Timestamp of deletion (UTC) */
	val deletedAt : Instant
) : FileInfo()

@Serializable
data class FileRef(
	@SerialName("_id")
	override val id : String,
	override val name : String,
	val linkedFileData : LinkedFile?,
	val created : Instant
) : FileInfo()

@Serializable
@JsonClassDiscriminator("provider")
sealed class LinkedFile
{
	abstract val sourceProjectID : String
}

@Serializable
@SerialName("project_file")
data class LinkedProjectFile(
	@SerialName("source_project_id")
	override val sourceProjectID : String,
	@SerialName("source_entity_path")
	val sourceEntityPath : String
) : LinkedFile()

@Serializable
@SerialName("project_output_file")
data class LinkedOutputFile(
	@SerialName("source_project_id")
	override val sourceProjectID : String,
	@SerialName("source_output_file_path")
	val sourceOutputFilePath : String,
	@SerialName("build_id")
	val buildID : String,
) : LinkedFile()

/** A folder */
@Serializable
data class FolderInfo(
	@SerialName("_id")
	override val id: String,
	override val name : String,
	/** All (shallow) contained documents */
	val docs : List<Document>,
	/** All (shallow) contained file referenced */
	val fileRefs : List<FileRef>,
	/** All (shallow) subfolders */
	val folders : List<FolderInfo>,
) : FileInfo()
{
	fun lookup(path : String) : FileInfo?
		= lookup(path.split('/'))

	fun lookup(path : List<String>) : FileInfo?
	{
		if(path.isEmpty() || path.singleOrNull()?.isEmpty() ?: false)
			return this

		val p0 = path[0]
		val hit = folders.firstOrNull { it.name == p0 }

		if(hit !== null)
			return hit.lookup(path.subList(1, path.size))
		if(path.size > 1)
			return null

		return docs.plus(fileRefs).firstOrNull { it.name == p0 }
	}

	/** Lists all (non-folder) files below this folder, recursively */
	fun ls() : Sequence<Pair<String, FileInfo>> = sequence {
		yieldAll(docs.plus(fileRefs).map { it.name to it })

		for(f in folders)
			yieldAll(f.ls().map { (k,v) -> "$name/$k" to v })
	}
}
