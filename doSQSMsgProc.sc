#!/usr/bin/env /usr/local/bin/amm
// TODO need to change orintlns to logging
// TODO need to add SQS notification
// TODO need to add shutdown
import ammonite.ops._
import ammonite.ops.ImplicitWd._
import $ivy.`io.circe::circe-core:0.8.0`
import $ivy.`io.circe::circe-generic:0.8.0`
import $ivy.`io.circe::circe-parser:0.8.0`
import $ivy.`io.circe::circe-optics:0.8.0`
import $plugin.$ivy.`org.scalamacros:::paradise:2.1.0`
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._,io.circe.optics._

import scala.util.Try
import scala.util.{Success,Failure}

import $ivy.`com.github.nscala-time::nscala-time:2.16.0`
import com.github.nscala_time.time.Imports._

// import the Ciris configuration 
import $exec.awsConfig
import awsConfig._

//set initial stuff
val mwd = pwd
val myEnv = AppEnvironment.withName(sys.env("APP_ENV"))
// create log file
val todayS = DateTime.now.toLocalDate.toString
val timeS = DateTime.now.toLocalTime.toString
val fileS = s"log-$todayS.log"
val logFile = Path(fileS,root/'data/'Scripts/'Logs)
write(logFile,s"Log for $todayS at $timeS\n\n")

/* 
*** case classes for SQS messages - use optics instead to many problems with structure ***
* //case classes for S3 event
* case class UserIdentity(principalId: String)
* case class RequestParameters(sourceIPAddress: String)
* // these strings cause problems - likely because they dont match teh JSON which arent valid scala
* case class ResponseElements(xAmzRequestId: String, xAmzRequestId2: String)
* case class OwnerIdentity(principalId: String)
* case class Bucket(name: String, ownerIdentity: OwnerIdentity, arn: String)
* case class S3Object(key: String, size: String, eTag: String, sequencer: String)
* case class S3(s3SchemaVersion: String, configurationId: String, bucket: Bucket, s3Object: S3Object)
* case class S3Event(eventVersion: String, eventSource: String, awsRegion: String, 
*                     eventTime: String, eventName: String, userIdentity: UserIdentity,
*                     requestParameters: RequestParameters, responseElements: ResponseElements,
*                     s3: S3)
* case class S3Events(Records: List[S3Event])
*
*** case classes for Message Body not necessary - turns out Body is a string ***
* case class Records(records: List[S3Event]) 
* case class AttributesL(SenderId: String, ApproximateFirstReceiveTimestamp: String, 
*                         ApproximateReceiveCount: String, SentTimestamp: String)
* case class SQSMessage(MessageId: String: String, ReceiptHandle: String, MD5OfBody: String, Body: String, Buffer, Attributes: AttributesL)
* case class SQSMessages(Messages: List[SQSMessage])
*/
// extract relevant config variables
val myProfile = awsProfile match {
    case Right(awsProfile) => awsProfile.awsProfile
    case Left(awsProfile)  => "default"
}

val nMsgs = sqsQueues match {
    case Right(sqsQueues) => sqsQueues.numMessages
    case Left(sqsQueues) => 0
}

val listQueues = sqsQueues match {
    case Right(sqsQueues) => sqsQueues.sqsQueueList
    case Left(sqsQueues) => Nil
}

// case class for QueueURL
case class QueueURL(QueueUrl: String)

// iterate over the queues
for (sQueue <- listQueues) {
    //do a Try on the aws sqs call to getURL
    val qURLT = Try(%%('aws,"sqs","get-queue-url","--queue-name",sQueue))
    val qURLS = qURLT match {
        case Success(qURLT) => qURLT.out.string
        case Failure(qURLT) => qURLT.getMessage
    }
    if (qURLT.isSuccess) {
        write.append(logFile,s"successfully got queue URL with message $qURLS\n")
        // we assume since the Try succeeded the decode of the JSON will give correct value
        val qURL = parser.decode[QueueURL](qURLS).right.get.QueueUrl
        //do a Try on the aws sqs call to get the SQS queue attributes
        val qAttributesT = Try(%%('aws,"sqs","get-queue-attributes","--queue-url",qURL,"--attribute-names","ApproximateNumberOfMessages"))
        val qAttributesS = qAttributesT match {
            case Success(qAttributesT) => qAttributesT.out.string
            case Failure(qAttributesT) => qAttributesT.getMessage
        }
        if (qAttributesT.isSuccess) {
            write.append(logFile,s"successfully got queue attributes with message $qAttributesS\n")
            // Get the attribute as JSON
            val qAttributesJ = parse(qAttributesS).getOrElse(Json.Null)
            // get the approximate number of messages in Queue
            val _numMsgs = JsonPath.root.Attributes.ApproximateNumberOfMessages.string
            val numMsgs = _numMsgs.getOption(qAttributesJ).getOrElse("").toString.toInt
            write.append(logFile,"Number of messages in attributes is " + numMsgs + "-- will loop over one more\n")
            // loop numMsgs because its "Approximate" number of messages & may be less than actual number. 
            // This still will give expected behavior in usual case where there is only one message.
            // That's because even though loop again after delete one message, if queue still empty get Success.
            // If attempt happens too close get a Success but it's empty since message is "in flight". Since copy takes a while likely won't happen
            // 
            0 to numMsgs foreach { _ => {
                val eventT = Try(%%('aws,"sqs","receive-message","--queue-url",qURL,"--attribute-names","All","--message-attribute-names","All"))
                val eventS = eventT match {
                    case Success(eventT) => eventT.out.string
                    case Failure(eventT) => eventT.getMessage
                }
                if(eventT.isSuccess) {
                    write.append(logFile,s"successfully received message $eventS \n")
                    // get the SQS Messages as Json
                    val sqsMessageJ = parse(eventS).getOrElse(Json.Null)
                    //example with index but not the right thing to do here
                    //val _receiptHandle = JsonPath.root.Messages.index(0).ReceiptHandle.string
                    //val receiptHandle: Option[String] = _receiptHandle.getOption(eventJ)
                    // Create List of each Messages as separate Json then iterate over them
                    val _sqsMessages = JsonPath.root.Messages.each.json   //Optics traversal definition
                    val sqsMessages: List[Json] = _sqsMessages.getAll(sqsMessageJ)
                    for (message <- sqsMessages) {
                        // get the receiptHandle - val we'll need it to delete the message later
                        val _receiptHandle = JsonPath.root.ReceiptHandle.string   //Optics traversal definition
                        val receiptHandle = _receiptHandle.getOption(message).getOrElse().toString
                        // the body which contains the S3 event is a string. So have to Hack...
                        val _s3Body = JsonPath.root.Body.string
                        val s3BodySP = _s3Body.getOption(message).getOrElse("").toString
                        // single quotes are malformed Json
                        val s3BodyR = s3BodySP.replace('\'','"')
                        // without carriage returns its not a multi-line string & hence malformed
                        val s3BodyN = s3BodyR.replace(",",",\n")
                        // keyword object is a scala keyword and causes problems
                        val s3BodyO = s3BodyN.replace("object","s3Object")
                        val s3BodyJ = parse(s3BodyO).getOrElse(Json.Null)
                        // get Event Time
                        val _eventTime = JsonPath.root.Records.index(0).eventTime.string
                        val eventTimeS = _eventTime.getOption(s3BodyJ).getOrElse().toString
                        val eventTime : DateTime = DateTime.parse(eventTimeS)
                        val _s3 = JsonPath.root.Records.index(0).s3.json
                        val s3J = _s3.getOption(s3BodyJ).getOrElse(Json.Null)
                        // We're finally ready to get the strings we need - bucket name and key!
                        val _bucketName = JsonPath.root.bucket.name.string
                        val bucketName = _bucketName.getOption(s3J).getOrElse().toString
                        val _s3Key = JsonPath.root.s3Object.key.string
                        val s3Key = _s3Key.getOption(s3J).getOrElse().toString.takeWhile(_ != '/')
                        if (DateTime.now > eventTime + 24.hours) {
                            // event is more than 24 hours old, discard it
                            write.append(logFile,"discarding S3 event from bucket " + bucketName + " key "+ s3Key + " from date " + eventTime.toDate.toString + "\n")
                            val discardEvent = Try(%%('aws,"sqs","delete-message","--queue-url",qURL,"--receipt-handle",receiptHandle))
                            discardEvent match {
                                case Success(discardEvent) => write.append(logFile,"Discard of old message successful\n")
                                case Failure(discardEvent) => write.append(logFile,"Discard of old message failed\n")
                            }
                        } else {
                            // copy files from S3 and process 
                            write.append(logFile,"copying...")
                            // get configuration info for Bucket in message
                            val s3BN = s3BucketEnum(bucketName)
                            val files2Copy = s3CopyConfig(s3BN).right.get.filesList
                            val dirName = s3CopyConfig(s3BN).right.get.dirName
                            val procSc = s3CopyConfig(s3BN).right.get.procScript
                            val snsARN = s3CopyConfig(s3BN).right.get.snsARN
                            // Create target directories
                            val dir2MkI = s"/data/$dirName/DataIn/$s3Key"
                            val dir2MkO = s"/data/$dirName/DataOut/$s3Key"
                            //val failFile = Path(s"/data/$dirName/DataIn/$todayS-failure.txt")
                            mkdir! Path(dir2MkI)
                            mkdir! Path(dir2MkO)
                            // copy files in list to local
                            var failCopy = false
                            for (fileN <- files2Copy) {
                                val copyT = Try(%%('aws,"s3","cp",s"s3://$bucketName/$s3Key/$fileN",dir2MkI))
                                val copyS = copyT match {
                                    case Success(copyT) => copyT.out.string
                                    case Failure(copyT) => copyT.getMessage
                                }
                                if(copyT.isFailure) {
                                    write.append(logFile,s"failed to copy file $fileN with message $copyS \n")
                                    failCopy = true
                                }
                            }
                            // if didnt fail process
                            if (failCopy) {
                                write.append(logFile,"doing Processing\n")
                                val procT = Try(%%(root/'usr/'local/'bin/'amm,procSc,s3Key,fileS))
                                val procS = procT match {
                                    case Success(procT) => procT.out.string
                                    case Failure(procT) => procT.getMessage
                                }
                                if(procT.isFailure) {
                                    write.append(logFile,s"$procSc failed with message $procS \n")
                                    val snsMsg = s"Failed to process $dirName files and didnt shut down instance")
                                        vat snsT = Try(%%('aws,"sns","publish","--topic-arn",snsARN,"--message",snsT))
                                        val snsS = snsT match {
                                            case Success(snsT) => snsT.out.string
                                            case Failure(snsT) => snsT.getMessage
                                        }
                                        if(snsT.isFailure) {
                                            write.append(logFile,s"Failed to to send SNS message with error $snsS\n")
                                        }
                                } else {
                                    write.append(logFile,s"Successfully processed $procSc with message $procS \n")
                                    if (!(myEnv == AppEnvironment.Local)) {
                                        // skip on Local machine
                                        // discard event after successful processing
                                        write.append(logFile,s"discarding S3 event from bucket " + bucketName + " key "+ s3Key + " from date " + eventTime.toDate.toString + "\n")
                                        val discardEvent = Try(%%('aws,"sqs","delete-message","--queue-url",qURL,"--receipt-handle",receiptHandle))
                                        discardEvent match {
                                            case Success(discardEvent) => write.append(logFile,"Post-processing discard successful\n")
                                            case Failure(discardEvent) => write.append(logFile,"Post-processing discard failed\n")
                                        }
                                        // shutdown upon completion but first notify
                                        val snsMsg = s"Successfully processed $dirName files and shutting down instance")
                                        vat snsT = Try(%%('aws,"sns","publish","--topic-arn",snsARN,"--message",snsT))
                                        val snsS = snsT match {
                                            case Success(snsT) => snsT.out.string
                                            case Failure(snsT) => snsT.getMessage
                                        }
                                        if(snsT.isFailure) {
                                            write.append(logFile,s"Failed to to send SNS message with error $snsS\n")
                                        }
                                        val shutD = Try(%%(root/'usr/'bin/'sudo,"shutdown","-h","now"))
                                        val shutS = shutD match {
                                            case Success(shutD) => shutD.out.string
                                            case Failure(shutD) => shutD.getMessage
                                        }
                                        if(shutD.isFailure) {
                                            write.append(logFile,s"Failed to shut down w/ message $shutS \n")
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    write.append(logFile,"failed to get SQS messages with message " + eventS + "\n")
                }
            }
            }
        } else {
            write.append(logFile,"failed to get queue attributes with message " + qAttributesS + "\n")
        }
    } else {
        write.append(logFile,"getting SQS queue URL failed with message " + qURLS + "\n")
    }
}
