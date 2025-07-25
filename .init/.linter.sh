#!/bin/bash
cd /home/kavia/workspace/code-generation/snake-eat-grow-81872-81881/snake_game_android_frontend
./gradlew lint
LINT_EXIT_CODE=$?
if [ $LINT_EXIT_CODE -ne 0 ]; then
   exit 1
fi

