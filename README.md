# AWSScripts
Various Ammonite Scripts for Working with AWS. This is a work in progress and incomplete, but there is still a lot of useful code here which shows.

Besides Ammonite, the main frameworks we use are:

* Circe - for parsing the AWS JSON
* Ciris - for using scala for compiled configurations

The main process we are trying to implement at this stage has the following steps:

1. User copies files over to an S3 Bucket to initiate a process. The last file that gets copied over is *success.txt*
1. The copy initiates a Lambda (currently in Python in another project, but we hope to move this to Scala too). The Lambda starts up the EC2 and send the event over to SQS
1. Once the EC2 starts an Ammonite script is initiated.
1. First it tries pulling events from all known queues (in configuration)
1. If an event is found, the script parses the event to get the bucket and the object key. Knowing this it can copy files from S3 to itself. Knowing the bucket it also knows what processing needs to take place.


Currently there are a bunch of scripts that do part of these things and give examples of some alternative approaches. These will eventually be moved to a subdirectory.