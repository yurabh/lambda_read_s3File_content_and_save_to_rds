                                        lambda_read_s3File_content_and_save_to_rds
1)About the project.

This is a lambda function that is triggered by an event when a file is placed in a bucket S3 from the aws console.
This function reads a file (txt), extracts data (phone numbers) from it and stores it in the mySql database (rds).
After saving the numbers, a message is sent to the sqs queue.
Services for use in this function like: S3,Rds,Sqs.

2)Start the project locally.

2.1 Required to install the project. 

* Java 11

2.2 It is necessary to create a bucket s3, sqs, rds database, lambda function in the aws console.

When creating a lambda, you need to specify a trigger (bucket), as well as set env variables:
(you can see more details about env variables in the code package settings Class Settings).

Also, to deploy the function, you need to download the jar file from the target folder to the lambda console.
