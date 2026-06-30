#!/bin/bash
# Download AGP 8.7.3 and Kotlin 2.0.21 artifacts to a local Maven repository
# Handles POM parsing for transitive dependency resolution

GOOGLE_BASE="https://dl.google.com/dl/android/maven2"
MAVEN_CENTRAL="https://repo1.maven.org/maven2"
GRADLE_PORTAL="https://plugins.gradle.org/m2"
LOCAL_REPO="/workspace/local-maven-repo"
CURL_OPTS="-sL -m 60 --retry 1"

mkdir -p "$LOCAL_REPO"

to_path() {
    local groupId="$1"
    local artifactId="$2"
    local version="$3"
    echo "$(echo "$groupId" | tr '.' '/')/$artifactId/$version"
}

download() {
    local url="$1"
    local dest="$2"
    if [ -f "$dest" ] && [ -s "$dest" ]; then
        echo "[SKIP] $dest already exists"
        return 0
    fi
    echo "[DOWNLOAD] $url -> $dest"
    mkdir -p "$(dirname "$dest")"
    local http_code
    http_code=$(curl $CURL_OPTS -w "%{http_code}" -o "$dest" "$url" 2>/dev/null) || true
    if [ "$http_code" != "200" ] || [ ! -s "$dest" ]; then
        echo "[WARN] Failed (HTTP $http_code): $url"
        rm -f "$dest"
        return 1
    fi
    return 0
}

# Download from multiple repos with fallback
download_with_fallback() {
    local dest="$1"
    shift
    for url in "$@"; do
        if download "$url" "$dest"; then
            return 0
        fi
    done
    return 1
}

download_artifact() {
    local groupId="$1"
    local artifactId="$2"
    local version="$3"
    local has_jar="$4"
    shift 4
    # Remaining args are base URLs to try

    local maven_path=$(to_path "$groupId" "$artifactId" "$version")
    local dest_dir="$LOCAL_REPO/$maven_path"
    local pom_dest="$dest_dir/$artifactId-$version.pom"
    local jar_dest="$dest_dir/$artifactId-$version.jar"

    # Build URL list for POM
    local pom_urls=()
    for base in "$@"; do
        pom_urls+=("$base/$maven_path/$artifactId-$version.pom")
    done

    download_with_fallback "$pom_dest" "${pom_urls[@]}"

    if [ "$has_jar" = "yes" ]; then
        local jar_urls=()
        for base in "$@"; do
            jar_urls+=("$base/$maven_path/$artifactId-$version.jar")
        done
        download_with_fallback "$jar_dest" "${jar_urls[@]}"
    fi
}

echo "=========================================="
echo "Phase 1: Download known AGP artifacts"
echo "=========================================="

# AGP artifacts - try Google Maven first, then Maven Central
# Note: Some original artifacts don't exist; alternatives are included below
AGP_ARTIFACTS=(
    # groupId artifactId version has_jar
    "com.android.application com.android.application.gradle.plugin 8.7.3 no"
    "com.android.tools.build gradle 8.7.3 yes"
    # gradle-core:8.7.3 does not exist (merged into gradle in AGP 8.x)
    "com.android.tools.build builder 8.7.3 yes"
    "com.android.tools.build builder-model 8.7.3 yes"
    # manifest-merger version is 31.7.3, not 8.7.3
    "com.android.tools.build manifest-merger 31.7.3 yes"
    "com.android.tools.build apkzlib 8.7.3 yes"
    "com.android.tools common 31.7.3 yes"
    "com.android.tools annotations 31.7.3 yes"
    "com.android.tools.ddms ddmlib 31.7.3 yes"
    "com.android.tools.layoutlib layoutlib-api 31.7.3 yes"
    "com.android.tools.build gradle-api 8.7.3 yes"
    "com.android.tools.build gradle-settings-api 8.7.3 yes"
    # transform-api:2.0.0 does not exist; correct version is 2.0.0-deprecated-use-gradle-api
    "com.android.tools.build transform-api 2.0.0-deprecated-use-gradle-api yes"
    # apkzlib-java-resources:8.7.3 and datamodel:8.7.3 do not exist in any repo
    "com.android.tools.build aaptcompiler 8.7.3 yes"
)

