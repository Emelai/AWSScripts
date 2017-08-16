import ammonite.ops._
import ammonite.ops.ImplicitWd._
import scala.util.Try
import scala.util.{Success,Failure}

@main
def nboProcess(s3Key:String) ={
    val outS = "/data/NBO/DataOut/" + s3Key + "/nbo.csv"
    val inFile  = "in_file <- '/data/NBO/DataIn/" + s3Key + "/nbo_assets.csv'" 
    val outFile  = "out_file <- '" + outS + "'"
    val libFile = "library('zapnbo')"
    val doR = Try(%%('Rscript,"-e",libFile,"-e",inFile,"-e",outFile,"-e","nbo.code(in_file,out_file)"))
    val doRS = doR match {
        case Success(doR) => doR.out.string
        case Failure(doR) => doR.getMessage
    }
    if(doR.isSuccess) {
        val outS3 = "s3://zapgroup-nbo-out/" + s3Key + "/nbo.csv"
        val copyT = Try(%%('aws,"s3","cp",outS,outS3))
        val copyS = copyT match {
            case Success(copyT) => copyT.out.string
            case Failure(copyT) => copyT.getMessage
        }
        if(copyT.isSuccess) {
            println("We're done")
        } else {
            println("Copy R output failed with message " + copyS)
        }
    } else {
        println("NBO R program failed with message " + doRS)
    }
}