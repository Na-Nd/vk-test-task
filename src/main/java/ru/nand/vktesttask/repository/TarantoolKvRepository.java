package ru.nand.vktesttask.repository;

import ru.nand.vktesttask.domain.KvEntry;
import ru.nand.vktesttask.exception.TarantoolOperationException;
import io.tarantool.client.box.TarantoolBoxClient;
import io.tarantool.mapping.TarantoolResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Repository
public class TarantoolKvRepository implements KvRepository {

    private static final Logger log = LoggerFactory.getLogger(TarantoolKvRepository.class);
    private static final int BATCH_SIZE = 1_000;

    private final TarantoolBoxClient client;

    public TarantoolKvRepository(TarantoolBoxClient client) {
        this.client = client;
    }

    @Override
    public void put(String key, @Nullable byte[] value) {
        try {
            String luaKey   = toLuaString(key);
            String luaValue = toLuaBytes(value);
            client.eval("box.space.KV:replace({" + luaKey + ", " + luaValue + "})").join();
            log.debug("Put: key={}, valueIsNull={}", key, value == null);
        } catch (TarantoolOperationException e) {
            throw e;
        } catch (Exception e) {
            throw new TarantoolOperationException("Put() failed for key: " + key, e);
        }
    }

    @Override
    public Optional<KvEntry> get(String key) {
        try {
            String luaKey = toLuaString(key);
            TarantoolResponse<List<?>> response = client.eval(
                    "local t = box.space.KV:get({" + luaKey + "}); " +
                            "if t == nil then return nil, nil, false end; " +
                            "return t[1], t[2], true"
            ).join();

            List<?> result = response.get();
            if (result == null || result.size() < 3 || !Boolean.TRUE.equals(result.get(2))) {
                log.debug("Get: key={} not found", key);
                return Optional.empty();
            }

            byte[] value = toBytes(result.get(1));
            log.debug("Get: key={}, valueIsNull={}", key, value == null);
            return Optional.of(new KvEntry(key, value));
        } catch (TarantoolOperationException e) {
            throw e;
        } catch (Exception e) {
            throw new TarantoolOperationException("Get() failed for key: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            client.eval("box.space.KV:delete({" + toLuaString(key) + "})").join();
            log.debug("Delete: key={}", key);
        } catch (TarantoolOperationException e) {
            throw e;
        } catch (Exception e) {
            throw new TarantoolOperationException("Delete() failed for key: " + key, e);
        }
    }

    @Override
    public void range(String keySince, String keyTo, Consumer<KvEntry> consumer) {
        if (keySince.compareTo(keyTo) > 0) {
            log.debug("Range: keySince > keyTo, skipping");
            return;
        }
        try {
            String  cursor     = keySince;
            boolean firstBatch = true;
            long    total      = 0;

            while (true) {
                String iter    = firstBatch ? "GE" : "GT";
                String luaFrom = toLuaString(cursor);
                String luaTo   = toLuaString(keyTo);
                firstBatch = false;

                TarantoolResponse<List<?>> response = client.eval(
                        "local res = {} " +
                                "for _, t in box.space.KV.index.primary:pairs(" +
                                "  {" + luaFrom + "}, {iterator='" + iter + "'}" +
                                ") do " +
                                "  if t[1] > " + luaTo + " then break end " +
                                "  table.insert(res, {t[1], t[2]}) " +
                                "  if #res >= " + BATCH_SIZE + " then break end " +
                                "end " +
                                "return res"
                ).join();

                List<?> batch = response.get();
                if (batch == null || batch.isEmpty()) break;

                String lastKey = null;
                for (Object rawEntry : batch) {
                    List<?> row = (List<?>) rawEntry;
                    String  k   = (String) row.get(0);
                    byte[]  val = row.size() > 1 ? toBytes(row.get(1)) : null;
                    consumer.accept(new KvEntry(k, val));
                    lastKey = k;
                    total++;
                }

                if (batch.size() < BATCH_SIZE || lastKey == null) break;
                cursor = lastKey;
            }
            log.debug("Range: streamed {} entries", total);
        } catch (TarantoolOperationException e) {
            throw e;
        } catch (Exception e) {
            throw new TarantoolOperationException(
                    "range() failed for [" + keySince + ", " + keyTo + "]", e);
        }
    }

    @Override
    public long count() {
        try {
            TarantoolResponse<List<?>> response = client.eval("return box.space.KV:count()").join();
            List<?> result = response.get();
            long count = (result != null && !result.isEmpty())
                    ? ((Number) result.getFirst()).longValue() : 0L;
            log.debug("Count={}", count);
            return count;
        } catch (TarantoolOperationException e) {
            throw e;
        } catch (Exception e) {
            throw new TarantoolOperationException("count() failed", e);
        }
    }

    private String toLuaString(String s) {
        return "'" + s.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    private String toLuaBytes(@Nullable byte[] value) {
        if (value == null) return "nil";
        String b64 = Base64.getEncoder().encodeToString(value);
        return "require('varbinary').new(require('digest').base64_decode('" + b64 + "'))";
    }

    @Nullable
    private byte[] toBytes(@Nullable Object raw) {
        if (raw == null) return null;
        if (raw instanceof byte[] bytes) return bytes;
        return raw.toString().getBytes();
    }
}