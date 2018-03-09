terraform {
  required_version = ">= 0.9.0"
  backend "s3" {
    bucket = "workshop-lviv"
    key    = "terraform/zeppelin"
    region = "us-west-1"
  }
}

data "aws_iam_instance_profile" "profile" {
  name = "core_zeppelin_profile"
}

data "aws_iam_role" "service_role" {
  name = "core_zeppelin_service_role"
}

data "aws_security_group" "zeppelin_sg" {
  name = "core_emr_zeppelin_sg"
}

variable "vpc_id" {
  #set your VPC id here
  default = "vpc-9ce254f9"
}

variable "cluster_size" {
  default = 3
}

variable "master" {
  default = "c4.2xlarge"
}

variable "core" {
  default = "c4.2xlarge"
}

variable "subnet" {
  default = "subnet-5b70ad02"
}

variable "apps" {
  type = "list"
  default = ["Spark", "Zeppelin"]
}

variable "ssh_key" {
  default = "~/.ssh/dev_workshop.pem"
}

resource "aws_emr_cluster" "zeppelin-cluster" {
  name          = "emr-zeppelin-${terraform.workspace}"
  release_label = "emr-5.11.1"
  applications  = "${var.apps}"

  visible_to_all_users = true
  termination_protection = false
  keep_job_flow_alive_when_no_steps = true

  ec2_attributes {
    key_name                          = "dev-workshop" #insert name of your keipair here
    subnet_id                         = "${var.subnet}"
    additional_master_security_groups = "${data.aws_security_group.zeppelin_sg.id}"
    additional_slave_security_groups  = "${data.aws_security_group.zeppelin_sg.id}"
    instance_profile                  = "${data.aws_iam_instance_profile.profile.arn}"
  }

  master_instance_type = "${var.master}"
  core_instance_type   = "${var.core}"
  core_instance_count  = "${var.cluster_size}"

  log_uri = "s3://workshop-lviv/logs/zeppelin/${terraform.workspace}/"

  service_role = "${data.aws_iam_role.service_role.arn}"

  configurations = "emr_config.json"
}

resource "null_resource" "run_emr_socks_proxy" {
  depends_on = ["aws_emr_cluster.zeppelin-cluster"]

  provisioner "local-exec" {
    command = "nohup aws emr socks --cluster-id ${aws_emr_cluster.zeppelin-cluster.id} --key-pair ${var.ssh_key} > /dev/null 2> /dev/null &"
  }
}

resource "null_resource" "open_zeppelin" {
  depends_on = ["null_resource.run_emr_socks_proxy"]

  provisioner "local-exec" {
    command = "sleep 5 && open http://${aws_emr_cluster.zeppelin-cluster.master_public_dns}:8890"
  }
}

output "cluster_id" {
  value = "${aws_emr_cluster.zeppelin-cluster.id}"
}

