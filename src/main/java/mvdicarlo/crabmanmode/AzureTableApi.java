package mvdicarlo.crabmanmode;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class AzureTableApi {
    private final HttpClient httpClient;
    private final String sasUrl;
    private final Gson gson;

    public AzureTableApi(String sasUrl, Gson gson) {
        this.httpClient = HttpClient.newHttpClient();
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

    private String sendRequest(HttpRequest request) throws Exception {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body();
        } else {
            throw new Exception("Request failed: " + response.body());
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
        HttpRequest request = createRequestBuilder(url)
                .GET()
                .build();
        String jsonResponse = sendRequest(request);
        UnlockedItemEntity entity = parseJsonResponse(jsonResponse);
        return entity;
    }

    public List<UnlockedItemEntity> listEntities(String query) throws Exception {
        String url = buildUrl("", "$filter=" + query);
        HttpRequest request = createRequestBuilder(url)
                .GET()
                .build();
        String jsonResponse = sendRequest(request);
        return parseJsonListResponse(jsonResponse);
    }

    public List<UnlockedItemEntity> listEntities() throws Exception {
        String url = buildUrl("", "");
        HttpRequest request = createRequestBuilder(url)
                .GET()
                .build();
        String jsonResponse = sendRequest(request);
        return parseJsonListResponse(jsonResponse);
    }

    public void deleteEntity(String partitionKey, String rowKey) throws Exception {
        String url = buildUrl("(PartitionKey='" + partitionKey + "',RowKey='" + rowKey + "')", "");
        HttpRequest request = createRequestBuilder(url)
                .DELETE()
                .header("If-Match", "*")
                .build();
        System.out.println(sendRequest(request));
    }

    public void insertEntity(UnlockedItemEntity entity) throws Exception {
        String url = buildUrl("", "");
        String jsonPayload = gson.toJson(entity.toMap());
        HttpRequest request = createRequestBuilder(url)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .header("Content-Type", "application/json")
                .build();
        System.out.println(sendRequest(request));
    }

    private HttpRequest.Builder createRequestBuilder(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json");
    }
}