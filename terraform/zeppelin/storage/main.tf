terraform {
  required_version = ">= 0.9.0"
  backend "s3" {
    bucket = "workshop-lviv"
    key    = "terraform/storage"
    region = "us-west-1"
  }
}

variable "s3_bucket" {
  default = "workshop-lviv"
}

resource "aws_s3_bucket" "storage_bucket" {
  bucket = "${var.s3_bucket}"
  region = "us-west-1"
  acl    = "private"
}
