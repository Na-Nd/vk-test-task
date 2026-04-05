package ru.nand.vktesttask.grpc;

import ru.nand.vktesttask.proto.*;
import com.google.protobuf.ByteString;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.nand.vktesttask.domain.KvEntry;
import ru.nand.vktesttask.repository.KvRepository;

import java.util.Optional;

@GrpcService
public class KvGrpcService extends KvServiceGrpc.KvServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(KvGrpcService.class);

    private final KvRepository kvRepository;

    public KvGrpcService(KvRepository kvRepository) {
        this.kvRepository = kvRepository;
    }

    @Override
    public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
        try {
            byte[] value = request.hasValue() ? request.getValue().toByteArray() : null;
            kvRepository.put(request.getKey(), value);
            responseObserver.onNext(PutResponse.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Put failed for key={}", request.getKey(), e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asException());
        }
    }

    @Override
    public void get(GetRequest request, StreamObserver<GetResponse> responseObserver) {
        try {
            Optional<KvEntry> entry = kvRepository.get(request.getKey());
            GetResponse.Builder builder = GetResponse.newBuilder();
            if (entry.isPresent()) {
                builder.setFound(true);
                byte[] val = entry.get().value();
                if (val != null) builder.setValue(ByteString.copyFrom(val));
            } else {
                builder.setFound(false);
            }
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Get failed for key={}", request.getKey(), e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asException());
        }
    }

    @Override
    public void delete(DeleteRequest request, StreamObserver<DeleteResponse> responseObserver) {
        try {
            kvRepository.delete(request.getKey());
            responseObserver.onNext(DeleteResponse.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Delete failed for key={}", request.getKey(), e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asException());
        }
    }

    @Override
    public void range(RangeRequest request, StreamObserver<Entry> responseObserver) {
        try {
            kvRepository.range(request.getKeySince(), request.getKeyTo(), kvEntry -> {
                Entry.Builder builder = Entry.newBuilder().setKey(kvEntry.key());
                byte[] val = kvEntry.value();
                if (val != null) builder.setValue(ByteString.copyFrom(val));
                responseObserver.onNext(builder.build());
            });
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Range failed", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asException());
        }
    }

    @Override
    public void count(CountRequest request, StreamObserver<CountResponse> responseObserver) {
        try {
            long count = kvRepository.count();
            responseObserver.onNext(CountResponse.newBuilder().setCount(count).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Count failed", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asException());
        }
    }
}