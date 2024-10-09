FROM ibm-semeru-runtimes:open-21-jre-jammy

ADD /sonic-server-controller/target/sonic-server-controller.jar /
ENTRYPOINT ["java","-server","-XX:-UseGCOverheadLimit","-XX:+DisableExplicitGC","-XX:SurvivorRatio=1","-XX:LargePageSizeInBytes=128M","-XX:SoftRefLRUPolicyMSPerMB=0","-Djava.security.egd=file:/dev/./urandom","-jar","/sonic-server-controller.jar"]
