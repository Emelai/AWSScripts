import $ivy.`io.circe::circe-core:0.8.0`
import $ivy.`io.circe::circe-generic:0.8.0`
import $ivy.`io.circe::circe-parser:0.8.0`


import $plugin.$ivy.`org.scalamacros:::paradise:2.1.0`

import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

import scala.util.Try

// import the Ciris configuration
import $exec.awsConfig
import awsConfig._

//set working directory
val wd = pwd

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

// iterate over the queues
for (sQueue <- sqsQueues) {
    //do a Try on the aws sqs call to getURL
    val qURLT = Try(%%('aws,"sqs","get-queue-url","--queue-name",sQueue))
    val qURLS = qURLT match {
        case Success(qURLT) => qURLT.out.string
        case Failure(qURLT) => qURLT.getMessage
    }
    if (qURLT.isSuccess) {
        // we assume since the Try succeeded the decode of the JSON will give correct value
        val qURL = parser.decode[QueueURL](qURLT).right.get.QueueUrl
        //do a Try on the aws sqs call to get the SQS message
    } else {
        println(qURLS)
    }
}
}