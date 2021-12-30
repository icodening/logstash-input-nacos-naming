# logstash-input-nacos-naming
Logstash Plugin for Nacos Naming Service Inputs

# Environment
Gradle: 6.5  
Logstash: 8

# Build
1. gradle clean  
2. gradle gem  
3. mkdir plugin  
4. mv logstash-input-nacos_naming-1.0.0.gem plugin/  
5. zip -r logstash-input-nacos_naming.zip plugin  
6. bin/logstash-plugin install file:///tmp/logstash-input-nacos_naming.zip  
7. bin/logstash-plugin list  
8. bin/logstash --java-execution -f config/logstash.conf (v6)
   bin/logstash -f config/logstash.conf (v7 +)
   
# Config
input {
  nacos_naming {
    namespace => "public"
    serverAddr => "127.0.0.1:8848"
    interval => "30"
  }
}