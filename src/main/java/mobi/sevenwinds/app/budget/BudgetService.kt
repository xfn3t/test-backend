package mobi.sevenwinds.app.budget

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mobi.sevenwinds.app.author.AuthorEntity
import mobi.sevenwinds.app.author.AuthorTable
import mobi.sevenwinds.app.budget.BudgetTable.author
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.transaction

object BudgetService {

    // Валидация данных для записи бюджета
    private fun validateBudgetRecord(body: BudgetRecord) {
        require(body.year > 0) { "Год должен быть положительным числом." }
        require(body.month in 1..12) { "Месяц должен быть от 1 до 12." }
        require(body.amount in 0..1_000_000) { "Сумма должна быть от 0 до 1,000,000." }

        transaction {
            body.authorId?.let {
                requireNotNull(AuthorEntity.findById(it)) { "Author with ID $it not found" }
            }
        }
    }

    // Добавление записи бюджета
    suspend fun addRecord(body: BudgetRecord): BudgetRecordResponse = withContext(Dispatchers.IO) {
        validateBudgetRecord(body)

        transaction {
            val entity = BudgetEntity.new {
                this.year = body.year
                this.month = body.month
                this.amount = body.amount
                this.type = body.type
                body.authorId?.let {
                    this.author = AuthorEntity.findById(it)
                }
            }
            return@transaction entity.toResponse()
        }
    }

    // Получение статистики за год
    suspend fun getYearStats(param: BudgetYearParam): BudgetYearStatsResponse = withContext(Dispatchers.IO) {
        transaction {
            // Фильтрация по имени автора
            val authorFilter = param.authorName?.lowercase()?.let { name ->
                AuthorTable.fullName.lowerCase().like("%$name%")
            }

            // Основной запрос с фильтром
            val query = BudgetTable
                .join(AuthorTable, JoinType.LEFT, additionalConstraint = { author eq AuthorTable.id })
                .select {
                    (BudgetTable.year eq param.year) and (authorFilter ?: Op.TRUE)
                }
                .orderBy(BudgetTable.month to SortOrder.ASC, BudgetTable.amount to SortOrder.DESC) // Добавлено
                .limit(param.limit, param.offset)


            // Общее количество записей
            val total = BudgetTable
                .join(AuthorTable, JoinType.LEFT, additionalConstraint = { author eq AuthorTable.id })
                .select {
                    (BudgetTable.year eq param.year) and (authorFilter ?: Op.TRUE)
                }
                .count()

            // Сумма по типам
            val sumByType = BudgetTable
                .slice(BudgetTable.type, BudgetTable.amount.sum())
                .select { BudgetTable.year eq param.year }
                .groupBy(BudgetTable.type)
                .associate {
                    it[BudgetTable.type].name to (it[BudgetTable.amount.sum()] ?: 0)
                }.withDefault { 0 }

            // Преобразование данных в DTO
            val data = query.map { row ->
                val author = row[AuthorTable.id]?.let {
                    AuthorEntity.findById(it.value)
                }

                BudgetRecordResponse(
                    year = row[BudgetTable.year],
                    month = row[BudgetTable.month],
                    amount = row[BudgetTable.amount],
                    type = row[BudgetTable.type],
                    authorId = author?.id?.value,
                    authorFullName = author?.fullName ?: "Unknown",
                    authorCreatedAt = author?.createdAt?.toString() ?: "N/A"
                )
            }

            return@transaction BudgetYearStatsResponse(
                total = total,
                totalByType = sumByType,
                items = data
            )
        }
    }
}
