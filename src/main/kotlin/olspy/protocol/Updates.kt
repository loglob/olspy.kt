package olspy.protocol

import kotlinx.datetime.Instant
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import olspy.support.EpochMillisSerializer
import olspy.support.ProjectOpConverter

@Serializable
data class WrappedUpdates(
	val updates : List<Update>,
	val nextBeforeTimestamp : Int? = null
)

/** An update aggregates multiple atomic changes */
@Serializable
data class Update(
	/** The version number before applying this update */
	val fromV : Int,
	/** The version number after applying this update */
	val toV : Int,
	/** meta information (users and timestamp) about this update */
	val meta : UpdateMeta,
	/** Labels applied to versions of this update */
	val labels : List<UpdateLabel>,
	/** The files that were edited in this update.
	 * Note that this covers only files edited via editor, not created, deleted, or moved files.
	 */
	@SerialName("pathnames")
	val pathNames : List<String>,
	/** Project-level (i.e. create/delete/rename file) operations */
	@SerialName("project_ops")
	val projectOps : List<ProjectOp>
)

@Serializable
data class UpdateMeta(
	val users : List<JsonElement>, // FIXME: typing
	@SerialName("start_ts")
	@Serializable(with = EpochMillisSerializer::class)
	val startAt : Instant,
	@SerialName("end_ts")
	@Serializable(with = EpochMillisSerializer::class)
	val endAt : Instant,
	val origin : UpdateMetaOrigin? = null
)

@Serializable
data class UpdateMetaOrigin(
	/** observed values: "history-migration" */
	val kind : String
)

/** A label applied to mark a project revision */
@Serializable
data class UpdateLabel(
	/** A unique ID for this label */
	val id : String,
	/** The name specified by the user */
	val comment : String,
	/** The exact version number this label is applied to */
	val version : Int,
	/** The UUID of the user that created this label */
	@SerialName("user_id")
	val userID : String,
	/** Creation timestamp */
	@SerialName("created_at")
	val createdAt : Instant
)

/** A project-level operation. One of three cases Add, Remove or Rename. */
@Serializable(with = ProjectOpConverter::class)
sealed class ProjectOp
{
	/** The exact version number of the operation */
	abstract val atV: Int
	/** The path being modified */
	abstract val path: String
}

data class AddFile(override val atV: Int, override  val path : String) : ProjectOp()
data class RemoveFile(override val atV: Int, override val path : String) : ProjectOp()
data class RenameFile(override val atV: Int, override val path : String, val newPath : String) : ProjectOp()
