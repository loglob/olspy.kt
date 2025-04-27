package olspy

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import olspy.protocol.JoinProjectArgs
import olspy.protocol.Opcode
import olspy.protocol.RPC_JOIN_PROJECT
import olspy.protocol.readPacket
import java.nio.ByteBuffer
import kotlin.test.*

class WebsocketParsing
{
	@Test
	fun openingPacket()
	{
		val pkt = readPacket(ByteBuffer.wrap("1::".toByteArray(Charsets.UTF_8)))

		assertEquals(Opcode.CONNECT, pkt.opcode)
		assertEquals(null, pkt.id)
		assertEquals(false, pkt.shouldAcknowledge)
		assertEquals("", pkt.payload)
	}

	@Test
	fun joinPacket()
	{
		val json = "{\"name\":\"joinProjectResponse\",\"args\":[]}"
		val pkt = readPacket(ByteBuffer.wrap("5:::$json".toByteArray(Charsets.UTF_8)))

		assertEquals(Opcode.EVENT, pkt.opcode)
		assertEquals(null, pkt.id)
		assertEquals(false, pkt.shouldAcknowledge)
		assertEquals(json, pkt.payload)

		val ev = pkt.eventPayload
		assertEquals(RPC_JOIN_PROJECT, ev.name)
		assertTrue(ev.args.isEmpty())
	}

	@Test
	fun heartbeat()
	{
		val pkt = readPacket(ByteBuffer.wrap("2::".toByteArray(Charsets.UTF_8)))

		assertEquals(Opcode.HEARTBEAT, pkt.opcode)
		assertEquals(null, pkt.id)
		assertEquals(false, pkt.shouldAcknowledge)
		assertEquals("", pkt.payload)
	}

