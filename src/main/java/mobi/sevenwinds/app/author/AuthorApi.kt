package mobi.sevenwinds.app.author

import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route

fun NormalOpenAPIRoute.author() {
    route("/author") {
        route("/create").post<Unit, AuthorResponse, CreateAuthorRequest>(info("Создать нового автора")) { _, body ->
            val response = AuthorService.createAuthor(body)
            respond(response)
        }
    }
}


data class CreateAuthorRequest(
    val fullName: String
)