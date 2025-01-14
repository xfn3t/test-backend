package mobi.sevenwinds.app.budget

import io.restassured.RestAssured
import io.restassured.http.ContentType
import kotlinx.coroutines.runBlocking
import mobi.sevenwinds.app.author.AuthorService.createAuthor
import mobi.sevenwinds.app.author.AuthorTable
import mobi.sevenwinds.app.author.CreateAuthorRequest
import mobi.sevenwinds.common.ServerTest
import mobi.sevenwinds.common.jsonBody
import mobi.sevenwinds.common.toResponse
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BudgetApiKtTest : ServerTest() {

    @BeforeEach
    internal fun setUp() {
        transaction { BudgetTable.deleteAll() }
        transaction { AuthorTable.deleteAll() }
    }

    @Test
    fun testBudgetPagination() = runBlocking {
        // Создание авторов
        val author1 = createAuthor(CreateAuthorRequest("Author 1"))
        val author2 = createAuthor(CreateAuthorRequest("Author 2"))
        val author3 = createAuthor(CreateAuthorRequest("Author 3"))

        // Добавляем записи в базу, используя ID авторов
        addRecord(BudgetRecord(2020, 5, 10, BudgetType.Приход, author1.id))
        addRecord(BudgetRecord(2020, 5, 5, BudgetType.Приход, null))  // Без автора
        addRecord(BudgetRecord(2020, 5, 20, BudgetType.Приход, author2.id))
        addRecord(BudgetRecord(2020, 5, 30, BudgetType.Приход, null))  // Без автора
        addRecord(BudgetRecord(2020, 5, 40, BudgetType.Приход, author3.id))
        addRecord(BudgetRecord(2030, 1, 1, BudgetType.Расход, null))    // Без автора

        // Выполняем запрос с ожиданием формата JSON
        val response = RestAssured.given()
            .accept(ContentType.JSON) // Ожидаемый формат ответа - JSON
            .queryParam("limit", 3)
            .queryParam("offset", 1)
            .get("/budget/year/2020/stats")
            .then()
            .statusCode(200) // Проверяем, что статус ответа 200 (ОК)
            .extract()
            .response()
            .`as`(BudgetYearStatsResponse::class.java) // Парсим ответ в объект

        // Убедитесь, что items является List<BudgetRecord>
        val items: List<BudgetRecordResponse> = response.items

        // Выводим результаты для проверки
        println("${response.total} / ${items} / ${response.totalByType}")

        // Проверка общего количества записей
        assertThat(response.total).isEqualTo(5)
        // Проверка, что количество элементов в items равно 3
        assertThat(items).hasSize(3)
        // Проверка общего значения для типа "Приход"
        assertThat(response.totalByType[BudgetType.Приход.name]).isEqualTo(105)

        // Проверка, что авторы отображаются, только если они есть
        items.forEach { record ->
            if (record.authorId != null) {
                assertThat(record.authorFullName).isNotNull
                assertThat(record.authorCreatedAt).isNotNull
            } else {
                assertThat(record.authorFullName).isEqualTo("Unknown")
                assertThat(record.authorCreatedAt).isEqualTo("N/A")
            }
        }
    }

    @Test
    fun testStatsSortOrder() = runBlocking {
        // Создание авторов
        val author1 = createAuthor(CreateAuthorRequest("Author 1"))
        val author2 = createAuthor(CreateAuthorRequest("Author 2"))
        val author3 = createAuthor(CreateAuthorRequest("Author 3"))

        // Добавляем записи в базу, используя ID авторов
        addRecord(BudgetRecord(2020, 5, 100, BudgetType.Приход, author1.id))
        addRecord(BudgetRecord(2020, 1, 5, BudgetType.Приход, null))  // Без автора
        addRecord(BudgetRecord(2020, 5, 50, BudgetType.Приход, author2.id))
        addRecord(BudgetRecord(2020, 1, 30, BudgetType.Приход, null))  // Без автора
        addRecord(BudgetRecord(2020, 5, 400, BudgetType.Приход, author3.id))

        // Выполняем запрос
        RestAssured.given()
            .get("/budget/year/2020/stats?limit=100&offset=0")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println(response.items)

                // Проверяем порядок сортировки: месяц по возрастанию, сумма по убыванию
                assertThat(response.items[0].amount).isEqualTo(30)
                assertThat(response.items[1].amount).isEqualTo(5)
                assertThat(response.items[2].amount).isEqualTo(400)
                assertThat(response.items[3].amount).isEqualTo(100)
                assertThat(response.items[4].amount).isEqualTo(50)

                // Проверяем, что авторы отображаются, только если они есть
                response.items.forEach { record ->
                    if (record.authorId != null) {
                        assertThat(record.authorFullName).isNotNull
                        assertThat(record.authorCreatedAt).isNotNull
                    } else {
                        assertThat(record.authorFullName).isEqualTo("Unknown")
                        assertThat(record.authorCreatedAt).isEqualTo("N/A")
                    }
                }
            }
    }


    @Test
    fun testInvalidMonthValues() {
        RestAssured.given()
            .jsonBody(BudgetRecord(2020, -5, 5, BudgetType.Приход, null))
            .post("/budget/add")
            .then().statusCode(400)

        RestAssured.given()
            .jsonBody(BudgetRecord(2020, 15, 5, BudgetType.Приход, null))
            .post("/budget/add")
            .then().statusCode(400)
    }

    private fun addRecord(record: BudgetRecord) {
        RestAssured.given()
            .jsonBody(record)
            .post("/budget/add")
            .toResponse<BudgetRecord>().let { response ->
                assertThat(response).isEqualTo(record)
            }
    }
}
