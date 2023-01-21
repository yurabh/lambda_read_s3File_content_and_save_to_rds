package com.example;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClientBuilder;
import com.amazonaws.services.rds.model.CreateDBInstanceRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.example.model.PhoneNumber;

import com.example.settings.Settings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

public class S3EventHandler implements RequestHandler<S3EventNotification, Boolean> {
    private static final Logger LOGGER = LogManager.getLogger(S3EventHandler.class);
    private static final AWSCredentials CREDENTIALS = new BasicAWSCredentials(Settings.getAccessKey(), Settings.getSecretKey());
    private static final AmazonS3 amazonS3 = AmazonS3ClientBuilder
            .standard()
            .withCredentials(new AWSStaticCredentialsProvider(CREDENTIALS))
            .withRegion(Regions.US_EAST_1)
            .build();
    private static final AmazonRDS amazonRDS = AmazonRDSClientBuilder
            .standard()
            .withCredentials(new AWSStaticCredentialsProvider(CREDENTIALS))
            .withRegion(Regions.US_EAST_1)
            .build();
    private static final AmazonSQS amazonSQS = AmazonSQSClientBuilder
            .standard()
            .withCredentials(new AWSStaticCredentialsProvider(CREDENTIALS))
            .withRegion(Regions.US_EAST_1)
            .build();
    private static final String LINE_SEPARATOR = "|";
    private static final String MESSAGE_BODY = "phones saved";
    private static final String QUEUE = "phone-number-queue";
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS phone_numbers (id SERIAL PRIMARY KEY, numbers INT(15))";
    private static final String INSERT_INTO_TABLE = "INSERT INTO phone_numbers (numbers) VALUES (?)";

    public Boolean handleRequest(S3EventNotification s3EventNotification, Context context) {
        if (s3EventNotification.getRecords().isEmpty()) {
            LOGGER.info("Finish processing function so seEvent is empty: {}", s3EventNotification.getRecords().size());
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
                    if (Objects.nonNull(phoneNumbers)) {
                        createDbInstance();
                        createTableInDbRds();
                        insertPhoneNumbersInDbRds(phoneNumbers);
                        createQueue();
                        sendMessageToTheQueue();
                        return true;
                    }
                } catch (IOException ex) {
                    LOGGER.error("Failed read content from file: {}", ex.getMessage());
                    return false;
                }
            }
            LOGGER.info("S3 Object is null");
        }
        LOGGER.info("EventNotificationRecord is null");
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

    private static void createQueue() {
        LOGGER.info("Create queue");
        amazonSQS.createQueue(QUEUE);
    }

    private static void sendMessageToTheQueue() {
        GetQueueUrlResult queueUrl = amazonSQS.getQueueUrl(QUEUE);
        LOGGER.info("Try to send message to the queue: {}", queueUrl.getQueueUrl());
        amazonSQS.sendMessage(queueUrl.getQueueUrl(), MESSAGE_BODY);
        LOGGER.info("Message sent to the queue: {}", queueUrl.getQueueUrl());
    }

    private static void createTableInDbRds() {
        try (Connection connection = DriverManager.getConnection(Settings.getRdsUrl(), Settings.getUserName(), Settings.getPassword());
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(CREATE_TABLE);
            LOGGER.info("Created table in db");
        } catch (SQLException e) {
            LOGGER.error("Cannot connect do db or create table statement: {}", e.getMessage());
        }
    }

    private static void insertPhoneNumbersInDbRds(PhoneNumber phoneNumbers) {
        try (Connection connection = DriverManager.getConnection(Settings.getRdsUrl(), Settings.getUserName(), Settings.getPassword());
             PreparedStatement preparedStatement = connection.prepareStatement(INSERT_INTO_TABLE)) {
            for (int i = 0; i < phoneNumbers.getPhoneNumbers().size(); i++) {
                preparedStatement.setInt(1, phoneNumbers.getPhoneNumbers().get(i));
                preparedStatement.executeUpdate();
            }
            LOGGER.info("Phone numbers were inserted into db");
        } catch (SQLException e) {
            LOGGER.error("Cannot insert phone number in mySql database: {}", e.getMessage());
        }
    }

    public static void createDbInstance() {
        LOGGER.info("Form db Instance");
        CreateDBInstanceRequest request = new CreateDBInstanceRequest();
        request.setDBInstanceIdentifier(Settings.getDbInstanceIdentifier());
        request.setDBInstanceClass(Settings.getInstanceClass());
        request.setEngine(Settings.getEngine());
        request.setMultiAZ(false);
        request.setMasterUsername(Settings.getUserName());
        request.setMasterUserPassword(Settings.getPassword());
        request.setDBName(Settings.getDbName());
        request.setStorageType(Settings.getStorageType());
        request.setAllocatedStorage(10);
        try {
            amazonRDS.createDBInstance(request);
            LOGGER.info("Db Instance were created");
        } catch (Exception e) {
            LOGGER.error("Db instance already exists: {}", request.getDBName());
        }
    }

    private static PhoneNumber getPhoneNumbers(BufferedReader bufferedReader) {
        PhoneNumber phoneNumbers = new PhoneNumber();
        LOGGER.info("Start parsing file received from s3 bucket");
        try {
            while (bufferedReader.read() != -1) {
                String line = bufferedReader.readLine();
                if (line.contains(LINE_SEPARATOR)) {
                    StringBuilder parsingPhoneNumber = new StringBuilder(line);
                    parsingPhoneNumber.deleteCharAt(parsingPhoneNumber.length() - 1);
                    phoneNumbers.addNumber(Integer.parseInt(parsingPhoneNumber.toString()));
                }
            }
            LOGGER.info("Finish parsing file received from s3 bucket");
        } catch (IOException | NumberFormatException e) {
            LOGGER.error("Failed parsing file received from s3 bucket: {}", e.getMessage());
            return null;
        }
        return phoneNumbers;
    }
}
