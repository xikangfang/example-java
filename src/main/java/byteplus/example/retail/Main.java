package byteplus.example.retail;

import byteplus.retail.sdk.protocol.ByteplusRetail.AckServerImpressionsRequest;
import byteplus.retail.sdk.protocol.ByteplusRetail.DateConfig;
import byteplus.retail.sdk.protocol.ByteplusRetail.ImportErrorsConfig;
import byteplus.retail.sdk.protocol.ByteplusRetail.ImportProductsRequest;
import byteplus.retail.sdk.protocol.ByteplusRetail.ImportProductsResponse;
import byteplus.retail.sdk.protocol.ByteplusRetail.ImportUserEventsRequest;
import byteplus.retail.sdk.protocol.ByteplusRetail.ImportUserEventsResponse;
import byteplus.retail.sdk.protocol.ByteplusRetail.ImportUsersRequest;
import byteplus.retail.sdk.protocol.ByteplusRetail.ImportUsersResponse;
import byteplus.retail.sdk.protocol.ByteplusRetail.ListOperationsRequest;
import byteplus.retail.sdk.protocol.ByteplusRetail.ListOperationsResponse;
import byteplus.retail.sdk.protocol.ByteplusRetail.Operation;
import byteplus.retail.sdk.protocol.ByteplusRetail.PredictRequest;
import byteplus.retail.sdk.protocol.ByteplusRetail.PredictResponse;
import byteplus.retail.sdk.protocol.ByteplusRetail.PredictResult;
import byteplus.retail.sdk.protocol.ByteplusRetail.Product;
import byteplus.retail.sdk.protocol.ByteplusRetail.ProductsInlineSource;
import byteplus.retail.sdk.protocol.ByteplusRetail.ProductsInputConfig;
import byteplus.retail.sdk.protocol.ByteplusRetail.User;
import byteplus.retail.sdk.protocol.ByteplusRetail.UserEvent;
import byteplus.retail.sdk.protocol.ByteplusRetail.UserEventsInlineSource;
import byteplus.retail.sdk.protocol.ByteplusRetail.UserEventsInputConfig;
import byteplus.retail.sdk.protocol.ByteplusRetail.UsersInlineSource;
import byteplus.retail.sdk.protocol.ByteplusRetail.UsersInputConfig;
import byteplus.retail.sdk.protocol.ByteplusRetail.WriteProductsRequest;
import byteplus.retail.sdk.protocol.ByteplusRetail.WriteProductsResponse;
import byteplus.retail.sdk.protocol.ByteplusRetail.WriteUserEventsRequest;
import byteplus.retail.sdk.protocol.ByteplusRetail.WriteUserEventsResponse;
import byteplus.retail.sdk.protocol.ByteplusRetail.WriteUsersRequest;
import byteplus.retail.sdk.protocol.ByteplusRetail.WriteUsersResponse;
import byteplus.sdk.core.BizException;
import byteplus.sdk.core.Option;
import byteplus.sdk.core.Options;
import byteplus.sdk.core.Region;
import byteplus.sdk.retail.RetailClient;
import byteplus.sdk.retail.RetailClientBuilder;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Parser;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static byteplus.sdk.core.Constant.RCF3339;

@Slf4j
public class Main {
    private final static RetailClient client;

    private final static RequestHelper requestHelper;

    private final static ConcurrentHelper concurrentHelper;

    private final static int DEFAULT_RETRY_TIMES = 2;

    private final static Duration DEFAULT_WRITE_TIMEOUT = Duration.ofMillis(800);

    private final static Duration DEFAULT_IMPORT_TIMEOUT = Duration.ofMillis(800);

    private final static Duration DEFAULT_PREDICT_TIMEOUT = Duration.ofMillis(800);

    private final static Duration DEFAULT_ACK_IMPRESSIONS_TIMEOUT = Duration.ofMillis(800);

    private final static Duration DEFAULT_LIST_OPERATIONS_TIMEOUT = Duration.ofMillis(800);

