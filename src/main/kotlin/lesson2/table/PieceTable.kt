package lesson2.table

import lesson2.TextCollector
import java.security.InvalidParameterException

class PieceTable(val original: String) : TextCollector {

    /**
     * Returns the length of this character sequence.
     */
    var length: Int = original.length
        private set


    private var added: String = ""
    private var nodes: List<Node> = listOf(Node(NodeType.ORIGINAL, 0, original.length))

    override fun add(line: String) {
        add(length, line)
    }

    override fun add(index: Int, line: String) {
        assertAddParams(index, line)

        val splitIndex = getIndexOfNode(index)
        val newNode = Node(NodeType.ADDED, added.length, line.length)

        if (splitIndex == nodes.size) {
            nodes += newNode
        }
        else {
            addNewNodeInMiddle(splitIndex, newNode, index)
        }

        added += line
        length += line.length
    }

    private fun addNewNodeInMiddle(splitIndex: Int, newNode: Node, index: Int) {
        val splitNode = nodes[splitIndex]
        val amountCharsBeforeSplitNode = getAmountCharsBeforeNodeIndex(splitIndex)

        val firstPartLength = index - amountCharsBeforeSplitNode
        val secondPartLength = splitNode.length - index + amountCharsBeforeSplitNode

        nodes = buildList {
            addAll(nodes.subList(0, splitIndex))
            if (firstPartLength > 0) {
                add(Node(splitNode.type, splitNode.start, firstPartLength))
            }
            add(newNode)
            if (secondPartLength > 0) {
                add(Node(splitNode.type, splitNode.start + index - amountCharsBeforeSplitNode, secondPartLength))
            }
            addAll(nodes.subList(splitIndex, nodes.size))
        }
    }

    private fun assertAddParams(index: Int, line: String) {
        if (index < 0 || index > length) {
            throw IndexOutOfBoundsException("Index $index is out of text bounds")
        }
        assertAddParams(line)
    }

    private fun assertAddParams(line: String) {
        if (line.isEmpty()) {
            throw InvalidParameterException("Line cannot be empty")
        }
    }

    private fun getIndexOfNode(textIndex: Int): Int {
        var handledChars = 0

        for ((index, node) in nodes.withIndex()) {
            if (handledChars + node.length > textIndex) {
                return index
            }
            handledChars += node.length
        }
        return nodes.size
    }

    private fun getAmountCharsBeforeNodeIndex(nodeIndex: Int): Int {
        return nodes.asSequence()
            .take(nodeIndex)
            .map(Node::length)
            .sum()
    }

    override fun remove(index: Int, length: Int) {
        assertRemoveParams(index, length)

        val splitFirstIndex = getIndexOfNode(index)
        val splitSecondIndex = getIndexOfNode(index + length - 1)

        val amountCharsBeforeFirstSplitNode = getAmountCharsBeforeNodeIndex(splitFirstIndex)
        val amountCharsBeforeSecondSplitNode = getAmountCharsBeforeNodeIndex(splitSecondIndex)

        val splitFirstNode = nodes[splitFirstIndex]
        val splitSecondNode = nodes[splitSecondIndex]

        val firstPartLength = index - amountCharsBeforeFirstSplitNode
        val secondPartLength = splitSecondNode.length - index - length + amountCharsBeforeSecondSplitNode

        nodes = buildList {
            addAll(nodes.subList(0, splitFirstIndex))
            if (firstPartLength > 0) {
                add(Node(splitFirstNode.type, splitFirstNode.start, firstPartLength))
            }
            if (secondPartLength > 0) {
                add(Node(splitSecondNode.type, splitSecondNode.start + firstPartLength + length, secondPartLength))
            }
            addAll(nodes.subList(splitSecondIndex + 1, nodes.size))
        }

        this.length -= length
    }

    private fun assertRemoveParams(index: Int, length: Int) {
        assertIndex(index)
        assertIndex(index + length - 1)
        assertLength(length)
    }

    private fun assertLength(length: Int) {
        if (length <= 0) {
            throw InvalidParameterException("Length $length must be a positive number")
        }
    }

    override operator fun get(indexTo: Int, indexFrom: Int): String {
        assertGetParams(indexTo, indexFrom)

        val splitFirstIndex = getIndexOfNode(indexTo)
        val splitSecondIndex = getIndexOfNode(indexFrom)

        val amountCharsBeforeFirstSplitNode = getAmountCharsBeforeNodeIndex(splitFirstIndex)
        val amountCharsBeforeSecondSplitNode = getAmountCharsBeforeNodeIndex(splitSecondIndex)

        val splitFirstNode = nodes[splitFirstIndex]
        val splitSecondNode = nodes[splitSecondIndex]

        val firstPartLength = indexTo - amountCharsBeforeFirstSplitNode
        val secondPartLength = splitSecondNode.length - indexFrom - 1 + amountCharsBeforeSecondSplitNode

        val tmpNodes = buildList {
            if (splitFirstIndex == splitSecondIndex) {
                add(Node(splitFirstNode.type, splitFirstNode.start + firstPartLength, indexFrom - indexTo + 1))
            }
            else {
                add(
                    Node(
                        splitFirstNode.type,
                        splitFirstNode.start + firstPartLength,
                        splitFirstNode.length - firstPartLength
                    )
                )
                if (splitFirstIndex + 1 <= splitSecondIndex) {
                    addAll(nodes.subList(splitFirstIndex + 1, splitSecondIndex))
                    add(Node(splitSecondNode.type, splitSecondNode.start, splitSecondNode.length - secondPartLength))
                }
            }
        }

        return toString(tmpNodes)
    }

    private fun assertIndex(index: Int) {
        if (index < 0 || index >= length) {
            throw IndexOutOfBoundsException("Index $index is out of text bounds")
        }
    }

    private fun assertGetParams(indexTo: Int, indexFrom: Int) {
        assertIndex(indexTo)
        assertIndex(indexFrom)
    }

    override fun toString(): String {
        return toString(nodes)
    }

    private fun toString(nodes: List<Node>): String {
        val stringBuilder = StringBuilder()

        nodes.asSequence()
            .map(::getStringByNode)
            .forEach { stringBuilder.append(it) }

        return stringBuilder.toString()
    }

    private fun getStringByNode(node: Node): String {
        val searchingString = getSearchingStringByType(node.type)
        return searchingString.substring(node.start, node.start + node.length)
    }

    private fun getSearchingStringByType(type: NodeType): String {
        return if (type == NodeType.ORIGINAL) original else added
    }

}

private data class Node(val type: NodeType, val start: Int, val length: Int)

private enum class NodeType { ORIGINAL, ADDED }