package com.example;

import com.amazonaws.auth.AWSStaticCredentialsProvider;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.example.model.PhoneNumber;
import com.example.utils.LambdaUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

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
        S3EventNotification.S3EventNotificationRecord eventNotificationRecord = s3EventNotification.getRecords().get(0);
        if (Objects.nonNull(eventNotificationRecord)) {
            String bucketName = getBucketName(eventNotificationRecord);
            String objectKey = getObjectKey(eventNotificationRecord);
            S3Object s3Object = getS3Object(bucketName, objectKey);
            if (Objects.nonNull(s3Object)) {
                S3ObjectInputStream objectContent = s3Object.getObjectContent();
                try (final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(objectContent, StandardCharsets.UTF_8))) {
                    PhoneNumber phoneNumbers = getPhoneNumbers(bufferedReader);
                    return true;
                } catch (IOException ex) {

                    return false;
                }
            }
        }
        return false;
    }

    private static S3Object getS3Object(String bucketName, String objectKey) {
        return amazonS3.getObject(bucketName, objectKey);
    }

    private static String getBucketName(S3EventNotification.S3EventNotificationRecord eventNotificationRecord) {
        return eventNotificationRecord.getS3().getBucket().getName();
    }

    private static String getObjectKey(S3EventNotification.S3EventNotificationRecord eventNotificationRecord) {
        return eventNotificationRecord.getS3().getObject().getKey();
    }

    private static PhoneNumber getPhoneNumbers(BufferedReader bufferedReader) {
        PhoneNumber phoneNumbers = new PhoneNumber();
        try {
            while (bufferedReader.read() != -1) {
                String line = bufferedReader.readLine();
                if (line.contains("|")) {
                    StringBuilder parsingPhoneNumber = new StringBuilder(line);
                    parsingPhoneNumber.deleteCharAt(parsingPhoneNumber.length() - 1);
                    phoneNumbers.addNumber(Integer.parseInt(parsingPhoneNumber.toString()));
                }
            }
        } catch (IOException | NumberFormatException e) {
            return null;
        }
        return phoneNumbers;
    }
}