    static {
        client = new RetailClientBuilder()
                .tenant(Constant.TENANT)
                .tenantId(Constant.TENANT_ID)
                .token(Constant.TOKEN)
                .region(Region.OTHER)
                .build();
        requestHelper = new RequestHelper(client);
        concurrentHelper = new ConcurrentHelper(client);
    }

    /**
     * Those examples request server with account named 'retail_demo',
     * The data in the "demo" account is only used for testing
     * and communication between customers.
     * Please don't send your private data by "demo" account.
     * If you need to send your private data,
     * you can change account to yours here: {@link Constant}
     */
    public static void main(String[] args) {
        // Write real-time user data
        writeUsersExample();
        // Write real-time user data concurrently
        concurrentWriteUsersExample();
        // Import daily offline user data
        importUsersExample();
        // Import daily offline user data concurrently
        concurrentImportUsersExample();

        // Write real-time product data
        writeProductsExample();
        // Write real-time product data concurrently
        concurrentWriteProductsExample();
        // Import daily offline product data
        importProductsExample();
        // Concurrent import daily offline product data
        concurrentImportProductsExample();

        // Write real-time user event data
        writeUserEventsExample();
        // Write real-time user event data concurrently
        concurrentWriteUserEventsExample();
        // Import daily offline user event data
        importUserEventsExample();
        // Concurrent import daily offline user event data
        concurrentImportUserEventsExample();

        // Lists operations that match the specified filter in the request.
        // It can be used to retrieve the task when losing 'operation.name',
        // or to statistic the execution of the task within the specified range,
        // for example, the total count of successfully imported data.
        // The result of "listOperations" is not real-time.
        // The real-time info should be obtained through "getOperation"
        listOperationsExample();

        // Get recommendation results
        recommendExample();

        try {
            // Pause for 5 seconds until the asynchronous import task completes
            Thread.sleep(5000);
        } catch (InterruptedException ignored) {

        }
        System.exit(0);
    }

    public static void writeUsersExample() {
        // The "WriteXXX" api can transfer max to 100 items at one request
        WriteUsersRequest request = buildWriteUsersRequest(1);
        Options.Filler[] opts = buildOptions(DEFAULT_WRITE_TIMEOUT);
        WriteUsersResponse response;
        try {
            response = requestHelper.doWithRetry(client::writeUsers, request, opts, DEFAULT_RETRY_TIMES);
        } catch (BizException e) {
            log.error("write user occur err, msg:{}", e.getMessage());
            return;
        }
        if (StatusHelper.isWriteSuccess(response.getStatus())) {
            log.info("write user success");
            return;
        }
        log.error("write suer find fail, msg:{} errItems:{}", response.getStatus(), response.getErrorsList());
    }

    private static void concurrentWriteUsersExample() {
        // The "WriteXXX" api can transfer max to 100 items at one request
        WriteUsersRequest request = buildWriteUsersRequest(1);
        Options.Filler[] opts = buildOptions(DEFAULT_WRITE_TIMEOUT);
        try {
            concurrentHelper.submitRequest(request, opts);
        } catch (BizException ignore) {
        }
    }

    public static WriteUsersRequest buildWriteUsersRequest(int count) {
        List<User> users = MockHelper.mockUsers(count);
        return WriteUsersRequest.newBuilder()
                .addAllUsers(users)
                .putExtra("extra_info", "info")
                .build();
    }

    public static void importUsersExample() {
        // The "ImportXXX" api can transfer max to 10k items at one request
        ImportUsersRequest request = buildImportUsersRequest(10);
        Parser<ImportUsersResponse> rspParser = ImportUsersResponse.parser();
        Options.Filler[] opts = buildOptions(DEFAULT_IMPORT_TIMEOUT);
        ImportUsersResponse response;
        try {
            response = requestHelper.doImport(client::importUsers, request, opts, rspParser, DEFAULT_RETRY_TIMES);
        } catch (BizException e) {
            log.error("import user occur err, msg:{}", e.getMessage());
            return;
        }
        if (StatusHelper.isSuccess(response.getStatus())) {
            log.info("import user success");
            return;
        }
        log.error("import user find failure info, msg:{} errSamples:{}",
                response.getStatus(), response.getErrorSamplesList());
    }

