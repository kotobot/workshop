terraform {
  required_version = ">= 0.11.6"
  backend "s3" {
    bucket = "state-000000000000"
    key    = "core/zeppelin/dw/security/"
    region = "us-east-1"
  }
}

provider "aws" {
  region     = "us-east-1"
  allowed_account_ids = ["000000000000"]
}

variable "vpc_id" { default = "vpc-50d6d336" }

resource "aws_iam_role" "zeppelin_service_role" {
  name = "core_zeppelin_service_role"

  assume_role_policy = <<EOF
{
  "Version": "2008-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "elasticmapreduce.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF
}

resource "aws_iam_role_policy" "zeppelin_role_policy" {
  name = "core_zeppelin_service_policy"
  role = "${aws_iam_role.zeppelin_service_role.id}"

  policy = "${file("${path.module}/emr_policy.json")}"
}

# IAM Role for EC2 Instance Profile
resource "aws_iam_role" "zeppelin_profile_role" {
  name = "core_zeppelin_profile_role"

  assume_role_policy = <<EOF
{
  "Version": "2008-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "ec2.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF
}

resource "aws_iam_instance_profile" "emr_profile" {
  name  = "core_zeppelin_profile"
  role = "${aws_iam_role.zeppelin_profile_role.name}"
}

resource "aws_iam_role_policy" "iam_emr_profile_policy" {
  name = "core_zeppelin_profile_policy"
  role = "${aws_iam_role.zeppelin_profile_role.id}"
  policy = "${file("${path.module}/ec2_policy.json")}"
}

resource "aws_security_group" "emr_zeppelin_sg" {
  name        = "core_emr_zeppelin_sg"
  description = "Allow all"
  vpc_id      = "${var.vpc_id}"

  ingress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["10.0.0.0/16"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

}
