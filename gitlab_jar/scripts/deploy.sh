#!/bin/sh
set -eu

cd "$HOME/java-ci-demo"
test -f app-next.jar

if [ -f app.pid ]; then
    old_pid=$(cat app.pid)
    case "$old_pid" in
        ''|*[!0-9]*) echo "Invalid PID in app.pid"; exit 1 ;;
    esac
    if [ "$old_pid" -gt 1 ] && kill -0 "$old_pid" 2>/dev/null; then
        kill "$old_pid"
        attempt=0
        while kill -0 "$old_pid" 2>/dev/null && [ "$attempt" -lt 10 ]; do
            sleep 1
            attempt=$((attempt + 1))
        done
        if kill -0 "$old_pid" 2>/dev/null; then
            echo "Could not stop the previous Java process"
            exit 1
        fi
    fi
fi

mv app-next.jar app.jar
nohup java -jar app.jar > app.log 2>&1 < /dev/null &
echo "$!" > app.pid

attempt=0
while [ "$attempt" -lt 20 ]; do
    if curl -fsS --max-time 2 http://127.0.0.1:8080/ >/dev/null 2>&1; then
        echo "DEPLOY SUCCESS: http://127.0.0.1:8080/"
        exit 0
    fi
    if ! kill -0 "$(cat app.pid)" 2>/dev/null; then
        echo "Java application exited prematurely"
        cat app.log
        exit 1
    fi
    sleep 1
    attempt=$((attempt + 1))
done

echo "HTTP health check failed"
tail -n 60 app.log
exit 1
