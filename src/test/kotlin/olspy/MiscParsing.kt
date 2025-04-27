package olspy

import kotlinx.serialization.json.*
import olspy.protocol.http.Update
import kotlin.test.*
import kotlinx.datetime.*
import olspy.protocol.http.AddFile
import olspy.protocol.http.ProjectOp
import olspy.protocol.http.RemoveFile
import olspy.protocol.http.RenameFile
import kotlin.time.Duration.Companion.seconds

class MiscParsing
{
	fun assertClose(want : Instant, got : Instant)
	{
		val diff = got - want
		if(diff.absoluteValue > 1.seconds)
			throw AssertionError("Expected $want, but got $got (difference of $diff)")
	}

	@Test
	fun parseUpdate()
	{
		val data = """
			{
				"fromV": 87,
				"toV": 95,
				"meta": {
					"users": [
						{
							"first_name": "anon",
							"last_name": "",
							"email": "anon@anon.ymous",
							"id": "63ceddf755065a00970392db"
						}
					],
					"start_ts": 1716745620626,
					"end_ts": 1716745774311
				},
				"labels": [],
				"pathnames": [
					"mainfile.tex",
					"subdir/subfile.tex"
				],
				"project_ops": []
			}
		""".trimIndent()

		val dec = Json.decodeFromString<Update>(data)

		assertClose(Instant.parse("2024-05-26T17:47:00Z"),  dec.meta.startAt)
		assertClose(Instant.parse("2024-05-26T17:49:34Z"),  dec.meta.endAt)

		println(dec)
	}

	@Test
	fun parseOps()
	{
		val data = """
			[
				{
					"rename": {
						"pathname": "bar.tex",
						"newPathname": "baz.tex"
					},
					"atV": 21
				},
				{
					"add": {
						"pathname": "bar.tex"
					},
					"atV": 20
				},
				{
					"remove": {
						"pathname": "foo.tex"
					},
					"atV": 19
				},
				{
					"add": {
						"pathname": "foo.tex"
					},
					"atV": 18
				}
			]
		""".trimIndent()
		val ops = Json.decodeFromString<List<ProjectOp>>(data)

		assertIs<RenameFile>(ops[0])
		assertIs<AddFile>(ops[1])
		assertIs<RemoveFile>(ops[2])
		assertIs<AddFile>(ops[3])

		println(ops)
	}

}