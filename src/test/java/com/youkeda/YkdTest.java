package com.youkeda;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cetide.im.model.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.util.CollectionUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class YkdTest {

    @LocalServerPort
    int randomServerPort;

    @Autowired
    private ObjectMapper mapper;

    public static void error(String msg) {
        System.err.println("<YkdError>" + msg + "</YkdError>");
    }

    @Test
    void contextLoads() throws Exception {

        Result<List<ProductDetail>> result = post("api/productdetail/productId?productId=857297d47e864b448fb45d338c6e0ca7",
                                                  new TypeReference<Result<List<ProductDetail>>>() {
                                                  });

        List<ProductDetail> data = result.getData();
        if (CollectionUtils.isEmpty(data)) {
            error("查询商品详情接口出错！！！");
            return;
        }

    }

    private <T> T post(String url, TypeReference typeReference) throws Exception {
        String baseUrl = "http://localhost:" + randomServerPort + "/" + url;

        URI uri = new URI(baseUrl);
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri).header("Content-Type", "application/json").GET();

        HttpRequest request = builder.build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return (T)mapper.readValue(response.body(), typeReference);
    }

}
