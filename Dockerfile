FROM eclipse-temurin:17-jre

WORKDIR /usrapp/bin

ENV PORT=35000

COPY /target/classes /usrapp/bin/classes
COPY /target/dependency /usrapp/bin/dependency

EXPOSE 35000

CMD ["java","-cp","./classes:./dependency/*","com.eci.arep.web.WebApplication"]
