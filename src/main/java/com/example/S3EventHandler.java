package com.example;

import com.amazonaws.auth.AWSStaticCredentialsProvider;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.example.utils.LambdaUtils;

public class S3EventHandler implements RequestHandler<S3EventNotification, Boolean> {
    private static final AmazonS3 amazonS3 = AmazonS3ClientBuilder
            .standard()
            .withCredentials(new AWSStaticCredentialsProvider(LambdaUtils.CREDENTIALS))
            .withRegion(LambdaUtils.REGION)
            .build();

    public Boolean handleRequest(S3EventNotification s3EventNotification, Context context) {
        if (s3EventNotification.getRecords().isEmpty()) {
            return false;
        }
        S3EventNotification.S3EventNotificationRecord record = s3EventNotification.getRecords().get(0);
        String bucketName = getBucketName(record);
        String objectKey = getObjectKey(record);
//        S3Object amazonS3Object = amazonS3.getObject(bucketName, objectKey);
//        S3ObjectInputStream objectContent = s3ClientObject.getObjectContent();
//        try (final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(objectContent, StandardCharsets.UTF_8))) {
//            PhoneNumber phoneNumbers = getPhoneNumbers(bufferedReader);
//            phoneNumbers.getPhoneNumbers().forEach(System.out::println);
//        } catch (IOException ex) {
//            return false;
//        }
        return null;
    }

    private String getBucketName(S3EventNotification.S3EventNotificationRecord eventNotificationRecord) {
        return eventNotificationRecord.getS3().getBucket().getName();
    }

    private String getObjectKey(S3EventNotification.S3EventNotificationRecord eventNotificationRecord) {
        return eventNotificationRecord.getS3().getObject().getKey();
    }

   /* private static PhoneNumber getPhoneNumbers(BufferedReader bufferedReader) {
        Stream<String> lines = bufferedReader.lines();
        PhoneNumber phoneNumbers = new PhoneNumber();
        phoneNumbers.setPhoneNumbers(lines.map(Integer::parseInt).collect(Collectors.toList()));
        return phoneNumbers;
    }*/
}
