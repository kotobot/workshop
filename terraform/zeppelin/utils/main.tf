terraform {
  required_version = ">= 0.9.0"
  backend "s3" {
    bucket = "workshop-lviv"
    key    = "terraform/utils"
    region = "us-west-1"
  }
}

variable "artifacts_bucket" {
  default = "workshop-lviv"
}

variable "artifacts_path" {
  default = "artifacts"
}

resource "aws_s3_bucket_object" "warc-hadoop" {
  bucket = "${var.artifacts_bucket}"
  key = "${var.artifacts_path}/warc-hadoop.jar"
  source = "${path.module}/../../../../warc-hadoop/build/libs/warc-hadoop-0.1.0.jar"
  etag = "${md5(file(format("%s/%s", path.module, "../../../../warc-hadoop/build/libs/warc-hadoop-0.1.0.jar")))}"
}

resource "aws_s3_bucket_object" "language-detector" {
  bucket = "${var.artifacts_bucket}"
  key = "${var.artifacts_path}/language-detector.jar"
  source = "${path.module}/../../../../language-detector/target/language-detector-0.7-SNAPSHOT.jar"
  etag = "${md5(file(format("%s/%s", path.module, "../../../../language-detector/target/language-detector-0.7-SNAPSHOT.jar")))}"
}
