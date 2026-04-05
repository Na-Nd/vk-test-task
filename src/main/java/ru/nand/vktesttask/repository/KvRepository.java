package ru.nand.vktesttask.repository;


import ru.nand.vktesttask.domain.KvEntry;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Consumer;

public interface KvRepository {

    void put(String key, @Nullable byte[] value);

    Optional<KvEntry> get(String key);

    void delete(String key);

    void range(String keySince, String keyTo, Consumer<KvEntry> consumer);

    long count();
}
