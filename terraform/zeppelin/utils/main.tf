terraform {
  required_version = ">= 0.11.6"
  backend "s3" {
    bucket = "state-000000000000"
    key    = "core/zeppelin/dw/utils/"
    region = "us-east-1"
  }
}

provider "aws" {
  region     = "us-east-1"
  allowed_account_ids = ["000000000000"]
}

variable "artifacts_bucket" {
  default = "cc-processing"
}

variable "artifacts_path" {
  default = "artifacts"
}

resource "aws_s3_bucket_object" "warc-hadoop" {
  bucket = "${var.artifacts_bucket}"
  key = "${var.artifacts_path}/warc-hadoop.jar"
  source = "${path.module}/../../../../warc-hadoop/build/libs/warc-hadoop-0.1.0-gr.jar"
  etag = "${md5(file(format("%s/%s", path.module, "../../../../warc-hadoop/build/libs/warc-hadoop-0.1.0-gr.jar")))}"
}

resource "aws_s3_bucket_object" "language-detector" {
  bucket = "${var.artifacts_bucket}"
  key = "${var.artifacts_path}/language-detector.jar"
  source = "${path.module}/../../../../language-detector/target/language-detector-0.7-SNAPSHOT.jar"
  etag = "${md5(file(format("%s/%s", path.module, "../../../../language-detector/target/language-detector-0.7-SNAPSHOT.jar")))}"
}
