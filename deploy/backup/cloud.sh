#!/bin/sh
# Work with copies in the bucket without typing aws-cli options (credentials come from the container env).
#   cloud ls [daily|weekly|monthly]   list copies (newest last)
#   cloud get <file> [dest]           download a copy, prints the local path; then: restore <path>
set -eu

export AWS_ACCESS_KEY_ID="$S3_ACCESS_KEY" AWS_SECRET_ACCESS_KEY="$S3_SECRET_KEY" AWS_DEFAULT_REGION="${S3_REGION:-ru-central1}"
s3() { aws --endpoint-url "$S3_ENDPOINT" "$@"; }

case "${1:-}" in
    ls)
        s3 s3 ls "s3://$S3_BUCKET/${2:-daily}/"
        ;;
    get)
        key=${2:?usage: cloud get <file> [dest]}
        case "$key" in */*) ;; *) key="daily/$key" ;; esac
        dest=${3:-/tmp/$(basename "$key")}
        s3 s3 cp --only-show-errors "s3://$S3_BUCKET/$key" "$dest"
        echo "$dest"
        ;;
    *)
        echo "usage: cloud ls [daily|weekly|monthly] | cloud get <file> [dest]" >&2
        exit 1
        ;;
esac
