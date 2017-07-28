@doc("A script to copy any number of files from s3 to local machine based on a json file listing source and dest")
import $ivy.`io.circe::circe-core:0.8.0`
import $ivy.`io.circe::circe-generic:0.8.0`
import $ivy.`io.circe::circe-parser:0.8.0`
import $ivy.`is.cir::ciris-core:0.4.0`, ciris._
//import $ivy.`is.cir::ciris-enumeratum:0.4.0`, ciris.enumeratum._
//import $ivy.`is.cir::ciris-generic:0.4.0`, ciris.generic._
//import $ivy.`is.cir::ciris-refined:0.4.0`, ciris.refined._
//import $ivy.`is.cir::ciris-squants:0.4.0`, ciris.squants._


import $plugin.$ivy.`org.scalamacros:::paradise:2.1.0`

import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

// Define Config class for Ciris
case class Config(awsProfile: String)
val config =
  loadConfig(
    env[String]("AWS_PROFILE") // Reads environment variable AWS_PROFILE
  ) { (awsProfile) =>
    Config(
      awsProfile = awsProfile
    )
  }
// The JSON file is a List of buckets that consists of a List of Files and metadada
// The file tells us the S3 source key and the destination file name
case class S3File(srcKey:String, destFile: String)
// The Bucket metadata indicates the bucket name, a default destination (optional) 
// and whether the destination name is same as source
case class S3Bucket(name: String, defaultDest: String, destIsSrc: Int, s3Files: List[S3File])
case class BucketList(buckets: List[S3Bucket])

//Open file as stream and store in String
val stream = new java.io.FileInputStream("s3files.json")
val jsonString: String = scala.io.Source.fromInputStream(stream).mkString

/*parse(jsonString) match {
  case Left(failure) => println("Invalid JSON :(")
  case Right(json) => println("Yay, got some JSON!" + bucketListJson)
}
*/
// decode String into an Either
val blX = parser.decode[BucketList](jsonString).getOrElse()

/*
if (blX.isInstanceOf[BucketList]) {
  val bl = blX.asInstanceOf[BucketList]
  for (b <- bl.buckets) {
    for (s3File <- b.s3Files) {
      val srcKeyFull:String = "s3://" + b.name + "/" + s3File.srcKey
      if (b.destIsSrc == 1)
        println(b.defaultDest + s3File.srcKey)
      else 
        println(b.defaultDest + s3File.destFile)
    } 
  }
} else {
  println("error in decoding JSON file")
}
*/

// if decode is successful cast the Any Result as BucketList and traverse and download
if (blX.isInstanceOf[BucketList]) {
  val bl = blX.asInstanceOf[BucketList]
  for (b <- bl.buckets) {
    for (s3File <- b.s3Files) {
      val srcKeyFull:String = "s3://" + b.name + "/" + s3File.srcKey
      val destPath:String = if (b.destIsSrc == 1)
        b.defaultDest + s3File.srcKey
      else 
        b.defaultDest + s3File.destFile
        val myProfile = config match {
          case Right(config) => config.awsProfile
          case Left(config) => "fail"
        }
        if(myProfile=="fail") {// no defined profile - proper option on EC2 instance
          println("copying files without profile")
          %%('aws,"s3","cp",srcKeyFull,destPath)
        }
        else { // profile defined - needed when run on local machine
          println(s"copying files with profile $myProfile")
          %%('aws,"s3","cp",srcKeyFull,destPath,"--profile",myProfile)
        }
    } 
  }
} else {
  println("error in decoding JSON file")
}
