# 编译
{
    mvn verify
    mvn package -Dmaven.test.skip=true

    alias build='mvn package -Dmaven.test.skip=true'
    alias build='mvn package -pl core -am -Dmaven.test.skip=true'
    alias build='mvn install -pl core -am -Dmaven.test.skip=true'

    mvn install:install-file \
        -Dfile=/home/qstesiro/github.com/qstesiro/sonarlint-core/core/target/sonarlint-core-8.13-SNAPSHOT.jar \
        -DgroupId=org.sonarsource.sonarlint.core \
        -DartifactId=sonarlint-core \
        -Dversion=8.13-SNAPSHOT \
        -Dpackaging=jar
}
