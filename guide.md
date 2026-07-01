mvn spring-boot:run "-Dspring-boot.run.profiles=api"

mvn spring-boot:run "-Dspring-boot.run.profiles=scanner"

mvn spring-boot:run "-Dspring-boot.run.profiles=metadata,sender"






nouveau : 
mvn spring-boot:run "-Dspring-boot.run.profiles=api"
mvn spring-boot:run "-Dspring-boot.run.profiles=scanner"
mvn spring-boot:run "-Dspring-boot.run.profiles=metadata"
mvn spring-boot:run "-Dspring-boot.run.profiles=sender,deleter"