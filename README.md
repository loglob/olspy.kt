# Olspy.kt
Kotlin port of [olspy](https://github.com/loglob/olspy) based on KTOR.

## Usage
First, open a project using `Project.open()`:
```kt
// The recommended way is to get a share link and pass it using:
Project.open(Url("https://overleaf.com/<YOUR JOIN LINK>"))
// Alternatively, you can use your user credentials
Project.open(Url("https://overleaf.com/"), "<YOUR PROJECT ID>", "your@email.here", "<YOUR PASSWORD>")
```
### Project Information and File Structure
You need to open a `ProjectSession` to read documents, inspect the file structure or get project metadata.
Either call `project.join()` (with matching `.close()`), or use `project.withSession`:
```kt
project.withSession {
    // contains general project info, including its file tree
    val info = getProjectInfo()
    // info.project holds miscellaneous project information
    val mainFile = info.project.rootDocID;
    // gets the lines of an editable document
    val lines = session.getDocument(mainFile);
}
```

### Compiling
You can request a compilation using `project.compile()`:
```kt
// you can also specify different main files, draft mode, etc.
val build = project.compile()
```
You can then search the produced files:
```kt
// First, find an output file ID
val aux = compilation.outputFiles.first { it.type == "aux" }
// Then retrieve it via the project
val auxContent = project.getOutFile(aux);
// getOutFile() returns a HttpResponse
val auxString = auxContent.bodyAsText();
```
To check if a pdf was produced:
```kt
build.pdf?.let {
    val content = project.getOutFile(it).bodyAsBytes()
    File("compiled.pdf").writeBytes(content)
}
```
