package olspy.protocol

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The success state of a `compile()` request */
@Serializable
enum class CompileStatus
{
	/** A PDF was produced, but there may have been non-fatal errors */
	@SerialName("success")
	SUCCESS,
	/** No PDF was produced due to a fatal error in the latex source code */
	@SerialName("failure")
	FAILURE,
	/** Either another compilation is already running or one finished very recently */
	@SerialName("too-recently-compiled")
	TOO_RECENTLY_COMPILED,
	/** Auto-compile is enabled and a rate limit was hit */
	@SerialName("autocompile-backoff")
	AUTOCOMPILE_BACKOFF,
	/** Corresponds to internal HTTP code 409.
		Probably transient. */
	@SerialName("conflict")
	CONFLICT,
	/** Internal compile server is temporarily unavailable.
		Corresponds to internal HTTP code 503.
		Also returned when the server is shutting down.
	 */
	@SerialName("unavailable")
	UNAVAILABLE,
	/** Could not run compile because the latex configuration was invalid */
	@SerialName("validation-problems")
	VALIDATION_PROBLEMS,
	/** Corresponds to internal HTTP code 423 */
	@SerialName("compile-in-progress")
	COMPILE_IN_PROGRESS,
	/** Internal compile API hit some size limit */
	@SerialName("project-too-large")
	PROJECT_TOO_LARGE,
	/** Equivalent (?) to Failure if stoppedOnFirstError is active */
	@SerialName("stopped-on-first-error")
	STOPPED_ON_FIRST_ERROR,
	/** Unknown internal error */
	@SerialName("error")
	ERROR,
	@Suppress("SpellCheckingInspection")
	@SerialName("timedout")
	TIMED_OUT,
	@SerialName("terminated")
	TERMINATED,
	/** Files weren't synchronized properly. Transient. */
	@SerialName("retry")
	RETRY,
	/** A compile run failed on validation. */
	@SerialName("validation-fail")
	VALIDATION_FAIL,
	/** A validation-only (no compile) run was successful */
	@SerialName("validation-pass")
	VALIDATION_PASS
}

@Serializable
data class OutputFile(
	val path : String,
	/** A complete URL for this file */
	val url : String,
	/** The file extension */
	val type : String,
	/** The unique build ID that produced this file */
	val build : String,
	/** Always empty (?), only present on PDF files */
	val ranges : List<Int>? = null,
	/** The number of bytes, only present on PDF files */
	val size : Int? = null,
	/** A timestamp only present on PDF files */
	val createdAt : Instant? = null,
)

@Serializable
data class CompileStats(
	/** Identical to `latexRunsWithErrors0` */
	@SerialName("latexmk-errors")
	val latexmkErrors : Int,
	/** Always 0 (?) */
	@SerialName("latex-runs")
	val latexRuns : Int,
	/** Always 0 (?) See `latexRunsWithErrors0` instead */
	@SerialName("latex-runs-with-errors")
	val latexRunsWithErrors : Int,
	@SerialName("latex-runs-0")
	val latexRuns0 : Int,
	/** 1 if a successful compilation encountered any recoverable errors, 0 otherwise */
	@SerialName("latex-runs-with-errors-0")
	val latexRunsWithErrors0 : Int,
	/** Output PDF file size in bytes, if there is one. Identical to `OutputFile.Size`. */
	@SerialName("pdf-size")
	val pdfSize : Int? = null
)

/** Duration of the individual compile steps */
@Serializable
data class CompileTimings(
	val sync : Int,
	val compile : Int,
	val output : Int,
	@SerialName("compileE2E")
	val total : Int
)

/** Information returned by the compile API */
@Serializable
data class CompileInfo(
	/** The overall success state of the compilation.
	 * Note that SUCCESS is also returned id there were recoverable compilation errors.
	 */
	val status : CompileStatus,
	/** The files produced by the compilation.
	 * Note that a PDF may be produced even if there were errors.
	 */
	val outputFiles : List<OutputFile>,
	/** Always "standard" (?) */
	val compileGroup : String,
	/** Information on compile errors */
	val stats : CompileStats,
	/** Information on compile runtime */
	val timings : CompileTimings
) {
	/** Whether the compilation was successful, meaning it both indicates success and produces exactly one PDF */
	val isSuccessful get()
			= status == CompileStatus.SUCCESS && outputFiles.count { it.path.endsWith((".pdf"))} == 1

	/** The first PDF produced by this run. Also check `isSuccessful`
	 * Given as a filename that can be requested.
	 */
	val pdf get()
			= outputFiles.firstOrNull { it.path.endsWith(".pdf") }
}
