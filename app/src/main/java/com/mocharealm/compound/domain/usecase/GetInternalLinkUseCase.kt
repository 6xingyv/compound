package com.mocharealm.compound.domain.usecase

import com.mocharealm.compound.domain.model.InternalLink
import com.mocharealm.compound.domain.repository.TelegramRepository

class GetInternalLinkUseCase (
    private val repository: TelegramRepository
) {
    suspend operator fun invoke(link: String): Result<InternalLink> {
        return repository.getInternalLink(link)
    }
}