package cn.icodening.logstash.plugin.input.nacos.naming;

import co.elastic.logstash.api.*;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.common.Constants;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.ListView;
import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@LogstashPlugin(name = "nacos_naming")
public class NacosNaming implements Input {

    private final Logger logger;

    /**
     * nacos 命名空间
     */
    public static final PluginConfigSpec<String> NAMESPACE =
            PluginConfigSpec.stringSetting("namespace", "public");

    /**
     * nacos 服务地址
     */
    public static final PluginConfigSpec<String> SERVER_ADDR =
            PluginConfigSpec.stringSetting("serverAddr", "127.0.0.1:8848");

    /**
     * 需要订阅的分组
     */
    public static final PluginConfigSpec<String> GROUP =
            PluginConfigSpec.stringSetting("group", Constants.DEFAULT_GROUP);

    /**
     * 全量拉取服务间隔，默认30秒
     */
    public static final PluginConfigSpec<String> GET_ALL_INTERVAL =
            PluginConfigSpec.stringSetting("interval", "30");

    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    private final Set<String> subscribed = new HashSet<>();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ScheduledExecutorService fetchAllServicesExecutor;

    private final String id;

    private final Configuration config;

    private final Context context;

    // all plugins must provide a constructor that accepts String id, Configuration, and Context
    public NacosNaming(String id, Configuration config, Context context) {
        this.id = id;
        this.config = config;
        this.context = context;
        this.logger = context.getLogger(this);
        this.fetchAllServicesExecutor = new ScheduledThreadPoolExecutor(1, r -> {
            Thread thread = new Thread(r);
            thread.setName("nacos-fetch-services-thread");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public void start(Consumer<Map<String, Object>> writer) {
        String namespace = config.get(NAMESPACE);
        String serverAddr = config.get(SERVER_ADDR);
        String group = config.get(GROUP);
        String interval = config.get(GET_ALL_INTERVAL);

        Properties properties = new Properties();
        properties.setProperty("serverAddr", serverAddr);
        properties.setProperty("namespace", namespace);
        try {
            NamingService namingService = NacosFactory.createNamingService(properties);
            long intervalLong = Long.parseLong(interval);
            logger.info("start fetch services task, interval: {}s", intervalLong);
            fetchAllServicesExecutor.scheduleAtFixedRate(() -> {
                try {
                    ListView<String> servicesOfServer = namingService.getServicesOfServer(1, Integer.MAX_VALUE, group);
                    List<ServiceInfo> subscribeServices = namingService.getSubscribeServices();
                    List<String> services = servicesOfServer.getData();
                    logger.debug("get services from nacos: " + services);
                    for (ServiceInfo subscribeService : subscribeServices) {
                        String service = subscribeService.getName();
                        String cacheKey = buildCacheKey(service, group);
                        subscribed.add(cacheKey);
                    }
                    for (String service : services) {
                        String cacheKey = buildCacheKey(service, group);
                        if (subscribed.add(cacheKey)) {
                            namingService.subscribe(service, event -> {
                                if (!(event instanceof NamingEvent)) {
                                    return;
                                }
                                NamingEvent ne = (NamingEvent) event;
                                Map<String, Object> data = new HashMap<>();
                                data.put("namespace", namespace);
                                data.put("service", ne.getServiceName());
                                try {
                                    String instances = OBJECT_MAPPER.writeValueAsString(ne.getInstances());
                                    data.put("instances", instances);
                                    writer.accept(data);
                                } catch (JsonProcessingException e) {
                                    logger.error("json pass instances error !!!", e);
                                }
                            });
                        }
                    }

                } catch (Throwable e) {
                    logger.error("fetch services task error !!!", e);
                }
            }, 0, intervalLong, TimeUnit.SECONDS);
            countDownLatch.await();
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }

    }

    @Override
    public void stop() {
        countDownLatch.countDown();
    }

    @Override
    public void awaitStop() throws InterruptedException {
        countDownLatch.await();
    }

    @Override
    public Collection<PluginConfigSpec<?>> configSchema() {
        return Arrays.asList(NAMESPACE, GROUP, SERVER_ADDR, GET_ALL_INTERVAL);
    }

    @Override
    public String getId() {
        return id;
    }


    private String buildCacheKey(String serviceName, String groupName) {
        return serviceName + "@" + groupName;
    }

}
