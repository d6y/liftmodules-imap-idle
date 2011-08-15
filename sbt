java -XX:MaxPermSize=256m -Xmx512M -Xss2M  -XX:+CMSClassUnloadingEnabled -jar `dirname $0`/sbt-launch.jar "$@"

