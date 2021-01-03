FROM lolhens/sbt-graal:graal-20.3.0-java11 as builder

COPY . .
ARG CI_VERSION=
RUN sbt graalvm-native-image:packageBin
RUN cp target/graalvm-native-image/cadvisor-limiter* cadvisor-limiter

FROM google/cadvisor

COPY --from=builder /root/cadvisor-limiter /.

COPY ["entrypoint.sh", "/entrypoint.sh"]
ENTRYPOINT ["/entrypoint.sh"]