for artifact in "${AGP_ARTIFACTS[@]}"; do
    read -r groupId artifactId version has_jar <<< "$artifact"
    download_artifact "$groupId" "$artifactId" "$version" "$has_jar" "$GOOGLE_BASE" "$MAVEN_CENTRAL"
done

echo ""
echo "=========================================="
echo "Phase 2: Download known Kotlin artifacts"
echo "=========================================="

# Kotlin artifacts - try Maven Central first, then Gradle Portal
# Note: org.jetbrains.kotlin.compose plugin marker does not exist at any repo
KOTLIN_ARTIFACTS=(
    "org.jetbrains.kotlin.android org.jetbrains.kotlin.android.gradle.plugin 2.0.21 no"
    "org.jetbrains.kotlin kotlin-gradle-plugin 2.0.21 yes"
    "org.jetbrains.kotlin kotlin-stdlib 2.0.21 yes"
    "org.jetbrains.kotlin kotlin-compiler-embeddable 2.0.21 yes"
    "org.jetbrains.kotlin kotlin-reflect 2.0.21 yes"
    # org.jetbrains.kotlin.compose:org.jetbrains.kotlin.compose.gradle.plugin:2.0.21
    # does not exist; use org.jetbrains.kotlin.plugin.compose instead
    "org.jetbrains.kotlin.plugin.compose org.jetbrains.kotlin.plugin.compose.gradle.plugin 2.0.21 no"
    "org.jetbrains.kotlin.plugin.serialization org.jetbrains.kotlin.plugin.serialization.gradle.plugin 2.0.21 no"
    "org.jetbrains.kotlin.plugin.parcelize org.jetbrains.kotlin.plugin.parcelize.gradle.plugin 2.0.21 no"
)

for artifact in "${KOTLIN_ARTIFACTS[@]}"; do
    read -r groupId artifactId version has_jar <<< "$artifact"
    download_artifact "$groupId" "$artifactId" "$version" "$has_jar" "$MAVEN_CENTRAL" "$GRADLE_PORTAL"
done

echo ""
echo "=========================================="
echo "Phase 3: Parse POMs and download transitive dependencies"
echo "=========================================="

DOWNLOADED_FILE="$LOCAL_REPO/.downloaded"
touch "$DOWNLOADED_FILE"

# Mark initial known artifacts as downloaded
for artifact in "${AGP_ARTIFACTS[@]}"; do
    read -r groupId artifactId version has_jar <<< "$artifact"
    echo "$groupId:$artifactId:$version" >> "$DOWNLOADED_FILE"
done
for artifact in "${KOTLIN_ARTIFACTS[@]}"; do
    read -r groupId artifactId version has_jar <<< "$artifact"
    echo "$groupId:$artifactId:$version" >> "$DOWNLOADED_FILE"
done

parse_pom_deps() {
    local pom_file="$1"
    if [ ! -f "$pom_file" ] || [ ! -s "$pom_file" ]; then
        return
    fi

    python3 << PYEOF
import xml.etree.ElementTree as ET
import sys, re

pom_file = "$pom_file"
try:
    tree = ET.parse(pom_file)
    root = tree.getroot()
except:
    sys.exit(0)

ns = ''
if root.tag.startswith('{'):
    ns = root.tag.split('}')[0] + '}'

props = {}
props_elem = root.find(f'{ns}properties')
if props_elem is not None:
    for prop in props_elem:
        tag = prop.tag.replace(ns, '')
        if prop.text:
            props[tag] = prop.text.strip()

parent_version = None
pv = root.find(f'{ns}parent/{ns}version')
if pv is not None and pv.text:
    parent_version = pv.text.strip()
ov = root.find(f'{ns}version')
if ov is not None and ov.text:
    parent_version = ov.text.strip()

def resolve_version(version_str):
    if not version_str:
        return version_str
    version_str = re.sub(r'[\[\]]', '', version_str)
    if ',' in version_str:
        return ''
    if version_str.startswith('\${') and version_str.endswith('}'):
        prop_name = version_str[2:-1]
        if prop_name == 'project.version':
            return parent_version or version_str
        if prop_name in props:
            resolved = props[prop_name]
            if resolved.startswith('\${'):
                return resolve_version(resolved)
            return resolved
        return ''
    return version_str

deps = root.findall(f'.//{ns}dependency')
for dep in deps:
    g = dep.find(f'{ns}groupId')
    a = dep.find(f'{ns}artifactId')
    v = dep.find(f'{ns}version')
    scope = dep.find(f'{ns}scope')
    optional = dep.find(f'{ns}optional')

    if g is None or a is None:
        continue

    group_id = g.text.strip() if g.text else ''
    artifact_id = a.text.strip() if a.text else ''
    version = resolve_version(v.text.strip()) if v is not None and v.text else ''
    scope_val = scope.text.strip() if scope is not None and scope.text else 'compile'
    optional_val = optional.text.strip() if optional is not None and optional.text else 'false'

    if scope_val in ('test', 'provided', 'system', 'runtime') or optional_val == 'true':
        continue

    if version and not version.startswith('\${') and not version.startswith('['):
        print(f"{group_id}:{artifact_id}:{version}")
PYEOF
}

