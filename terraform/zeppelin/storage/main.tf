terraform {
  required_version = ">= 0.11.6"
  backend "s3" {
    bucket = "state-000000000000"
    key    = "core/zeppelin/dw/storage/"
    region = "us-east-1"
  }
}

provider "aws" {
  region     = "us-east-1"
  allowed_account_ids = ["000000000000"]
}

variable "s3_bucket" {
  default = "cc-processing"
}

resource "aws_s3_bucket" "storage_bucket" {
  bucket = "${var.s3_bucket}"
  region = "us-east-1"
  acl    = "private"
}
