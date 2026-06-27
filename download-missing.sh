#!/bin/bash
set -e
REPOS=(
  "https://maven.aliyun.com/repository/google"
  "https://maven.aliyun.com/repository/public"
  "https://maven.aliyun.com/repository/central"
  "https://maven.aliyun.com/repository/gradle-plugin"
)
LOCAL_REPO="/workspace/local-maven-repo"
MISSING_FILE="/workspace/missing-deps.txt"

while IFS= read -r line || [[ -n "$line" ]]; do
  group=$(echo "$line" | cut -d: -f1)
  artifact=$(echo "$line" | cut -d: -f2)
  version=$(echo "$line" | cut -d: -f3)
  group_path=${group//./\/}
  dest_dir="$LOCAL_REPO/$group_path/$artifact/$version"
  mkdir -p "$dest_dir"
  pom_name="$artifact-$version.pom"
  jar_name="$artifact-$version.jar"
  
  if [ ! -f "$dest_dir/$pom_name" ] || [ "$(stat -c%s "$dest_dir/$pom_name" 2>/dev/null || echo 0)" -lt 100 ]; then
    downloaded=false
    for repo in "${REPOS[@]}"; do
      url="$repo/$group_path/$artifact/$version/$pom_name"
      if curl -x http://127.0.0.1:18080 -f -L --max-time 30 -s -o "$dest_dir/$pom_name" "$url"; then
        if [ -f "$dest_dir/$pom_name" ] && [ "$(stat -c%s "$dest_dir/$pom_name")" -gt 100 ]; then
          echo "Downloaded POM: $line"
          downloaded=true
          break
        fi
      fi
    done
    if [ "$downloaded" = false ]; then
      echo "Failed POM: $line"
    fi
  fi
  
  if [ ! -f "$dest_dir/$jar_name" ]; then
    for repo in "${REPOS[@]}"; do
      url="$repo/$group_path/$artifact/$version/$jar_name"
      if curl -x http://127.0.0.1:18080 -f -L --max-time 60 -s -o "$dest_dir/$jar_name" "$url"; then
        if [ -f "$dest_dir/$jar_name" ] && [ "$(stat -c%s "$dest_dir/$jar_name")" -gt 100 ]; then
          echo "Downloaded JAR: $line"
          break
        fi
      fi
    done
  fi
done < "$MISSING_FILE"
