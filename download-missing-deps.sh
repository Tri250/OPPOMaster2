#!/bin/bash
# 下载缺失的依赖到本地 Maven 仓库

set -e
REPO_DIR="/workspace/local-maven-repo"
MAVEN_CENTRAL="https://repo1.maven.org/maven2"
GOOGLE_MAVEN="https://dl.google.com/android/maven2"

download_dep() {
    local group=$1
    local artifact=$2
    local version=$3
    local ext=${4:-jar}
    
    local group_path="${group//.//}"
    local dir="$REPO_DIR/$group_path/$artifact/$version"
    mkdir -p "$dir"
    
    local base_name="$artifact-$version"
    local pom_file="$dir/$base_name.pom"
    local artifact_file="$dir/$base_name.$ext"
    
    # Download POM
    if [ ! -f "$pom_file" ] || grep -q "<html" "$pom_file" 2>/dev/null; then
        # Try Maven Central first
        curl -f -L --max-time 30 -s -o "$pom_file" "$MAVEN_CENTRAL/$group_path/$artifact/$version/$base_name.pom" 2>/dev/null || \
        curl -f -L --max-time 30 -s -o "$pom_file" "$GOOGLE_MAVEN/$group_path/$artifact/$version/$base_name.pom" 2>/dev/null || \
        echo "Failed to download POM: $group:$artifact:$version"
    fi
    
    # Download artifact (if not pom-only)
    if [ "$ext" != "pom" ] && [ ! -f "$artifact_file" ]; then
        curl -f -L --max-time 60 -s -o "$artifact_file" "$MAVEN_CENTRAL/$group_path/$artifact/$version/$base_name.$ext" 2>/dev/null || \
        curl -f -L --max-time 60 -s -o "$artifact_file" "$GOOGLE_MAVEN/$group_path/$artifact/$version/$base_name.$ext" 2>/dev/null || \
        echo "Failed to download $ext: $group:$artifact:$version"
    fi
    
    echo "Downloaded: $group:$artifact:$version"
}

# HTTP Components
download_dep "org.apache.httpcomponents" "httpmime" "4.5.6"
download_dep "org.apache.httpcomponents" "httpclient" "4.5.6"
download_dep "org.apache.httpcomponents" "httpcore" "4.4.10"
download_dep "org.apache.httpcomponents" "httpclient-cache" "4.5.6"

# Commons
download_dep "commons-io" "commons-io" "2.13.0"
download_dep "commons-codec" "commons-codec" "1.10"
download_dep "commons-logging" "commons-logging" "1.2"

# BouncyCastle
download_dep "org.bouncycastle" "bcpkix-jdk18on" "1.77"
download_dep "org.bouncycastle" "bcprov-jdk18on" "1.77"
download_dep "org.bouncycastle" "bcutil-jdk18on" "1.77"

# Jakarta XML Bind
download_dep "jakarta.xml.bind" "jakarta.xml.bind-api" "2.3.2"
download_dep "jakarta.xml.bind" "jakarta.xml.bind-api" "2.3.3"

# Glassfish JAXB
download_dep "org.glassfish.jaxb" "jaxb-runtime" "2.3.2"
download_dep "org.glassfish.jaxb" "txw2" "2.3.2"
download_dep "org.glassfish.jaxb" "jaxb-core" "2.3.0.1"

# istack
download_dep "com.sun.istack" "istack-commons-runtime" "3.0.8"
download_dep "com.sun.istack" "istack-commons-tools" "3.0.8"

# StAX
download_dep "org.jvnet.staxex" "stax-ex" "1.8.1"

# Other commonly needed
download_dep "com.sun.xml.bind" "jaxb-impl" "2.3.2"
download_dep "com.sun.xml.bind" "jaxb-core" "2.3.0.1"
download_dep "com.sun.xml.bind" "jaxb-xjc" "2.3.2"

echo "=== Done downloading missing dependencies ==="