	@Test
	fun fullJoinPacket()
	{
		val data = """5:::{"name":"joinProjectResponse","args":[{
			"publicId":"P.3wD2nBW0Ebo0adTQAAAJ", "project":{ "_id":"65c4ec91c7d163444d06840f","name":"olspy-test","rootDoc_id":"65c4ec91c7d163444d068414",
				"rootFolder":[{"_id":"65c4ec91c7d163444d06840e","name":"rootFolder","folders":[{"_id":"661f372c3e0728650ad83a94","name":"img","folders":[],"fileRefs":[{
				"_id":"661f373c3e0728650ad83aaf","name":"paper.jpg","linkedFileData":{"provider":"project_file","source_project_id":"661f26993e0728650ad8390c",
				"source_entity_path":"/img/paper.jpg"},"created":"2024-04-17T02:43:08.284Z"},{"_id":"661f37473e0728650ad83acb","name":"footerscroll.pdf",
				"linkedFileData":{"provider":"project_file","source_project_id":"661f26993e0728650ad8390c","source_entity_path":"/img/footerscroll.pdf"},
				"created":"2024-04-17T02:43:19.080Z"},{"_id":"66278b43bdd77952ce750b47","name":"1 action.png","linkedFileData":{"provider":"project_file",
				"source_project_id":"661f26993e0728650ad8390c","source_entity_path":"/img/1 action.png"},"created":"2024-04-23T10:19:47.403Z"},
				{"_id":"66278b4bbdd77952ce750b68","name":"2 action.png","linkedFileData":{"provider":"project_file",
				"source_project_id":"661f26993e0728650ad8390c","source_entity_path":"/img/2 action.png"},"created":"2024-04-23T10:19:55.956Z"},
				{"_id":"66278b63bdd77952ce750b8b","name":"3 action.png","linkedFileData":{"provider":"project_file","source_project_id":"661f26993e0728650ad8390c",
				"source_entity_path":"/img/3 action.png"},"created":"2024-04-23T10:20:19.082Z"},{"_id":"66278b71bdd77952ce750bae","name":"double_swirl.png",
				"linkedFileData":{"provider":"project_file","source_project_id":"661f26993e0728650ad8390c","source_entity_path":"/img/double_swirl.png"},
				"created":"2024-04-23T10:20:33.787Z"},{"_id":"66278b7cbdd77952ce750bd3","name":"swirl.png","linkedFileData":{"provider":"project_file",
				"source_project_id":"661f26993e0728650ad8390c","source_entity_path":"/img/swirl.png"},"created":"2024-04-23T10:20:44.275Z"},
				{"_id":"66278b90bdd77952ce750bf9","name":"free action.png","linkedFileData":{"provider":"project_file",
				"source_project_id":"661f26993e0728650ad8390c","source_entity_path":"/img/free action.png"},"created":"2024-04-23T10:21:04.987Z"},
				{"_id":"66278babbdd77952ce750c21","name":"re action.png","linkedFileData":{"provider":"project_file",
				"source_project_id":"661f26993e0728650ad8390c","source_entity_path":"/img/re action.png"},
				"created":"2024-04-23T10:21:31.190Z"}],"docs":[]},{"_id":"6653757c12680afb443702e5","name":"subdir",
				"folders":[],"fileRefs":[],"docs":[{"_id":"6653758012680afb44370305","name":"subfile.tex"}]}],
				"fileRefs":[{"_id":"65c4ec91c7d163444d068421","name":"frog.jpg","linkedFileData":null,
				"created":"2024-02-08T15:00:33.519Z"},{"_id":"661f383b3e0728650ad83b38","name":"spell.tex",
				"linkedFileData":{"provider":"project_file","source_project_id":"65bc2250c7d163444d05ec1b","source_entity_path":"/spell.tex"},
				"created":"2024-04-17T02:47:23.174Z"},{"_id":"65c4ee6ec7d163444d0684da","name":"goedendag la3 spells.pdf",
				"linkedFileData":{"provider":"project_output_file","source_project_id":"65bc2250c7d163444d05ec1b",
				"source_output_file_path":"output.pdf","build_id":"18d89436140-ab0dbdc1ea95a340"},"created":"2024-02-08T15:08:30.957Z"},
				{"_id":"661f39983e0728650ad83bd5","name":"dndbook.cls","linkedFileData":{"provider":"project_file","source_project_id":"661f26993e0728650ad8390c",
				"source_entity_path":"/dndbook.cls"},"created":"2024-04-17T02:53:12.862Z"},{"_id":"661f3ee53e0728650ad8413e","name":"cprbook.cls","linkedFileData":
				{"provider":"project_file","source_project_id":"661f26993e0728650ad8390c","source_entity_path":"/cprbook.cls"},
				"created":"2024-04-17T03:15:49.509Z"},{"_id":"66278ab4bdd77952ce750aef","name":"pf2ebook.cls",
				"linkedFileData":{"provider":"project_file","source_project_id":"661f26993e0728650ad8390c",
				"source_entity_path":"/pf2ebook.cls"},"created":"2024-04-23T10:17:24.025Z"}],"docs":[
				{"_id":"65c4ec91c7d163444d068414","name":"main.tex"},{"_id":"65c4ec91c7d163444d06841b","name":"sample.bib"},
				{"_id":"65c9304cc7d163444d06c03d","name":"fatal-error.tex"},{"_id":"65c936b3c7d163444d06c0e8","name":"recoverable-error.tex"},
				{"_id":"65c936c1c7d163444d06c105","name":"warning.tex"},{"_id":"65c94089c7d163444d06c331","name":"single-error.tex"},
				{"_id":"65c9510cc7d163444d06c626","name":"baz.tex"},{"_id":"66060c04123167a638600a73","name":"cami.tex.tex"},
				{"_id":"66185d0fe582be5a51cba958","name":"libcpr-test.tex"},{"_id":"66278b27bdd77952ce750b25","name":"pftext.tex"},
				{"_id":"6653758612680afb44370327","name":"mainfile.tex"}]}],"publicAccesLevel":"tokenBased","dropboxEnabled":false,
				"compiler":"pdflatex","description":"","spellCheckLanguage":"en","deletedByExternalDataSource":false,
				"deletedDocs":[{"_id":"65c950f6c7d163444d06c5fa","name":"foo.tex","deletedAt":"2024-02-11T22:58:09.854Z"}],
				"members":[],"invites":[],"imageName":"texlive-full:2020.1","owner":{"_id":"63ceddf755065a00970392db"},
				"features":{"collaborators":-1,"versioning":true,"dropbox":true,"github":true,"gitBridge":true,
				"compileTimeout":180,"compileGroup":"standard","templates":true,"references":true,"trackChanges":true,
				"referencesSearch":true,"mendeley":true,"trackChangesVisible":false,"symbolPalette":false},
				"trackChangesState":false},"permissionsLevel":"readOnly","protocolVersion":2}]}
		""".trimIndent()

		val pkt = readPacket(ByteBuffer.wrap(data.toByteArray(Charsets.UTF_8)))
		assertEquals(Opcode.EVENT, pkt.opcode)
		assertEquals(null, pkt.id)
		assertEquals(false, pkt.shouldAcknowledge)

		val ev = pkt.eventPayload
		assertEquals(RPC_JOIN_PROJECT, ev.name)

		val args = Json.decodeFromJsonElement<JoinProjectArgs>(ev.args[0])

		assertEquals("P.3wD2nBW0Ebo0adTQAAAJ", args.publicId)
		assertEquals(2, args.protocolVersion)
	}

}