    public static void concurrentImportUsersExample() {
        // The "ImportXXX" api can transfer max to 10k items at one request
        ImportUsersRequest request = buildImportUsersRequest(10);
        Options.Filler[] opts = buildOptions(DEFAULT_IMPORT_TIMEOUT);
        try {
            concurrentHelper.submitRequest(request, opts);
        } catch (BizException ignore) {
        }
    }

    public static ImportUsersRequest buildImportUsersRequest(int count) {
        UsersInlineSource inlineSource = UsersInlineSource.newBuilder()
                .addAllUsers(MockHelper.mockUsers(count))
                .build();
        UsersInputConfig inputConfig = UsersInputConfig.newBuilder()
                .setUsersInlineSource(inlineSource)
                .build();

        DateConfig dateConfig = DateConfig.newBuilder()
                .setDate(ZonedDateTime.now().format(RCF3339))
                .setIsEnd(false)
                .build();

        ImportErrorsConfig errorsConfig = ImportErrorsConfig.newBuilder()
                .build();

        return ImportUsersRequest.newBuilder()
                .setInputConfig(inputConfig)
                .setDateConfig(dateConfig)
                .setErrorsConfig(errorsConfig)
                .build();
    }

    public static void writeProductsExample() {
        // The "WriteXXX" api can transfer max to 100 items at one request
        WriteProductsRequest request = buildWriteProductsRequest(1);
        Options.Filler[] options = buildOptions(DEFAULT_WRITE_TIMEOUT);
        WriteProductsResponse response;
        try {
            response = requestHelper.doWithRetry(client::writeProducts, request, options, DEFAULT_RETRY_TIMES);
        } catch (BizException e) {
            log.error("write product occur err, msg:{}", e.getMessage());
            return;
        }
        if (StatusHelper.isWriteSuccess(response.getStatus())) {
            log.info("write product success");
            return;
        }
        log.error("write product find failure info, msg:{} errItems:{}",
                response.getStatus(), response.getErrorsList());
    }

    private static void concurrentWriteProductsExample() {
        // The "WriteXXX" api can transfer max to 100 items at one request
        WriteProductsRequest request = buildWriteProductsRequest(1);
        Options.Filler[] opts = buildOptions(DEFAULT_WRITE_TIMEOUT);
        try {
            concurrentHelper.submitRequest(request, opts);
        } catch (BizException ignore) {
        }
    }

    public static WriteProductsRequest buildWriteProductsRequest(int count) {
        List<Product> products = MockHelper.mockProducts(count);
        return WriteProductsRequest.newBuilder()
                .addAllProducts(products)
                .putExtra("extra_info", "info")
                .build();
    }

    public static void importProductsExample() {
        // The "ImportXXX" api can transfer max to 10k items at one request
        ImportProductsRequest request = buildImportProductsRequest(10);
        Parser<ImportProductsResponse> rspParser = ImportProductsResponse.parser();
        Options.Filler[] opts = buildOptions(DEFAULT_IMPORT_TIMEOUT);
        ImportProductsResponse response;
        try {
            response = requestHelper.doImport(client::importProducts, request, opts, rspParser, DEFAULT_RETRY_TIMES);
        } catch (BizException e) {
            log.error("import products occur err, msg:{}", e.getMessage());
            return;
        }
        if (StatusHelper.isSuccess(response.getStatus())) {
            log.info("import products success");
            return;
        }
        log.error("import products find failure info, msg:{} errSamples:{}",
                response.getStatus(), response.getErrorSamplesList());
    }

