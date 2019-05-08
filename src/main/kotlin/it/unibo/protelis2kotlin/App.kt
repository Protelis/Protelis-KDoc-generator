/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package it.unibo.protelis2kotlin
import java.io.File
import kotlin.text.RegexOption.MULTILINE
import kotlin.text.RegexOption.DOT_MATCHES_ALL
import java.io.File.separator as SEP

var context = Context(setOf())

val protelisFileExt = "pt"

data class Context(var protelisTypes: Set<String>)
fun registerProtelisType(pt: String) {
    context = context.copy(context.protelisTypes + pt)
}

interface DocPiece {
    companion object {
        val docParamRegex = """@param\s+(\w+)\s*([^\n]*)""".toRegex()
        val docReturnRegex = """@return\s+([^\n]*)""".toRegex()
        val docOtherDirectiveRegex = """@(\w+)\s+([^\n]*)""".toRegex()
    }

    fun extendWith(txt: String): DocPiece
}
data class DocText(val text: String) : DocPiece {
    override fun extendWith(txt: String): DocPiece {
        return DocText(text + txt)
    }
}

data class DocParam(
    val paramName: String,
    val paramType: String,
    val paramDescription: String
) : DocPiece {
    override fun extendWith(txt: String): DocPiece {
        return DocParam(paramName, paramType, paramDescription + txt)
    }
}

data class DocReturn(
    val returnType: String,
    val returnDescription: String
) : DocPiece {
    override fun extendWith(txt: String): DocPiece {
        return DocReturn(returnType, returnDescription + txt)
    }
}

data class DocDirective(
        val directive: String,
        val description: String
) : DocPiece {
    override fun extendWith(txt: String): DocPiece {
        return DocDirective(directive, description + txt)
    }
}

data class ProtelisFunArg(val name: String, val type: String)
data class ProtelisFun(
    val name: String,
    val params: List<ProtelisFunArg> = listOf(),
    val returnType: String = "",
    val public: Boolean = false,
    val genericTypes: Set<String> = setOf()
)

data class ProtelisFunDoc(val docPieces: List<DocPiece>)
data class ProtelisItem(val function: ProtelisFun, val docs: ProtelisFunDoc)

fun parseTypeAndRest(line: String): Pair<String, String> {
    // Works by finding the first comma which is not contained within parentheses
    var stillType = true
    var k = 0
    var parentheses = ""
    var type = line.takeWhile { c ->
        k++
        val cond = (c != ',' || stillType) && !(c == ',' && k>0 && parentheses.isEmpty())
        if (stillType && (c == '(' || c == '[')) parentheses += c
        if (stillType && (c == ')' || c == ']')) {
            parentheses = parentheses.dropLast(1)
            if (parentheses.isEmpty()) stillType = false
        }
        cond
    }
    return Pair(type, line.substring(k).trim())
}

fun parseDoc(doc: String): ProtelisFunDoc {
    var txt = ""
    val pieces: MutableList<DocPiece> = mutableListOf()
    doc.lines().map { """\s*\*\s*""".trimMargin().toRegex().replace(it, "").trim() }.forEach { l ->
        if (!l.startsWith("@")) {
            val partialtxt = l
            if(pieces.isEmpty()) txt += if(txt.isEmpty()) partialtxt else "\n $partialtxt"
            else {
                val last = pieces.last()
                pieces.remove(last)
                pieces.add(last.extendWith(" "+partialtxt))
            }
        }
        else {
            DocPiece.docParamRegex.matchEntire(l)?.let { matchRes ->
                val gs = matchRes.groupValues
                val (type, desc) = parseTypeAndRest(gs[2])
                pieces.add(DocParam(gs[1], type, desc))
                return@forEach
            }

            DocPiece.docReturnRegex.matchEntire(l)?.let { matchRes ->
                val gs = matchRes.groupValues
                val (type, desc) = parseTypeAndRest(gs[1])
                pieces.add(DocReturn(type, desc))
                return@forEach
            }

            DocPiece.docOtherDirectiveRegex.matchEntire(l)?.let {matchRes ->
                val directive = matchRes.groupValues[1]
                val desc = matchRes.groupValues[2]
                pieces.add(DocDirective(directive, desc))
                return@forEach
            }
        }
    }
    if (!txt.isEmpty()) pieces.add(0, DocText(txt))

    return ProtelisFunDoc(pieces)
}

fun parseProtelisFunction(fline: String): ProtelisFun {
    return ProtelisFun(
            name = """def (\w+)""".toRegex().find(fline)!!.groupValues[1],
            params = """\(([^\)]*)\)""".toRegex().find(fline)!!.groupValues[1].split(",")
                    .filter { !it.isEmpty() }.map { ProtelisFunArg(it.trim(), "") }.toList(),
            public = """(public def)""".toRegex().find(fline) != null)
}

fun parseFile(content: String): List<ProtelisItem> {
    val pitems = mutableListOf<ProtelisItem>()

    """^\s*(/\*\*(.*?)\*/)\n*([^\n]*)"""
            .toRegex(setOf(MULTILINE, DOT_MATCHES_ALL))
            .findAll(content)
            .forEach { matchRes ->
                val groups = matchRes.groupValues
                val doc = groups[2]
                val funLine = groups[3]
//                println("-----------------\nDoc: $doc")
//                parseDoc(doc).docPieces.forEach { p ->
//                    println("Doc piece: $p")
//                }
//                println("Function line: $funLine\n${parseProtelisFunction(funLine)}")
                val parsedDoc = parseDoc(doc)
                val parsedFun = parseProtelisFunction(funLine)
                pitems.add(ProtelisItem(parsedFun, parsedDoc))
            }
    return pitems
}

