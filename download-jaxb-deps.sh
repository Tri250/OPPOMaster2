#!/bin/bash
# 下载所有JAXB相关依赖

set -e
REPO_DIR="/workspace/local-maven-repo"
MAVEN_CENTRAL="https://repo1.maven.org/maven2"

download_pom() {
    local group=$1
    local artifact=$2
    local version=$3
    
    local group_path="${group//.//}"
    local dir="$REPO_DIR/$group_path/$artifact/$version"
    mkdir -p "$dir"
    
    local pom_file="$dir/$artifact-$version.pom"
    if [ ! -f "$pom_file" ] || grep -q "<html" "$pom_file" 2>/dev/null; then
        curl -f -L --max-time 30 -s -o "$pom_file" "$MAVEN_CENTRAL/$group_path/$artifact/$version/$artifact-$version.pom" 2>/dev/null || true
    fi
    echo "POM: $group:$artifact:$version"
}

download_jar() {
    local group=$1
    local artifact=$2
    local version=$3
    
    local group_path="${group//.//}"
    local dir="$REPO_DIR/$group_path/$artifact/$version"
    mkdir -p "$dir"
    
    local base_name="$artifact-$version"
    local pom_file="$dir/$base_name.pom"
    local jar_file="$dir/$base_name.jar"
    
    if [ ! -f "$pom_file" ] || grep -q "<html" "$pom_file" 2>/dev/null; then
        curl -f -L --max-time 30 -s -o "$pom_file" "$MAVEN_CENTRAL/$group_path/$artifact/$version/$base_name.pom" 2>/dev/null || true
    fi
    if [ ! -f "$jar_file" ]; then
        curl -f -L --max-time 60 -s -o "$jar_file" "$MAVEN_CENTRAL/$group_path/$artifact/$version/$base_name.jar" 2>/dev/null || true
    fi
    echo "JAR: $group:$artifact:$version"
}

# JAXB Parent POMs
download_pom "jakarta.xml.bind" "jakarta.xml.bind-api-parent" "2.3.2"
download_pom "com.sun.xml.bind.mvn" "jaxb-txw-parent" "2.3.2"
download_pom "com.sun.xml.bind.mvn" "jaxb-parent" "2.3.2"
download_pom "com.sun.xml.bind" "jaxb-bom" "2.3.2"
download_pom "com.sun.xml.bind" "jaxb-bom-ext" "2.3.2"
download_pom "org.glassfish.jaxb" "jaxb-bom" "2.3.2"
download_pom "org.glassfish.jaxb" "jaxb-bom-ext" "2.3.2"
download_pom "org.glassfish.jaxb" "jaxb-runtime-parent" "2.3.2"
download_pom "org.glassfish.jaxb" "jaxb-core-parent" "2.3.0.1"
download_pom "org.glassfish.jaxb" "jaxb-txw-parent" "2.3.2"

# JAXB Core
download_jar "org.glassfish.jaxb" "jaxb-core" "2.3.0.1"
download_jar "org.glassfish.jaxb" "jaxb-runtime" "2.3.2"
download_jar "org.glassfish.jaxb" "txw2" "2.3.2"

# istack
download_pom "com.sun.istack" "istack-commons" "3.0.8"
download_pom "com.sun.istack" "istack-commons-parent" "3.0.8"
download_jar "com.sun.istack" "istack-commons-runtime" "3.0.8"

# Jakarta
download_jar "jakarta.xml.bind" "jakarta.xml.bind-api" "2.3.2"
download_pom "jakarta.xml.bind" "jakarta.xml.bind-api-parent" "2.3.2"

# StAX
download_jar "org.jvnet.staxex" "stax-ex" "1.8.1"

# FastInfoset
download_pom "com.sun.xml.fastinfoset" "fastinfoset-project" "1.2.16"
download_jar "com.sun.xml.fastinfoset" "FastInfoset" "1.2.16"

# Activation
download_jar "jakarta.activation" "jakarta.activation-api" "1.2.1"
download_pom "org.eclipse.ee4j" "project" "1.0.2"

echo "=== Done ==="
