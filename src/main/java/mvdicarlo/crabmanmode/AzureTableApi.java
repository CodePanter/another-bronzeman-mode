package mvdicarlo.crabmanmode;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AzureTableApi {
    private final OkHttpClient httpClient;
    private final String sasUrl;
    private final Gson gson;

    public AzureTableApi(String sasUrl, Gson gson, OkHttpClient httpClient) {
        this.httpClient = httpClient;
        this.sasUrl = sasUrl;
        this.gson = gson;
    }

    private static class AzureTableResponse {
        List<Map<String, Object>> value;
    }

    private String buildUrl(String path, String queryParams) {
        String baseUrl = sasUrl.split("\\?")[0];
        String existingParams = sasUrl.split("\\?")[1];
        return baseUrl + path + "?" + existingParams + (queryParams.isEmpty() ? "" : "&" + queryParams);
    }

    private String sendRequest(Request request) throws Exception {
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                return response.body().string();
            } else {
                throw new Exception("Request failed: " + response.body().string());
            }
        }
    }

    private List<UnlockedItemEntity> parseJsonListResponse(String jsonResponse) {
        Type type = new TypeToken<AzureTableResponse>() {
        }.getType();
        AzureTableResponse response = gson.fromJson(jsonResponse, type);
        return response.value.stream()
                .map(UnlockedItemEntity::fromMap)
                .collect(Collectors.toList());
    }

    private UnlockedItemEntity parseJsonResponse(String jsonResponse) {
        Map<String, Object> map = gson.fromJson(jsonResponse, new TypeToken<Map<String, Object>>() {
        }.getType());
        return UnlockedItemEntity.fromMap(map);
    }

    public UnlockedItemEntity getEntity(String partitionKey, String rowKey) throws Exception {
        String url = buildUrl("(PartitionKey='" + partitionKey + "',RowKey='" + rowKey + "')", "");
        Request request = createRequestBuilder(url)
                .get()
                .build();
        String jsonResponse = sendRequest(request);
        UnlockedItemEntity entity = parseJsonResponse(jsonResponse);
        return entity;
    }

    public List<UnlockedItemEntity> listEntities(String query) throws Exception {
        String url = buildUrl("", "$filter=" + query);
        Request request = createRequestBuilder(url)
                .get()
                .build();
        String jsonResponse = sendRequest(request);
        return parseJsonListResponse(jsonResponse);
    }

    public List<UnlockedItemEntity> listEntities() throws Exception {
        String url = buildUrl("", "");
        Request request = createRequestBuilder(url)
                .get()
                .build();
        String jsonResponse = sendRequest(request);
        return parseJsonListResponse(jsonResponse);
    }

    public void deleteEntity(String partitionKey, String rowKey) throws Exception {
        String url = buildUrl("(PartitionKey='" + partitionKey + "',RowKey='" + rowKey + "')", "");
        Request request = createRequestBuilder(url)
                .delete()
                .header("If-Match", "*")
                .build();
        sendRequest(request);
    }

    public void insertEntity(UnlockedItemEntity entity) throws Exception {
        String url = buildUrl("", "");
        String jsonPayload = gson.toJson(entity.toMap());
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), jsonPayload);
        Request request = createRequestBuilder(url)
                .post(body)
                .header("Content-Type", "application/json")
                .build();
        sendRequest(request);
    }

    private Request.Builder createRequestBuilder(String url) {
        return new Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "CrabManModePlugin");
    }
}