package com.example.settings;

public class Settings {
    private Settings() {
    }

    private static final String SOURCE_ACCESS_KEY = "SOURCE_ACCESS_KEY";
    private static final String SOURCE_SECRET_KEY = "SOURCE_SECRET_KEY";
    private static final String DB_INSTANCE_IDENTIFIER = "DB_INSTANCE_IDENTIFIER";
    private static final String INSTANCE_CLASS = "INSTANCE_CLASS";
    private static final String ENGINE = "ENGINE";
    private static final String DB_NAME = "DB_NAME";
    private static final String STORAGE_TYPE = "STORAGE_TYPE";
    private static final String USER_NAME = "USER_NAME";
    private static final String PASSWORD = "PASSWORD";
    private static final String RDS_URL = "RDS_URL";

    public static String getStorageType() {
        return getEnvVar(STORAGE_TYPE);
    }

    public static String getDbName() {
        return getEnvVar(DB_NAME);
    }

    public static String getRdsUrl() {
        return getEnvVar(RDS_URL);
    }

    public static String getPassword() {
        return getEnvVar(PASSWORD);
    }

    public static String getUserName() {
        return getEnvVar(USER_NAME);
    }

    public static String getEngine() {
        return getEnvVar(ENGINE);
    }

    public static String getInstanceClass() {
        return getEnvVar(INSTANCE_CLASS);
    }

    public static String getDbInstanceIdentifier() {
        return getEnvVar(DB_INSTANCE_IDENTIFIER);
    }

    public static String getAccessKey() {
        return getEnvVar(SOURCE_ACCESS_KEY);
    }

    public static String getSecretKey() {
        return getEnvVar(SOURCE_SECRET_KEY);
    }

    private static String getEnvVar(String name) {
        return System.getenv(name);
    }
}
