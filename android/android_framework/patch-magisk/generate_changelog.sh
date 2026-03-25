#!/bin/sh
set -eu

version="${1:-$(git tag --sort=-creatordate | head -n 1)}"
output="${2:-changelog.md}"
current_ref="${3:-$version}"

previous_tag="$(git tag --sort=-creatordate | grep -Fxv "$version" | head -n 1 || true)"

if [ -n "$previous_tag" ]; then
  range="${previous_tag}..${current_ref}"
  range_label="${previous_tag}..${version}"
else
  range="${current_ref}"
  range_label="initial..${version}"
fi

{
  printf '# %s

' "$version"
  printf '_Commit titles and messages for `%s`._

' "$range_label"
  git log --reverse --no-merges --date=short     --format='## %s%n%n%b%n- Commit: `%h`%n- Author: %an%n- Date: %ad%n'     $range
} > "$output"
