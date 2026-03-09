package com.mocharealm.compound.data.mapper

import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.model.MessageBlock

object MessageAggregator {
    /**
     * Aggregates messages with matching mediaAlbumId into a single Message with multiple blocks.
     * Non-album messages pass through unchanged.
     */
    fun aggregate(messages: List<Message>): List<Message> {
        if (messages.isEmpty()) return emptyList()

        val result = mutableListOf<Message>()
        var currentGroup = mutableListOf<Message>()
        var currentAlbumId = 0L

        for (msg in messages) {
            val albumId = getAlbumId(msg)

            if (albumId != 0L) {
                if (currentAlbumId == albumId) {
                    currentGroup.add(msg)
                } else {
                    // Flush previous group
                    if (currentGroup.isNotEmpty()) {
                        result.add(mergeGroup(currentGroup))
                    }
                    currentGroup = mutableListOf(msg)
                    currentAlbumId = albumId
                }
            } else {
                // Flush previous group
                if (currentGroup.isNotEmpty()) {
                    result.add(mergeGroup(currentGroup))
                    currentGroup = mutableListOf()
                    currentAlbumId = 0L
                }
                result.add(msg)
            }
        }

        if (currentGroup.isNotEmpty()) {
            result.add(mergeGroup(currentGroup))
        }

        return result
    }

    private fun getAlbumId(msg: Message): Long {
        return when (val firstBlock = msg.blocks.firstOrNull()) {
            is MessageBlock.MediaBlock -> firstBlock.mediaAlbumId
            is MessageBlock.DocumentBlock -> firstBlock.mediaAlbumId
            else -> 0L
        }
    }

    private fun mergeGroup(group: List<Message>): Message {
        if (group.size == 1) return group[0]
        
        val first = group[0]
        val allBlocks = group.flatMap { it.blocks }.toMutableList()
        
        // Sort to ensure TextBlock(s) are at the end of the album.
        allBlocks.sortBy { it is MessageBlock.TextBlock }
        
        return first.copy(blocks = allBlocks)
    }
}
