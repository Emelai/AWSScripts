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
import enumerations.myEnumerations._
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