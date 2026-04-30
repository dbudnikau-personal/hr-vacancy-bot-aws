#!/usr/bin/env bash
set -euo pipefail

# ---------------------------------------------------------------------------
# deploy.sh — build and deploy to AWS
#
# Webhook registration is handled automatically by CloudFormation Custom Resource.
#
# Prerequisites:
#   - .env file with required variables (see .env.example)
#   - AWS CLI configured with eu-central-1 access
#   - SAM CLI installed
# ---------------------------------------------------------------------------

if [[ ! -f .env ]]; then
  echo "Error: .env file not found. Copy .env.example and fill in the values." >&2
  exit 1
fi

set -a; source .env; set +a

# Use Java 21 (required for AWS Lambda)
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"

echo "Building..."
sam build

echo "Deploying..."
sam deploy --no-confirm-changeset --no-fail-on-empty-changeset \
  --parameter-overrides \
    "SpringDatasourceUrl=${SPRING_DATASOURCE_URL}" \
    "SpringDatasourceUsername=${SPRING_DATASOURCE_USERNAME}" \
    "SpringDatasourcePassword=${SPRING_DATASOURCE_PASSWORD}" \
    "BotToken=${BOT_TOKEN}" \
    "DeepSeekApiKey=${DEEPSEEK_API_KEY}" \
    "AdminChatId=${ADMIN_CHAT_ID}"

echo "Done!"