    public static void concurrentImportProductsExample() {
        // The "ImportXXX" api can transfer max to 10k items at one request
        ImportProductsRequest request = buildImportProductsRequest(10);
        Options.Filler[] opts = buildOptions(DEFAULT_IMPORT_TIMEOUT);
        try {
            concurrentHelper.submitRequest(request, opts);
        } catch (BizException ignore) {
        }
    }

    public static ImportProductsRequest buildImportProductsRequest(int count) {
        ProductsInlineSource inlineSource = ProductsInlineSource.newBuilder()
                .addAllProducts(MockHelper.mockProducts(count))
                .build();
        ProductsInputConfig inputConfig = ProductsInputConfig.newBuilder()
                .setProductsInlineSource(inlineSource)
                .build();

        DateConfig dateConfig = DateConfig.newBuilder()
                .setDate(ZonedDateTime.now().format(RCF3339))
                .setIsEnd(false)
                .build();

        ImportErrorsConfig errorsConfig = ImportErrorsConfig.newBuilder()
                .build();

        return ImportProductsRequest.newBuilder()
                .setInputConfig(inputConfig)
                .setDateConfig(dateConfig)
                .setErrorsConfig(errorsConfig)
                .build();
    }

    public static void writeUserEventsExample() {
        // The "WriteXXX" api can transfer max to 100 items at one request
        WriteUserEventsRequest request = buildWriteUserEventsRequest(1);
        Options.Filler[] options = buildOptions(DEFAULT_WRITE_TIMEOUT);
        WriteUserEventsResponse response;
        try {
            response = requestHelper.doWithRetry(client::writeUserEvents, request, options, DEFAULT_RETRY_TIMES);
        } catch (BizException e) {
            log.error("write user events occur err, msg:{}", e.getMessage());
            return;
        }
        if (StatusHelper.isWriteSuccess(response.getStatus())) {
            log.info("write user events success");
            return;
        }
        log.error("write user events find failure info, msg:{} errItems:{}",
                response.getStatus(), response.getErrorsList());
    }

    private static void concurrentWriteUserEventsExample() {
        // The "WriteXXX" api can transfer max to 100 items at one request
        WriteUserEventsRequest request = buildWriteUserEventsRequest(1);
        Options.Filler[] opts = buildOptions(DEFAULT_WRITE_TIMEOUT);
        try {
            concurrentHelper.submitRequest(request, opts);
        } catch (BizException ignore) {
        }
    }

    public static WriteUserEventsRequest buildWriteUserEventsRequest(int count) {
        List<UserEvent> userEvents = MockHelper.mockUserEvents(count);
        return WriteUserEventsRequest.newBuilder()
                .addAllUserEvents(userEvents)
                .putExtra("extra_info", "info")
                .build();
    }

    public static void importUserEventsExample() {
        // The "ImportXXX" api can transfer max to 10k items at one request
        ImportUserEventsRequest request = buildImportUserEventsRequest(10);
        Parser<ImportUserEventsResponse> rspParser = ImportUserEventsResponse.parser();
        Options.Filler[] opts = buildOptions(DEFAULT_IMPORT_TIMEOUT);
        ImportUserEventsResponse response;
        try {
            response = requestHelper.doImport(client::importUserEvents, request, opts, rspParser, DEFAULT_RETRY_TIMES);
        } catch (BizException e) {
            log.error("import user events occur err, msg:{}", e.getMessage());
            return;
        }
        if (StatusHelper.isSuccess(response.getStatus())) {
            log.info("import user events success");
            return;
        }
        log.error("import user events find failure info, msg:{} errSamples:{}",
                response.getStatus(), response.getErrorSamplesList());
    }

    public static void concurrentImportUserEventsExample() {
        // The "ImportXXX" api can transfer max to 10k items at one request
        ImportUserEventsRequest request = buildImportUserEventsRequest(10);
        Options.Filler[] opts = buildOptions(DEFAULT_IMPORT_TIMEOUT);
        try {
            concurrentHelper.submitRequest(request, opts);
        } catch (BizException ignore) {
        }
    }

