package ru.nand.vktesttask.domain;

import javax.annotation.Nullable;

public record KvEntry(String key, byte[] value) {

    public KvEntry(String key, @Nullable byte[] value) {
        this.key = key;
        this.value = value;
    }

    @Override
    @Nullable
    public byte[] value() {
        return value;
    }
}