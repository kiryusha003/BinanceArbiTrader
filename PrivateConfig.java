package org.example;

public class PrivateConfig {
    private PrivateConfig() {
    }
    public static final String BASE_URL = "wss://ws-api.binance.com:443/ws-api/v3";

//    public static final String API_KEY = "mzTudmSztLccIBZkx1hkvxywXcMqcBU9SzdiRRYAocVRwJfe7XFaJX3Bt6HgJNm1";
//    public static final String SECRET_KEY = "EmKjruM6r39uTpdCcVpuqPOqc5ZpzVdGqBXpEpZZrZra4ZaZpXAo9xDcQfDjJg5R";
    public static final String API_KEY = "64kJQoOPUWd0OpzvepAJ3Sulw02jfGxqKTE2w36djfP5XyYyvu8NFYtZgyJXVaWO";
    public static final String SECRET_KEY = "wzZBzFR2JYxsxuN538X8Uw8rNqlkBMbSmnzNm3TkJuRqSups5iC576A2ST8pA78r"; // Unnecessary if PRIVATE_KEY_PATH is used
    public static final String PRIVATE_KEY_PATH = ""; // Key must be PKCS#8 standard

    public static final String TESTNET_API_KEY = "";
    public static final String TESTNET_SECRET_KEY = ""; // Unnecessary if TESTNET_PRIVATE_KEY_PATH is used
    public static final String TESTNET_PRIVATE_KEY_PATH = ""; //Key must be PKCS#8 standard
}