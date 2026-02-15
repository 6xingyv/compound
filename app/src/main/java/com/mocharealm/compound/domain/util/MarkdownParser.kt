package com.mocharealm.compound.domain.util

import com.mocharealm.compound.domain.model.TextEntity
import com.mocharealm.compound.domain.model.TextEntityType

object MarkdownParser {

    private data class MarkdownRule(
        val regex: Regex,
        val createType: (MatchResult) -> TextEntityType,
        val contentGroupIndex: Int = 1
    )

    private val rules = listOf(
        // PreCode: ```lang\ncode``` (supports optional lang, optional newline)
        // Group 1: Lang, Group 2: Content
        MarkdownRule(Regex("""```(\w*)\n?([\s\S]*?)```"""), { res -> 
            TextEntityType.PreCode(res.groupValues[1]) 
        }, contentGroupIndex = 2),
        
        // Code: `code`
        MarkdownRule(Regex("""`([^`]+)`"""), { _ -> TextEntityType.Code }),

        // TextUrl: [text](url)
        // Group 1: Text, Group 2: URL
        MarkdownRule(Regex("""\[(.*?)\]\((.*?)\)"""), { res -> 
            TextEntityType.TextUrl(res.groupValues[2]) 
        }, contentGroupIndex = 1),

        // Bold: **bold**
        MarkdownRule(Regex("""\*\*(.*?)\*\*"""), { _ -> TextEntityType.Bold }),

        // Italic: *italic*
        MarkdownRule(Regex("""\*(.*?)\*"""), { _ -> TextEntityType.Italic }),

        // Strikethrough: ~~strike~~
        MarkdownRule(Regex("""~~(.*?)~~"""), { _ -> TextEntityType.Strikethrough }),

        // Underline: <u>underline</u>
        MarkdownRule(Regex("""<u>(.*?)</u>"""), { _ -> TextEntityType.Underline }),

        // Spoiler: ||spoiler||
        MarkdownRule(Regex("""\|\|(.*?)\|\|"""), { _ -> TextEntityType.Spoiler })
    )

    data class MarkdownMatch(
        val type: TextEntityType,
        val fullRange: IntRange,
        val contentRange: IntRange,
        val content: String
    )

    /**
     * Parses the text and returns a list of matches.
     * Useful for UI highlighting where we want to keep the symbols but know where they are.
     */
    fun findMatches(text: CharSequence): List<MarkdownMatch> {
        val matches = mutableListOf<MarkdownMatch>()
        rules.forEach { rule ->
            rule.regex.findAll(text).forEach { result ->
                val contentGroup = result.groups[rule.contentGroupIndex]
                if (contentGroup != null) {
                    matches.add(
                        MarkdownMatch(
                            type = rule.createType(result),
                            fullRange = result.range,
                            contentRange = contentGroup.range,
                            content = contentGroup.value
                        )
                    )
                }
            }
        }
        return matches.sortedBy { it.fullRange.first }
    }

    /**
     * Parses the text, strips markdown symbols, and returns the Clean Text + TextEntities.
     * Useful for sending messages.
     */
    fun parseForSending(text: String): Pair<String, List<TextEntity>> {
        val matches = findMatches(text)
        if (matches.isEmpty()) return text to emptyList()

        val builder = StringBuilder()
        val entities = mutableListOf<TextEntity>()
        var lastIndex = 0

        // Note: Simple handling for non-overlapping matches.
        // If matches overlap, this logic needs to be more robust (e.g. stack based).
        // Since regexes here might overlap if not careful (e.g. * inside **), 
        // but greedy/non-greedy .*? usually handles siblings well.
        // Nested is tricky with Regex. For now, assuming flat or simple nesting.
        // But `sortedBy` helps. If we have nested `**a *b* c**`, the outer `**` might match first?
        // Actually regex `\*\*(.*?)\*\*` matches the whole thing.
        // If we want to support nesting, we need a real parser. 
        // For this MVP, let's stick to flat replacement or outer-first.
        // If there's overlap, we skip properties that are subsumed?
        // Simple approach: Filter out matches that are inside processed ranges.
        
        val validMatches = mutableListOf<MarkdownMatch>()
        var maxReachable = 0
        for (match in matches) {
            if (match.fullRange.first >= maxReachable) {
                validMatches.add(match)
                maxReachable = match.fullRange.last + 1
            }
        }

        for (match in validMatches) {
            // Append text before match
            builder.append(text.substring(lastIndex, match.fullRange.first))
            
            // Record start of content in the new string
            val start = builder.length // UTF-16 length
            
            // Append content (without symbols)
            builder.append(match.content)
            
            val length = builder.length - start
            entities.add(TextEntity(start, length, match.type))
            
            lastIndex = match.fullRange.last + 1
        }
        
        // Append remaining
        if (lastIndex < text.length) {
            builder.append(text.substring(lastIndex))
        }

        return builder.toString() to entities
    }
}
