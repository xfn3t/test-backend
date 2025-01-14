package mobi.sevenwinds.app.budget

import mobi.sevenwinds.app.author.AuthorEntity
import mobi.sevenwinds.app.author.AuthorTable
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object BudgetTable : IntIdTable("budget") {
    val year = integer("year")
    val month = integer("month")
    val amount = integer("amount")
    val type = enumerationByName("type", 100, BudgetType::class)
    val author = reference("author_id", AuthorTable, onDelete = ReferenceOption.SET_NULL).nullable()
}

class BudgetEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<BudgetEntity>(BudgetTable)

    var year by BudgetTable.year
    var month by BudgetTable.month
    var amount by BudgetTable.amount
    var type by BudgetTable.type
    var author by AuthorEntity optionalReferencedOn BudgetTable.author

    fun toResponse(): BudgetRecordResponse {
        return BudgetRecordResponse(
            year = year,
            month = month,
            amount = amount,
            type = type,
            authorId = author?.id?.value,  // Обрабатываем nullable
            authorFullName = author?.fullName ?: "Unknown Author",  // Задаем значение по умолчанию
            authorCreatedAt = author?.createdAt?.toString() ?: "N/A"  // Задаем значение по умолчанию
        )
    }


}