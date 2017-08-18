# AWSScripts
Various Ammonite Scripts for Working with AWS. This is a work in progress, but there is a lot of useful code here which also shows how to use Ammonite/Scala for DevOps automation. One of the key motivations here is to limit the number of languages & frameworks you & your organization need to learn. Ammonite scripts allows for bash-ing out so you can use the very simple & straightforward CLI commands the Cloud vendors provide & avoid the more complex SDKs. Unlike Python, Scala is type safe so you can discover most errors at compile-time. The Ammonite REPL is also a great environment for testing code snippets and figuring out by experimentation how to get things to work. You might want to consider using my [Ensimizer](https://github.com/FourMInfo/Ensimizer) which allows for Scala higlighting & code completion in text editors, since none yet support Ammonite's `.sc` extension (IntelliJ is working on it).

Besides Ammonite, the main frameworks we use are:

* [Circe](https://circe.github.io/circe/) - for parsing the AWS JSON results
* [Ciris](https://cir.is) - for using Scala code for compiled configurations. it makes use of
* [Enumeratum](https://github.com/lloydmeta/enumeratum) - for enumerated types which allow for dynamic configurations

The process implemented in the top level `doSQSMsgProc.sc` script has the following steps:

1. User copies files over to an S3 Bucket to initiate a process. The last file that gets copied over is *success.txt*
1. The copy initiates a Lambda (currently in Python in another project, but we hope to move this to Scala too). The Lambda starts up the EC2 and send the event over to SQS
1. Once the EC2 starts a bash script `procSQSMsg` is initiated by crontab. It sets up environment, moves to proper directory & invokes the Ammonite script.
1. That first tries pulling events from all known queues (in configuration)
1. If an event is found that is < 24 hours old, the script parses the event to get the bucket and the object key. 
1. Knowing this, configuration can tell it what files to copy from S3 to itself. 
1. Knowing the bucket, the configuration also knows what Ammonite script to call to do the processing.
1. when processing is done the results are copied over to the dynamically determined output bucket.
1. Once completed, SNS notification is sent & server shutdown.

The scripts provides error handling & simple logging which helps in figuring out what went wrong. The particular example involves processing some files using an R package. The package is already installed on the server so R scripting is almost trivial.

We use a standard structure for files and directories on the disk which makes using this script for different applications easy:
```
/data - top level
../Scripts - where these scripts are stored& run from
..../Logs - where logs are written
../<application> - top level directory for each application
..../DataIn - where files to be processed are copied from the S3 bucket named something like <application>-In
..../DataOut - where files post-processed are stored & then copied to the bucket named something like <application>-Out
```
To Do 
1. Build Tests
2. Functional version of logging and error handling :)
3. Refactor repetitive code

_________________________________
There are also a bunch of scripts that do partial things and give examples of some alternative approaches. These are in the  subdirectory `PartialScripts`
