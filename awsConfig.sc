/*
* Ciris is a framework that allows you to describe configurations as Scala code.
* It's main advantage is it allows type safety and precompiled configurations as part of the code
* The basics are quite straightforward as can be seen in this example. Essentially you define
* a configuration case class and load the data into it. You can load data from environmental variables
* or system properites or just straight up.
*/
import $ivy.`is.cir::ciris-core:0.4.0`, ciris._
import $ivy.`is.cir::ciris-enumeratum:0.4.0`, ciris.enumeratum._
import $ivy.`is.cir::ciris-generic:0.4.0`, ciris.generic._
import $ivy.`is.cir::ciris-refined:0.4.0`, ciris.refined._
//import $ivy.`is.cir::ciris-squants:0.4.0`, ciris.squants._

import enumeratum._
// define enumerations

object enumerations {
  sealed abstract class AppEnvironment extends EnumEntry
  object AppEnvironment extends Enum[AppEnvironment] {
    case object Local extends AppEnvironment
    case object Testing extends AppEnvironment
    case object Production extends AppEnvironment

    val values = findValues 
  }
}
val lEnums = enumerations

// Define Config case classes
case class AWSProfileConfig(
  awsProfile: String,
  )
case class AWSSQSConfig(
  numQueues: Int, 
  sqsQueueList: List[String]
  )

withValue(env[Option[lEnums.AppEnvironment]]("APP_ENV")) {
  case Some(lEnums.AppEnvironment.Local) | None =>
    loadConfig(
      env[String]("AWS_DEFAULT_PROFILE")
    ) { (awsProfile) =>
        AWSProfileConfig(
          awsProfile = awsProfile
        )
      }
  case _ =>
      loadConfig(
        AWSProfileConfig(
          awsProfile = "default"
        )
      )
  }

  val sqsQueues = 
  loadConfig(
    AWSSQSConfig(
      numQueues = 1,
      sqsQueueList = List("R-Start-Q")
    )
  )