package com.mocharealm.compound.domain.model

data class Text(
    val content: String,
    val entities: List<TextEntity> = emptyList(),
) {
    data class TextEntity(
        val offset: Int, val length: Int, val type: TextEntityType
    )

    sealed class TextEntityType {
        data object Bold : TextEntityType()
        data object Italic : TextEntityType()
        data object Underline : TextEntityType()
        data object Strikethrough : TextEntityType()
        data object Code : TextEntityType()
        data object Pre : TextEntityType()
        data class PreCode(val language: String) : TextEntityType()
        data class TextUrl(val url: String) : TextEntityType()
        data object Url : TextEntityType()
        data object Mention : TextEntityType()
        data object Spoiler : TextEntityType()
        data object EmailAddress : TextEntityType()
        data object PhoneNumber : TextEntityType()
        data class CustomEmoji(val customEmojiId: Long) : TextEntityType()
    }
}