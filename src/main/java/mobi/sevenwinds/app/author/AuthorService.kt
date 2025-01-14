package mobi.sevenwinds.app.author

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

object AuthorService {

    suspend fun createAuthor(request: CreateAuthorRequest): AuthorResponse = withContext(Dispatchers.IO) {
        transaction {
            val entity = AuthorEntity.new {
                this.fullName = request.fullName
                this.createdAt = DateTime.now()
            }
            entity.toResponse()
        }
    }

    private fun AuthorEntity.toResponse() = AuthorResponse(
        id = this.id.value,
        fullName = this.fullName,
        createdAt = this.createdAt.toString()
    )
}

data class AuthorResponse(
    val id: Int,
    val fullName: String,
    val createdAt: String
)
