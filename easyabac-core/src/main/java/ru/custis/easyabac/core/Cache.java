package ru.custis.easyabac.core;

public interface Cache {
    String get(String key);
    void set(String key, String value);
    void set(String key, long expire, String value);
    void flush();
}