process_dep() {
    local dep_str="$1"
    IFS=':' read -r groupId artifactId version <<< "$dep_str"

    local key="$groupId:$artifactId:$version"
    if grep -qF "$key" "$DOWNLOADED_FILE" 2>/dev/null; then
        return 0
    fi

    echo "[DEP] Processing: $key"
    echo "$key" >> "$DOWNLOADED_FILE"

    local maven_path=$(to_path "$groupId" "$artifactId" "$version")
    local dest_dir="$LOCAL_REPO/$maven_path"
    local pom_dest="$dest_dir/$artifactId-$version.pom"
    local jar_dest="$dest_dir/$artifactId-$version.jar"

    # Determine repos to try based on groupId
    local repos=()
    if [[ "$groupId" == com.android* ]] || [[ "$groupId" == androidx.* ]]; then
        repos=("$GOOGLE_BASE" "$MAVEN_CENTRAL")
    elif [[ "$groupId" == org.jetbrains.kotlin* ]]; then
        repos=("$MAVEN_CENTRAL" "$GRADLE_PORTAL")
    else
        repos=("$MAVEN_CENTRAL" "$GOOGLE_BASE")
    fi

    # Download POM
    local pom_urls=()
    for base in "${repos[@]}"; do
        pom_urls+=("$base/$maven_path/$artifactId-$version.pom")
    done
    download_with_fallback "$pom_dest" "${pom_urls[@]}"

    # Try to download JAR
    if [ ! -f "$jar_dest" ] || [ ! -s "$jar_dest" ]; then
        local jar_urls=()
        for base in "${repos[@]}"; do
            jar_urls+=("$base/$maven_path/$artifactId-$version.jar")
        done
        if ! download_with_fallback "$jar_dest" "${jar_urls[@]}"; then
            echo "[JAR-SKIP] No JAR for $key (POM-only or not found)"
        fi
    fi
}

# Iterative transitive dependency resolution
MAX_ROUNDS=15
for round in $(seq 1 $MAX_ROUNDS); do
    echo ""
    echo "--- Round $round of transitive dependency resolution ---"

    NEW_DEPS=""
    while IFS= read -r -d '' pom_file; do
        deps=$(parse_pom_deps "$pom_file")
        if [ -n "$deps" ]; then
            NEW_DEPS="$NEW_DEPS
$deps"
        fi
    done < <(find "$LOCAL_REPO" -name "*.pom" -size +0c -print0 2>/dev/null)

    NEW_COUNT=0
    if [ -n "$NEW_DEPS" ]; then
        while IFS= read -r dep; do
            [ -z "$dep" ] && continue
            if ! grep -qF "$dep" "$DOWNLOADED_FILE" 2>/dev/null; then
                process_dep "$dep"
                NEW_COUNT=$((NEW_COUNT + 1))
            fi
        done <<< "$(echo "$NEW_DEPS" | sort -u)"
    fi

    echo "Round $round: Found $NEW_COUNT new dependencies"

    if [ "$NEW_COUNT" -eq 0 ]; then
        echo "No new dependencies found. Dependency resolution complete."
        break
    fi
done

echo ""
echo "=========================================="
echo "Download Summary"
echo "=========================================="
echo "Total POM files: $(find "$LOCAL_REPO" -name '*.pom' -size +0c | wc -l)"
echo "Total JAR files: $(find "$LOCAL_REPO" -name '*.jar' -size +0c | wc -l)"
echo "Total unique dependencies tracked: $(sort -u "$DOWNLOADED_FILE" | wc -l)"
echo ""
echo "Done! Local Maven repo is at: $LOCAL_REPO"
