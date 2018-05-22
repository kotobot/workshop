variable "env" {}
variable "cluster_name" {}
variable "key_name" {}
variable "emr_config" {}
variable "master_type" {}
variable "core_count" {}
variable "core_volume_type" {}
variable "core_volume_gb" {}
variable "core_type" {}
variable "task_count" {}
variable "task_type" {}
variable "task_volume_type" {}
variable "task_volume_gb" {}
variable "artifacts_bucket" {}
variable "artifacts_path" {}
variable "subnet" {}
variable "logs_bucket" {}
variable "logs_path" {}

data "aws_iam_role" "emr_iam_service_role" {
  name = "core_zeppelin_service_role"
}

data "aws_iam_instance_profile" "core_prod_emr_instance_profile" {
  name = "core_zeppelin_profile"
}

data "aws_security_group" "emr_stats_cluster" {
  name = "core_emr_zeppelin_sg"
}

resource "aws_emr_cluster" "emr-test-cluster" {
  name          = "emr-spark-${terraform.workspace}"
  release_label = "emr-5.13.0"
  applications  = ["Spark", "Zeppelin", "Ganglia"]

  termination_protection = false
  keep_job_flow_alive_when_no_steps = false

  ec2_attributes {
    key_name                          = "${var.key_name}"
    subnet_id                         = "${var.subnet}"
    additional_master_security_groups = "${data.aws_security_group.emr_stats_cluster.id}"
    additional_slave_security_groups  = "${data.aws_security_group.emr_stats_cluster.id}"
    instance_profile                  = "${data.aws_iam_instance_profile.core_prod_emr_instance_profile.arn}"
  }

  instance_group {
    name ="master-instance-group"
    instance_role = "MASTER"
    instance_count = 1
    instance_type = "${var.master_type}"
    ebs_config {
      type = "standard"
      size = 1000
    }
  }

  instance_group {
    name ="core-instance-group"
    instance_role = "CORE"
    instance_count = "${var.core_count}"
    instance_type = "${var.core_type}"
    ebs_config {
      type = "${var.core_volume_type}"
      size = "${var.core_volume_gb}"
    }
  }

  instance_group {
    name ="task-instance-group"
    instance_role = "TASK"
    instance_count = "${var.task_count}"
    instance_type = "${var.task_type}"
    ebs_config {
      type = "${var.task_volume_type}"
      size = "${var.task_volume_gb}"
    }
  }

  log_uri = "s3://${var.logs_bucket}/${var.logs_path}-${terraform.workspace}/"

  service_role = "${data.aws_iam_role.emr_iam_service_role.arn}"

  configurations = "${var.emr_config}"
}

output "cluster_id" {
  value = "${aws_emr_cluster.emr-test-cluster.id}"
}