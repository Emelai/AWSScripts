/*
* Ciris is a framework that allows you to describe configurations as Scala code.
* It's main advantage is it allows type safety and precompiled configurations as part of the code
* The basics are quite straightforward as can be seen in this example. Essentially you define
* a configuration case class and load the data into it. You can load data from environmental variables
* or system properites or just straight up.
*/
import $ivy.`is.cir::ciris-core:0.4.0`, ciris._
//import $ivy.`is.cir::ciris-enumeratum:0.4.0`, ciris.enumeratum._
//import $ivy.`is.cir::ciris-generic:0.4.0`, ciris.generic._
//import $ivy.`is.cir::ciris-refined:0.4.0`, ciris.refined._
//import $ivy.`is.cir::ciris-squants:0.4.0`, ciris.squants._

import $exec.enumerations
import enumerations.awsEnumerations._
// define enumerations
//val lEnums = enumerations

// Define Config case classes
case class AWSProfileConfig(
  awsProfile: String,
  )
case class AWSSQSConfig(
  numMessages: Int, 
  sqsQueueList: List[String]
  )
case class S3CopyFiles(
  filesList : List[String],
  dirName : String,
  procScript : String
  )
//withValue(env[Option[lEnums.AppEnvironment]]("APP_ENV")) {
//  case Some(lEnums.AppEnvironment.Local) | None =>
val awsProfile = withValue(env[Option[AppEnvironment]]("APP_ENV")) {
  case Some(AppEnvironment.Local) =>
    loadConfig(
      // on Local will be changing profile depending on what working on, so set in env
      env[String]("AWS_DEFAULT_PROFILE")
    ) { (awsProfile) =>
        AWSProfileConfig(
          awsProfile = awsProfile
        )
      }
  case _ | None =>
      loadConfig(
        AWSProfileConfig(
          awsProfile = "default"
        )
      )
  }

  val sqsQueues = 
  loadConfig(
    AWSSQSConfig(
      numMessages = 10,
      sqsQueueList = List("R-Start-Q")
    )
  )

  // 
  // Create a series of methods that give you configs based on enum.
  // First call the function which uses the string which comes from the SQS event, to finds the appropriate enum
  // The calling script then can use the enum to get the configs needed on demand from the other medthods
  // This approach allows for dynamic configuration based on data you receive in the event
  //
  def s3BucketEnum(s3Info:String) = {
    s3Info match {
      case "zapgroup-nbo-in" => S3Bucket.withName("NBO")
      case "zapgroup-cro-in" => S3Bucket.withName("CRO")
      case _ => S3Bucket.withName("TESTIT")
    }
  }
  def s3CopyConfig(s3BucketEnum:S3Bucket) = {
    s3BucketEnum match {
      case S3Bucket.NBO => loadConfig(
        S3CopyFiles(
        filesList = List("nbo_assets.csv","customers_delta.csv","success.txt"),
        dirName = "NBO",
        procScript = "/data/NBO/Scripts/NBOProcess.sc"
        )
      )
      case S3Bucket.CRO => loadConfig(
        S3CopyFiles(
          filesList = List("cro_assets.csv","customers_delta.csv","success.txt"),
          dirName = "CRO",
          procScript = "/data/CRO/Scripts/NBOProcess.sc"
        )
      )
      case _  => loadConfig(
        S3CopyFiles(
          filesList = List("test.csv","success.txt"),
          dirName = "TESTIT",
          procScript = "/data/TESTIT/Scripts/TESTProcess.sc"
        )
      )
    }
  }