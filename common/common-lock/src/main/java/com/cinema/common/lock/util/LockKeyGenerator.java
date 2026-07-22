package com.cinema.common.lock.util;

import com.cinema.common.lock.constant.LockConstants;

public final class LockKeyGenerator {

    private LockKeyGenerator() {
    }

    public static String generate(String key) {

        return LockConstants.PREFIX + key;

    }

}