fun generateKotlinDoc(docs: ProtelisFunDoc): String {
    val docPieces = docs.docPieces
    return "/**\n" +
            docPieces.map { p ->
                if (p is DocText) {
                    p.text.lines().map { "  * $it" }.joinToString("\n")
                } else if (p is DocParam) {
                    "  * @param ${p.paramName} ${p.paramDescription}"
                } else if (p is DocReturn) {
                    "  * @return ${p.returnDescription}"
                } else if(p is DocDirective) {
                    "  * @${p.directive} ${p.description}"
                } else ""
            }.joinToString("\n") + "\n  */"
}

fun generateKotlinType(protelisType: String): String = when (protelisType) {
    "" -> ""
    "bool" -> "Boolean"
    "num" -> "Number"
    else ->
        """\(([^\)]*)\)\s*->\s*(.*)""".toRegex().matchEntire(protelisType)?.let { matchRes ->
            val args = matchRes.groupValues[1].split(",").map { generateKotlinType(it.trim()) }
            val ret = generateKotlinType(matchRes.groupValues[2])
            """(${args.joinToString(",")}) -> $ret"""
        } ?: """\[.*\]""".toRegex().matchEntire(protelisType)?.let { _ ->
            registerProtelisType("Tuple")
            "Tuple" // "List<${generateKotlinType()}>"
        } ?: if (protelisType.length == 1 && protelisType.any { it.isUpperCase() })
            protelisType
        else if ("""[A-Z]'""".toRegex().matches(protelisType))
            "${protelisType[0].inc()}"
        else if ("""\w+""".toRegex().matches(protelisType)) {
            registerProtelisType(protelisType)
            protelisType
        } else "Any"
}

fun sanitizeNameForKotlin(name: String): String = when (name) {
    "null" -> "`null`"
    else -> name
}

fun generateKotlinFun(fn: ProtelisFun): String {
    var genTypesStr = fn.genericTypes.joinToString(",")
    if (!genTypesStr.isEmpty()) genTypesStr = " <$genTypesStr>"

    return "fun$genTypesStr ${sanitizeNameForKotlin(fn.name)}(" +
            fn.params.map { "${sanitizeNameForKotlin(it.name)}: ${generateKotlinType(it.type)}" }.joinToString(", ") +
            "): ${generateKotlinType(fn.returnType)} = TODO()"
}

fun generateKotlinItem(pitem: ProtelisItem): String {
    val doc = pitem.docs
    var fn = pitem.function
    return generateKotlinDoc(doc) + "\n" + generateKotlinFun(fn)
}

fun generateKotlin(protelisItems: List<ProtelisItem>): String {
    // Retrieve type info from docs
    val pitems = protelisItems.map { pitem ->
        val doc = pitem.docs
        var fn = pitem.function
        pitem.copy(function = fn.copy(
                returnType = doc.docPieces.filter { it is DocReturn }.map { (it as DocReturn).returnType }.firstOrNull() ?: "",
                params = fn.params.map { param ->
                    param.copy(type = doc.docPieces.filter { it is DocParam && it.paramName == param.name }
                            .map { (it as DocParam).paramType }.firstOrNull() ?: "Any") },
                genericTypes = doc.docPieces.map {
                    if (!(it is DocParam)) "" else it.paramType
                }.flatMap { """([A-Z]'?)""".toRegex().findAll(it).map {
                    if (it.value.length == 2 && it.value[1] == '\'') "${it.value[0].inc()}"
                    else it.value
                }.toList() }.toSet()
        ))
    }

    return pitems.map { generateKotlinItem(it) }.joinToString("\n\n")
}

fun main(args: Array<String>) {
    if (args.size < 2) {
        println("USAGE: program <dir> <destDir>")
        return
    }

    val dir = args[0]
    val destDir = args[1]

    File(dir).walkTopDown().forEach { file ->
        if (!file.isFile || file.extension!= protelisFileExt) return@forEach

        val fileText: String = file.readText()

        println("Processing " + file.absolutePath)

        val pkg = """module (.+)""".toRegex().find(fileText)?.groupValues?.component2() ?: ""
        if (pkg.isEmpty()) {
            println("\tCannot parse Protelis package. Skipping.")
            return@forEach
        }

        val pkgParts = pkg.split(':')
        println("\tPackage: " + pkg)

        // RESET CONTEXT
        context = Context(setOf())

        val protelisItems = parseFile(fileText)
        println("\tFound " + protelisItems.size + " Protelis items.")

        val pkgCode = "package ${pkgParts.joinToString(".")}\n\n"
        val kotlinCode = generateKotlin(protelisItems)

        println("\tContext: " + context)

        val importCode = context.protelisTypes.map { when (it) {
            "ExecutionContext", "ExecutionEnvironment" -> "org.protelis.vm.$it"
            "Tuple" -> "org.protelis.lang.datatype.$it"
            else -> ""
        } }.filterNot { it.isEmpty() }.map { "import " + it }.joinToString("\n") + "\n\n"

        val kotlinFullCode = pkgCode + importCode + kotlinCode

        val outPath = "$destDir$SEP${pkgParts.joinToString(SEP)}$SEP${file.name.replace(".pt",".kt")}"

        println("\tWriting " + outPath)

        File(outPath).let {
            it.parentFile.mkdirs()
            it.createNewFile()
            it
        }.writeText(kotlinFullCode)
    }
}
