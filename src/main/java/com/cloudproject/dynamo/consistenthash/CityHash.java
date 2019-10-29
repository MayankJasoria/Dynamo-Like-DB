package com.cloudproject.dynamo.consistenthash;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Package-private class to provide a
 */
public class CityHash implements HashFunction {

    private MessageDigest instance;

    public CityHash() {
        try {
            instance = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    @Override
    public long hash(String key) {
        // removing any garbage instance
        instance.reset();

        // assigning the new value to be hashed
        instance.update(key.getBytes());

        // hashing the key
        byte[] digest = instance.digest();

        // applying CityHash algorithm to convert result from byte array to long
        long hashVal = 0;
        for (int i = 0; i < 4; i++) {
            hashVal <<= 8;
            hashVal |= ((int) digest[i]) & 0xFF;
        }

        return hashVal;
    }
}
