package com.ft.ftchinese.text

/** Immutable compact trie. Transitions are sorted and binary-searched at runtime. */
class ChineseTrie private constructor(
    private val firstEdge: IntArray,
    private val edgeCount: IntArray,
    private val edgeChars: IntArray,
    private val edgeTargets: IntArray,
    private val terminalValues: IntArray,
    private val values: Array<String>,
) {
    fun convert(source: String): String {
        if (source.isEmpty()) return source
        val input = source.replace('·', '•')
        val result = StringBuilder(input.length)
        var index = 0
        while (index < input.length) {
            var node = 0
            var cursor = index
            var matchEnd = -1
            var matchValue = -1
            while (cursor < input.length) {
                val next = child(node, input[cursor].code)
                if (next < 0) break
                node = next
                cursor++
                val value = terminalValues[node]
                if (value >= 0) {
                    matchEnd = cursor
                    matchValue = value
                }
            }
            if (matchValue >= 0) {
                result.append(values[matchValue])
                index = matchEnd
            } else {
                result.append(input[index])
                index++
            }
        }
        return result.toString()
    }

    private fun child(node: Int, character: Int): Int {
        var low = firstEdge[node]
        var high = low + edgeCount[node] - 1
        while (low <= high) {
            val middle = (low + high) ushr 1
            when {
                edgeChars[middle] < character -> low = middle + 1
                edgeChars[middle] > character -> high = middle - 1
                else -> return edgeTargets[middle]
            }
        }
        return -1
    }

    companion object {
        fun fromDictionary(dictionary: Map<String, String>): ChineseTrie {
            val nodes = mutableListOf(BuildNode())
            val values = mutableListOf<String>()
            val valueIndexes = HashMap<String, Int>()

            dictionary.forEach { (word, replacement) ->
                if (word.isEmpty()) return@forEach
                var node = 0
                word.forEach { character ->
                    node = nodes[node].children.getOrPut(character.code) {
                        nodes += BuildNode()
                        nodes.lastIndex
                    }
                }
                val valueIndex = valueIndexes.getOrPut(replacement) {
                    values += replacement
                    values.lastIndex
                }
                nodes[node].terminalValue = valueIndex
            }

            val firstEdge = IntArray(nodes.size)
            val edgeCount = IntArray(nodes.size)
            val edgeChars = ArrayList<Int>()
            val edgeTargets = ArrayList<Int>()
            nodes.forEachIndexed { index, buildNode ->
                firstEdge[index] = edgeChars.size
                buildNode.children.entries.sortedBy { it.key }.forEach { (character, target) ->
                    edgeChars += character
                    edgeTargets += target
                }
                edgeCount[index] = buildNode.children.size
            }

            return ChineseTrie(
                firstEdge = firstEdge,
                edgeCount = edgeCount,
                edgeChars = edgeChars.toIntArray(),
                edgeTargets = edgeTargets.toIntArray(),
                terminalValues = IntArray(nodes.size) { nodes[it].terminalValue },
                values = values.toTypedArray(),
            )
        }
    }

    private class BuildNode {
        val children = HashMap<Int, Int>()
        var terminalValue = -1
    }
}