    public static ImportUserEventsRequest buildImportUserEventsRequest(int count) {
        UserEventsInlineSource inlineSource = UserEventsInlineSource.newBuilder()
                .addAllUserEvents(MockHelper.mockUserEvents(count))
                .build();

        UserEventsInputConfig inputConfig = UserEventsInputConfig.newBuilder()
                .setUserEventsInlineSource(inlineSource)
                .build();

        DateConfig dateConfig = DateConfig.newBuilder()
                .setDate(ZonedDateTime.now().format(RCF3339))
                .setIsEnd(false)
                .build();

        ImportErrorsConfig errorsConfig = ImportErrorsConfig.newBuilder()
                .build();

        return ImportUserEventsRequest.newBuilder()
                .setInputConfig(inputConfig)
                .setDateConfig(dateConfig)
                .setErrorsConfig(errorsConfig)
                .build();
    }

    private static void listOperationsExample() {
        // The "pageToken" is empty when you get the first page
        ListOperationsRequest request = buildListOperationsRequest("");
        Options.Filler[] opts = buildOptions(DEFAULT_LIST_OPERATIONS_TIMEOUT);
        ListOperationsResponse response;
        try {
            response = client.listOperations(request, opts);
        } catch (Exception e) {
            log.error("list operations occur err, msg:{}", e.getMessage());
            return;
        }
        if (!StatusHelper.isSuccess(response.getStatus())) {
            log.error("list operations find failure info, msg:\n{}", response.getStatus());
            return;
        }
        log.info("list operations success");
        parseTaskResponse(response.getOperationsList());
        // When you get the next Page, you need to put the "nextPageToken"
        // returned by this Page into the request of next Page
        ListOperationsRequest nextPageRequest = buildListOperationsRequest(response.getNextPageToken());
        // request next page
    }

    private static ListOperationsRequest buildListOperationsRequest(String pageToken) {
        String filter = "date>=2021-06-15 and worksOn=ImportUsers and done=true";
        return ListOperationsRequest.newBuilder()
                .setFilter(filter)
                .setPageSize(3)
                .setPageToken(pageToken)
                .build();
    }

    private static void parseTaskResponse(List<Operation> operationsList) {
        for (Operation operation : operationsList) {
            if (!operation.getDone()) {
                continue;
            }
            Any responseAny = operation.getResponse();
            String typeUrl = responseAny.getTypeUrl();
            // To ensure compatibility, do not parse response by 'Any.unpack()'
            try {
                if (typeUrl.contains("ImportUsers")) {
                    ImportUsersResponse importUsersRsp;
                    importUsersRsp = ImportUsersResponse.parseFrom(responseAny.getValue());
                    log.info("[ListOperations] ImportUsers rsp:\n{}", importUsersRsp);
                } else if (typeUrl.contains("ImportProducts")) {
                    ImportProductsResponse importProductsRsp;
                    importProductsRsp = ImportProductsResponse.parseFrom(responseAny.getValue());
                    log.info("[ListOperations] ImportProducts rsp:\n{}", importProductsRsp);
                } else if (typeUrl.contains("ImportUserEvents")) {
                    ImportUserEventsResponse importUserEventsRsp;
                    importUserEventsRsp = ImportUserEventsResponse.parseFrom(responseAny.getValue());
                    log.info("[ListOperations] ImportUserEvents rsp:\n{}", importUserEventsRsp);
                } else {
                    log.error("[ListOperations] unexpected task response type:{}", responseAny.getTypeUrl());
                }
            } catch (InvalidProtocolBufferException e) {
                log.error("[ListOperations] parse task response fail, msg:{}", e.getMessage());
            }
        }
    }


