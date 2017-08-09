import $ivy.`is.cir::ciris-core:0.4.0`, ciris._
import $ivy.`is.cir::ciris-enumeratum:0.4.0`, ciris.enumeratum._
import $ivy.`is.cir::ciris-generic:0.4.0`, ciris.generic._
import $ivy.`is.cir::ciris-refined:0.4.0`, ciris.refined._

import _root_.enumeratum._

// define enumerations
object awsEnumerations {
    sealed abstract class AppEnvironment extends EnumEntry
    object AppEnvironment extends Enum[AppEnvironment] {
        case object Local extends AppEnvironment
        case object Testing extends AppEnvironment
        case object Production extends AppEnvironment

        val values = findValues 
    }

    sealed abstract class S3Bucket extends EnumEntry
    object S3Bucket extends Enum[S3Bucket] {
        case object NBO extends S3Bucket
        case object CRO extends S3Bucket
        case object TESTIT extends S3Bucket
        val values = findValues 
    }
}