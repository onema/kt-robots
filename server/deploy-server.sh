#!/usr/bin/env bash
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
VPC_ID=$(aws ec2 describe-vpcs --filters Name=isDefault,Values=true --query 'Vpcs[*].VpcId' --output text)
REGION=${1:-us-east-1}
SUBNET_IDS=$(aws ec2 describe-subnets --filter Name=vpc-id,Values=$VPC_ID --query 'Subnets[?MapPublicIpOnLaunch==`true`].SubnetId' --output text | sed 's/\t/,/g')

echo "VPC ID = ${VPC_ID}"
echo "SUBNET IDS = ${SUBNET_IDS}"

aws cloudformation deploy --stack-name kt-robots-server --template-file fargate_cluster.yml --parameter-overrides VpcId=${VPC_ID}  --capabilities CAPABILITY_NAMED_IAM
echo "Building Docker image"
docker build -t ktrobots-server .

echo "Loggin in"
aws ecr get-login-password --region "${REGION}" | docker login --username AWS --password-stdin "${AWS_ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"
echo "Tagging image"
docker tag ktrobots-server:latest "${AWS_ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com/kt-robots-server-repository:latest"

echo "Pushing image"
docker push "${AWS_ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com/kt-robots-server-repository:latest"

aws cloudformation deploy --stack-name kt-robots-server-service --template-file fargate_service_cfn.yml --parameter-overrides VpcId=${VPC_ID} Subnets=${SUBNET_IDS}  --capabilities CAPABILITY_NAMED_IAM

ECS_TASK_ARN=$(aws ecs list-tasks --cluster kt-robots-server-cluster --query 'taskArns[*]' --output text | sed 's/\t/,/g')
