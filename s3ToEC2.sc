import $ivy.`com.typesafe.play::play-json:2.6.1`
@
import play.api.libs.json._

case class S3File(srcKey:String, destFile: String)
case class Bucket(name: String, s3Files: List[S3File])
case class BucketList(buckets: List[Bucket])
implicit val s3FileReads = Json.reads[S3File]
implicit val BucketReads = Json.reads[Bucket]
implicit val BucketListReads = Json.reads[BucketList]

// In a request, a JsValue is likely to come from `request.body.asJson`
// or just `request.body` if using the `Action(parse.json)` body parser
val stream = new java.io.FileInputStream("s3files.json")
val jsonString: JsValue = Json.parse(stream)

val bucketListFromJson: JsResult[BucketList] = Json.fromJson[BucketList](jsonString)

bucketListFromJson match {
  case JsSuccess(bl: BucketList, path: JsPath) => 
    for (b <- bl.buckets) {
        println("Name: " + b.name)
        for (s3File <- b.s3Files) println(s3File.destFile)
     }
  case e: JsError => println("Errors: " + JsError.toJson(e).toString())
}

