### 1. Клонировать репозиторий

```bash
git clone <https://github.com/Na-Nd/vk-test-task>
cd vk-test-task
```

### 2. Запуск

```bash
docker compose up -d --build
```

Порядок запуска:
1. Поднимается контейнер `tarantool`, проходит health-check
2. После успешного health-check стартует контейнер `app` с прилжением

Статус, логи:

```bash
docker compose ps
docker compose logs -f app
```

### 3. Остановка

```bash
docker compose down
```

Удаление вместе с разделами Tarantool:

```bash
docker compose down -v
```

---

## gRPC API

Сервер слушает порт `9090`.

### Proto-контракт

```protobuf
service KvService {
  rpc Put    (PutRequest)    returns (PutResponse);
  rpc Get    (GetRequest)    returns (GetResponse);
  rpc Delete (DeleteRequest) returns (DeleteResponse);
  rpc Range  (RangeRequest)  returns (stream Entry);
  rpc Count  (CountRequest)  returns (CountResponse);
}
```

### Методы

#### Put — сохранить значение

```
Request:
  key   : string
  value : bytes         (опционально, может быть null)

Response: пустой
```

#### Get — получить значение

```
Request:
  key : string

Response:
  found : bool
  value : bytes  (если found будет true и значение не null)
```

#### Delete — удалить ключ

```
Request:
  key : string

Response: пустой
```

#### Range — диапазон ключей

```
Request:
  key_since : string  (включительно)
  key_to    : string  (включительно)

Response (stream):
  key   : string
  value : bytes
```

Возвращает все записи с ключами в диапазоне `[key_since, key_to]` в лексикографическом порядке. Данные отдаются батчами по 1000 записей.

#### Count — количество записей

```
Request: пустой

Response:
  count : int64
```

---