
import $ivy.`com.typesafe.play::play-json:2.6.1`
//import $ivy.`com.github.seratch::awscala:0.6.+`
//mport $ivy.`com.micronautics::awslib_scala:1.1.11`

import play.api.libs.json._
//import awscala._, s3._
//mport com.amazonaws.services.s3.model.Bucket
//import com.micronautics.aws.S3._



// The JSON file is a List of buckets that consists of a List of Files and metadada
// The file tells us the S3 source key and the destination file name
case class S3File(srcKey:String, destFile: String)
// The Bucket metadata indicates the bucket name, a default destination (optional) 
// and whether the destination name is same as source
case class S3Bucket(name: String, defaultDest: String, destIsSrc: Int, s3Files: List[S3File])
case class BucketList(buckets: List[S3Bucket])
implicit val s3FileReads = Json.reads[S3File]
implicit val S3BucketReads = Json.reads[S3Bucket]
implicit val BucketListReads = Json.reads[BucketList]

// In a request, a JsValue is likely to come from `request.body.asJson`
// or just `request.body` if using the `Action(parse.json)` body parser
val stream = new java.io.FileInputStream("s3files.json")
val jsonString: JsValue = Json.parse(stream)


val bucketListFromJson: JsResult[BucketList] = Json.fromJson[BucketList](jsonString)

/** 
 *bucketListFromJson match {
 *  case JsSuccess(bl: BucketList, path: JsPath) => 
 *    for (b <- bl.buckets) {
 *        println("Name: " + b.name)
 *       for (s3File <- b.s3Files) 
 *         if (b.destIsSrc == 1)
 *           println(b.defaultDest + s3File.srcKey)
 *         else
 *           println(b.defaultDest + s3File.destFile)
 *   }
 * case e: JsError => println("Errors: " + JsError.toJson(e).toString())
 *}
*/

bucketListFromJson match {
  case JsSuccess(bl: BucketList, path: JsPath) => 
    for (b <- bl.buckets) {
      for (s3File <- b.s3Files) {
        val srcKeyFull:String = "s3://" + b.name + "/" + s3File.srcKey
        val destPath:String = if (b.destIsSrc == 1)
          b.defaultDest + s3File.srcKey
        else 
          b.defaultDest + s3File.destFile
        val myProfile= "zapgroup"
        %%('aws,"s3","cp",srcKeyFull,destPath,"--profile",myProfile)
      } 
    }
  case e: JsError => println("Errors: " + JsError.toJson(e).toString())
}
