#!/bin/bash
set -e

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
RELEASE_APK_NAME="NexusAgent-debug.apk"

# 1. Verify APK exists
if [ ! -f "$APK_PATH" ]; then
    echo "Error: APK not found at $APK_PATH"
    echo "Please build the project first using: gradle :app:assembleDebug"
    exit 1
fi

# 2. Read the current version from app/build.gradle.kts or fallback
VERSION_NAME=$(grep -oP 'versionName\s*=\s*"\K[^"]*' app/build.gradle.kts || echo "")
if [ -z "$VERSION_NAME" ]; then
    VERSION_NAME="1.0.0"
fi
TAG="v${VERSION_NAME}-${BUILD_NUMBER:-1}"

echo "Found APK. Preparing release with tag: $TAG"

# Copy and rename the APK for the release attachment
cp "$APK_PATH" "$RELEASE_APK_NAME"

# 3 & 4. Publish Release and Attach APK
if command -v gh &> /dev/null; then
    echo "Using GitHub CLI to create release..."
    # Note: gh CLI requires the directory to be a Git repository, or you must specify --repo
    if git rev-parse --is-inside-work-tree > /dev/null 2>&1; then
        gh release create "$TAG" "$RELEASE_APK_NAME" --title "Nexus Agent $TAG" --notes "Automated release."
    else
        if [ -n "$GITHUB_REPO" ]; then
            gh release create "$TAG" "$RELEASE_APK_NAME" --repo "$GITHUB_REPO" --title "Nexus Agent $TAG" --notes "Automated release."
        else
            echo "Error: Not a git repository and \$GITHUB_REPO is not set."
            echo "Please set GITHUB_REPO (e.g. export GITHUB_REPO=\"owner/repo\") or run 'git init'."
            exit 1
        fi
    fi
else
    echo "GitHub CLI (gh) not found, falling back to curl..."
    if [ -z "$GITHUB_TOKEN" ]; then
        echo "Error: GITHUB_TOKEN environment variable is missing."
        exit 1
    fi
    if [ -z "$GITHUB_REPO" ]; then
        echo "Error: GITHUB_REPO environment variable is missing (e.g. owner/repo)."
        exit 1
    fi

    # Create the release
    CREATE_RESPONSE=$(curl -s -X POST -H "Authorization: token $GITHUB_TOKEN" \
        -H "Accept: application/vnd.github.v3+json" \
        https://api.github.com/repos/$GITHUB_REPO/releases \
        -d "{\"tag_name\":\"$TAG\", \"name\":\"Nexus Agent $TAG\", \"body\":\"Automated release.\", \"draft\":false, \"prerelease\":false}")
    
    # Extract upload URL (strip everything after the '{')
    UPLOAD_URL=$(echo "$CREATE_RESPONSE" | grep -oP '(?<="upload_url": ")[^"]*' | cut -d'{' -f1)
    
    if [ -z "$UPLOAD_URL" ]; then
        echo "Failed to create release. GitHub API Response:"
        echo "$CREATE_RESPONSE"
        exit 1
    fi
    
    echo "Release created successfully. Uploading APK asset..."
    curl -s -X POST -H "Authorization: token $GITHUB_TOKEN" \
        -H "Content-Type: application/vnd.android.package-archive" \
        --data-binary @"$RELEASE_APK_NAME" \
        "${UPLOAD_URL}?name=${RELEASE_APK_NAME}"
        
    echo ""
    echo "Upload complete!"
fi
