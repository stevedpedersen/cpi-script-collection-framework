package com.sap.it.api

class ITApiFactory {

    static <T> T getService(Class<T> clazz, Object context) {
        return clazz.getInstance()
    }

    static <T> T getApi(Class<T> clazz, Object context) {
        return clazz.getInstance()
    }
}