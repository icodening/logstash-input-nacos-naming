package cn.icodening.logstash.plugin.input.nacos.naming;

import co.elastic.logstash.api.Input;
import co.elastic.logstash.api.LogstashPlugin;
import co.elastic.logstash.api.PluginConfigSpec;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

@LogstashPlugin(name = "input_nacos_naming")
public class InputNacosNaming implements Input {
    @Override
    public void start(Consumer<Map<String, Object>> writer) {

    }

    @Override
    public void stop() {

    }

    @Override
    public void awaitStop() throws InterruptedException {

    }

    @Override
    public Collection<PluginConfigSpec<?>> configSchema() {
        return null;
    }

    @Override
    public String getId() {
        return null;
    }
}
