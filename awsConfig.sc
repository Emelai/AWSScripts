//@doc("Ciris Configuration")
import $ivy.`is.cir::ciris-core:0.4.0`, ciris._
//import $ivy.`is.cir::ciris-enumeratum:0.4.0`, ciris.enumeratum._
//import $ivy.`is.cir::ciris-generic:0.4.0`, ciris.generic._
//import $ivy.`is.cir::ciris-refined:0.4.0`, ciris.refined._
//import $ivy.`is.cir::ciris-squants:0.4.0`, ciris.squants._


// Define Config class for Ciris
case class AWSProfileConfig(
  awsProfile: String, 
  )
case class AWSSQSConfig(
  numQueues: Int, 
  sqsQueueList: List[String]
  )

val awsProfile =
  loadConfig(
    env[String]("AWS_PROFILE") // Reads environment variable AWS_PROFILE
  ) { (awsProfile) =>
    AWSProfileConfig(
      awsProfile = awsProfile
    )
  }
  val sqsQueues = 
  loadConfig(
    AWSSQSConfig(
      numQueues = 1,
      sqsQueueList = List("R-Start-Q")
    )
  )