package org.re.agent;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.re.agent.model.ReMetric;
import org.re.agent.mqtt.MqttConf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
public class ReportEngineClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String hostUrl;
    private final OkHttpClient okClient;
    private ObjectMapper objectMapper = new ObjectMapper();

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private void sethostUrl(String hostUrl) {
        if (hostUrl.endsWith("/")) {
            this.hostUrl = hostUrl.substring(0, hostUrl.length() - 1);
        } else {
            this.hostUrl = hostUrl;
        }
    }

    public ReportEngineClient(String hostUrl, OkHttpClient okClient) {
        sethostUrl(hostUrl);
        this.okClient = okClient;
    }

    public ReportEngineClient(String hostUrl) {
        sethostUrl(hostUrl);
        this.okClient = new OkHttpClient.Builder()
                .readTimeout(10, TimeUnit.MINUTES)
                .addInterceptor(chain -> {
                    long start = System.currentTimeMillis();
                    Response response = chain.proceed(chain.request());
                    long duration = System.currentTimeMillis() - start;
                    logger.trace("{} --> in {}s", response.body(), TimeUnit.MILLISECONDS.toSeconds(duration));
                    return response;
                })
                .build();
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public MqttConf getMqttBrokerSettings() {
        try {
            Request request = new Request.Builder()
                    .url(String.format("%s/system/mqttconfig", this.hostUrl))
                    .get()
                    .build();
            Response response = execute(request);
            if (response.isSuccessful()) {
                String jsonContent = response.body().string();
                return MAPPER.readValue(jsonContent, MqttConf.class);
            }
        } catch (IOException ex) {
            logger.error("Exception,", ex);
        }
        return new MqttConf();
    }

    public void addMetricsData(ReMetric metric) {
        try {
            RequestBody body = RequestBody.create(JSON, objectMapper.writeValueAsString(metric));
            Request request = new Request.Builder()
                    .url(String.format("%s/metrics/single", this.hostUrl))
                    .post(body)
                    .build();
            execute(request);
        } catch (JsonProcessingException ex) {
            logger.error("Exception,", ex);
        }
    }

    public void addMetricsData(List<ReMetric> metrics) {
        try {
            logger.trace("Metrics:{}", metrics);
            RequestBody body = RequestBody.create(JSON, objectMapper.writeValueAsString(metrics));
            Request request = new Request.Builder()
                    .url(String.format("%s/metrics/multiple", this.hostUrl))
                    .post(body)
                    .build();
            execute(request);
        } catch (JsonProcessingException ex) {
            logger.error("Exception,", ex);
        }
    }

    public Response execute(Request request) {
        try {
            Response response = okClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                logger.debug("{}, responseBody:{}", response, response.body().string());
            }
            return response;
        } catch (IOException ex) {
            logger.error("Exception,", ex);
        }
        return null;
    }

    public void close() {
        if (okClient != null) {
            okClient.dispatcher().executorService().shutdown();
            okClient.connectionPool().evictAll();
        }
    }

}
