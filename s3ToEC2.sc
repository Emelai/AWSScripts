import $ivy.`com.typesafe.play::play-json:2.6.1`
@
import play.api.libs.json._

case class Resident(name: String, age: Int, role: Option[String])
implicit val residentReads = Json.reads[Resident]

// In a request, a JsValue is likely to come from `request.body.asJson`
// or just `request.body` if using the `Action(parse.json)` body parser
val jsonString: JsValue = Json.parse(
  """{
    "name" : "Fiver",
    "age" : 4
  }"""
)

val residentFromJson: JsResult[Resident] = Json.fromJson[Resident](jsonString)

residentFromJson match {
  case JsSuccess(r: Resident, path: JsPath) => println("Name: " + r.name)
  case e: JsError => println("Errors: " + JsError.toJson(e).toString())
}

