# logstash-input-nacos-naming
Logstash Plugin for Nacos Naming Service Inputs

# Environment
> Gradle: 6.5  
  Logstash: 8

# Build & Run
````shell script
gradle clean
````
````shell script
gradle gem
````
````shell script
mkdir plugin
````
````shell script
mv logstash-input-nacos_naming-1.0.0.gem plugin/
````
````shell script
mv zip -r logstash-input-nacos_naming.zip plugin
````
````shell script
project_path=`pwd`
````
````shell script
${LOGSTASH_HOME}/bin/logstash-plugin install file://$project_path/logstash-input-nacos_naming.zip
````
````shell script
${LOGSTASH_HOME}/bin/logstash-plugin list
````
````shell script
${LOGSTASH_HOME}/bin/logstash -f config/logstash.conf (Logstash version > 7)
````
````shell script
${LOGSTASH_HOME}/bin/logstash --java-execution -f config/logstash.conf (Logstash version < 7)
````

# Config
````yaml
input {
  nacos_naming {
    namespace => "public"
    serverAddr => "127.0.0.1:8848"
    interval => "30"
  }
}
````