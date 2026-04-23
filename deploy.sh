#!/usr/bin/env bash
set -euo pipefail

# ---------------------------------------------------------------------------
# deploy.sh — build and deploy to AWS
#
# Webhook registration is handled automatically by CloudFormation Custom Resource.
#
# Prerequisites:
#   - AWS CLI configured with eu-central-1 access
#   - SAM CLI installed
# ---------------------------------------------------------------------------

# Use Java 21 (required for AWS Lambda)
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"

echo "🔨 Building..."
sam build

echo "🚀 Deploying..."
sam deploy --no-confirm-changeset

echo "✅ Done!"