    public static void recommendExample() {
        PredictRequest predictRequest = buildPredictRequest();
        Options.Filler[] opts = buildOptions(DEFAULT_PREDICT_TIMEOUT);
        PredictResponse response;
        try {
            response = client.predict(predictRequest, "home", opts);
        } catch (Exception e) {
            log.error("predict occur error, msg:{}", e.getMessage());
            return;
        }
        if (!StatusHelper.isSuccess(response.getStatus())) {
            log.info("predict return failure info, msg:{}", response.getStatus());
            return;
        }
        log.info("predict success");

        List<AckServerImpressionsRequest.AlteredProduct> alteredProducts =
                doSomethingWithPredictResult(response.getValue());

        // The items, which is eventually shown to user,
        // should send back to Bytedance for deduplication
        AckServerImpressionsRequest ackServerImpressionsRequest =
                buildAckServerImpressionsRequest(response.getRequestId(), predictRequest, alteredProducts);
        opts = buildOptions(DEFAULT_ACK_IMPRESSIONS_TIMEOUT);
        try {
            concurrentHelper.submitRequest(ackServerImpressionsRequest, opts);
        } catch (BizException ignore) {
        }
    }

    private static List<AckServerImpressionsRequest.AlteredProduct>
    doSomethingWithPredictResult(PredictResult predictResult) {
        // You can handle recommend results here,
        // such as filter, insert other items, sort again, etc.
        // The list of goods finally displayed to user and the filtered goods
        // should be sent back to bytedance for deduplication
        return conv2AlterProducts(predictResult);
    }

    private static AckServerImpressionsRequest buildAckServerImpressionsRequest(
            String requestId,
            PredictRequest predictRequest,
            List<AckServerImpressionsRequest.AlteredProduct> alteredProducts) {

        return AckServerImpressionsRequest.newBuilder()
                .setPredictRequestId(requestId)
                .setUserId(predictRequest.getUserId())
                .setScene(predictRequest.getScene())
                .addAllAlteredProducts(alteredProducts)
                .build();
    }

    @NotNull
    private static List<AckServerImpressionsRequest.AlteredProduct> conv2AlterProducts(
            PredictResult rspValue) {

        List<PredictResult.ResponseProduct> responseProducts =
                rspValue.getResponseProductsList();
        List<AckServerImpressionsRequest.AlteredProduct> alteredProducts =
                new ArrayList<>(rspValue.getResponseProductsCount());

        for (int i = 0; i < responseProducts.size(); i++) {
            PredictResult.ResponseProduct responseProduct = responseProducts.get(i);
            AckServerImpressionsRequest.AlteredProduct alteredProduct =
                    AckServerImpressionsRequest
                            .AlteredProduct
                            .newBuilder()
                            .setAlteredReason("kept")
                            .setProductId(responseProduct.getProductId())
                            .setRank(i + 1)
                            .build();

            alteredProducts.add(alteredProduct);
        }
        return alteredProducts;
    }

    public static PredictRequest buildPredictRequest() {
        UserEvent.Scene scene = UserEvent.Scene.newBuilder()
                .setSceneName("home")
                .build();

        Product rootProduct = MockHelper.mockProduct();

        UserEvent.Device device = MockHelper.mockDevice();

        PredictRequest.Context context = PredictRequest.Context.newBuilder()
                .setRootProduct(rootProduct)
                .setDevice(device)
                .addAllCandidateProductIds(Arrays.asList("pid1", "pid2"))
                .build();

        return PredictRequest.newBuilder()
                .setUserId("user_id")
                .setSize(20)
                .setScene(scene)
                .setContext(context)
                .putExtra("clear_impression", "true")
                .build();
    }

    private static Options.Filler[] buildOptions(Duration defaultWriteTimeout) {
        // All options are optional
        Map<String, String> headers = Collections.emptyMap();
        return new Options.Filler[]{
                Option.withRequestId(UUID.randomUUID().toString()),
                Option.withTimeout(defaultWriteTimeout),
                Option.withHeaders(headers)
        };
    }
}