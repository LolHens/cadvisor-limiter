FROM lolhens/sbt-graal:graal-20.3.0-java11 as builder

COPY . .
ARG CI_VERSION=
RUN sbt assembly
RUN cp target/scala-*/cadvisor-limiter*.sh.bat cadvisor-limiter.sh.bat

FROM gcr.io/cadvisor/cadvisor

RUN apk --no-cache add openjdk11 --repository=http://dl-cdn.alpinelinux.org/alpine/edge/community
COPY --from=builder /root/cadvisor-limiter.sh.bat /.

COPY ["entrypoint.sh", "/entrypoint.sh"]
ENTRYPOINT ["/entrypoint.sh"]
