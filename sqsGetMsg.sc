import $ivy.`io.circe::circe-core:0.8.0`
import $ivy.`io.circe::circe-generic:0.8.0`
import $ivy.`io.circe::circe-parser:0.8.0`


import $plugin.$ivy.`org.scalamacros:::paradise:2.1.0`

import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

// import the Ciris configuration
import $exec.awsConfig
import awsConfig._

// get Profile
val myProfile = awsProfile match {
    case Right(awsProfile) => awsProfile.awsProfile
    case Left(awsProfile) => "fail"
}
// case class for QueueURL
case class QueueURL(QueueUrl: String)

val nQueues = sqsQueues match {
    case Right(sqsQueues) => sqsQueues.numQueues
    case Left(sqsQueues) => 0
}

val listQueues = sqsQueues match {
    case Right(sqsQueues) => sqsQueues.sqsQueueList
    case Left(sqsQueues) => Nil
}

