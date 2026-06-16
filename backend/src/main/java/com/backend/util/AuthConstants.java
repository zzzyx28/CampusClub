package com.backend.util;

public final class AuthConstants {

    public static final String ATTR_USER_ID = "authUserId";
    public static final String ATTR_USER_TYPE = "authUserType";
    public static final String ATTR_USERNAME = "authUsername";

    public static final int USER_TYPE_USER = 1;
    public static final int USER_TYPE_SERVICE = 2;
    public static final int USER_TYPE_ADMIN = 3;

    private AuthConstants() {
    }
}
