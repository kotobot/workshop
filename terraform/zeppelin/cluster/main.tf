terraform {
  required_version = ">= 0.11.6"
  backend "s3" {
    bucket = "state-000000000000"
    key    = "core/zeppelin/dw/cluster/"
    region = "us-east-1"
  }
}

provider "aws" {
  region     = "us-east-1"
  allowed_account_ids = ["000000000000"]
}

variable "env" { default = "dw" }
variable "cluster_name" { default = "zeppelin" }
variable "master_type" { default = "c4.2xlarge" }
variable "core_count" { default = 10 }
variable "core_volume_type" { default = "standard" }
variable "core_volume_gb" { default = 400 }
variable "core_type" { default = "c4.2xlarge" }
variable "task_count" { default = 10 }
variable "task_type" { default = "c4.2xlarge" }
variable "task_volume_type" { default = "standard" }
variable "task_volume_gb" { default = 100 }
variable "subnet" { default = "subnet-abb1e6ce" }
variable "artifacts_bucket" { default = "gr-dw-statistics" }
variable "artifacts_path" { default = "artifacts" }
variable "logs_bucket" { default = "cc-processing" }
variable "logs_path" { default = "logs/zeppelin" }

module "emr" {
  source = "../../emr/cluster/"

  env = "${var.env}"
  cluster_name = "${var.cluster_name}"
  key_name = "dw_key"

  master_type = "${var.master_type}"

  core_count = "${var.core_count}"
  core_type = "${var.core_type}"
  core_volume_type = "${var.core_volume_type}"
  core_volume_gb = "${var.core_volume_gb}"

  task_count = "${var.task_count}"
  task_type = "${var.task_type}"
  task_volume_type = "${var.task_volume_type}"
  task_volume_gb = "${var.task_volume_gb}"

  subnet = "${var.subnet}"

  artifacts_bucket = "${var.artifacts_bucket}"
  artifacts_path = "${var.artifacts_path}"

  logs_bucket = "${var.logs_bucket}"
  logs_path = "${var.logs_path}"

  emr_config = "emr_config.json"
}

output "cluster_id" {
  value = "${module.emr.cluster_id}"
}