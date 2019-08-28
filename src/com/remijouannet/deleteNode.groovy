#!/usr/bin/env groovy

package com.remijouannet

import jenkins.model.*
import hudson.model.*
import hudson.slaves.*
import hudson.plugins.sshslaves.*
import java.util.ArrayList;
import hudson.slaves.EnvironmentVariablesNodeProperty.Entry
import com.amazonaws.auth.*
import com.amazonaws.client.builder.*
import com.amazonaws.services.ec2.model.*
import com.amazonaws.services.ec2.*
import com.amazonaws.waiters.Waiter
import com.amazonaws.waiters.WaiterParameters
import com.amazonaws.waiters.WaiterTimedOutException

def check_if_instance_exist(AmazonEC2 ec2, String job_name){
    DescribeInstancesRequest req = new DescribeInstancesRequest()
            .withFilters(new Filter("tag:job_name", [job_name]))
            .withFilters(new Filter("instance-state-name", ["running"]))
    DescribeInstancesResult res = ec2.describeInstances(req)
    if (res.reservations.size() != 0){
        return res.reservations[0].instances[0].instanceId.toString()
    } else {
        return null
    }
}

def terminate_instance(AmazonEC2 ec2, String instance_id){
    StopInstancesRequest stopInstancesRequest = new StopInstancesRequest()
            .withInstanceIds([instance_id])
            .withForce(true)
    StopInstancesResult stopInstancesResult = ec2.stopInstances(stopInstancesRequest)
    return true
}

def deleteNode(String name){
    Jenkins.instance.removeNode(Jenkins.instance.getNode(name))
}

def main2(){
def env = binding.build.environment
def job_name = env.job_name

print 'authentification ... \n'
BasicAWSCredentials creds = new BasicAWSCredentials(env.AWS_ACCESS_KEY, env.AWS_SECRET_KEY)
AwsClientBuilder.EndpointConfiguration endpoint = new AwsClientBuilder.EndpointConfiguration("fcu.eu-west-2.outscale.com", "eu-west-2")
AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard()
        .withEndpointConfiguration(endpoint)
        .withCredentials(new AWSStaticCredentialsProvider(creds))
        .build()

def instance_id = check_if_instance_exist(ec2, job_name)
println(instance_id)
if (instance_id != null) {
    println("slave exists")
    terminate_instance(ec2, instance_id)
    deleteNode(instance_id)
} else {
    println("slave is not exists")
}

println('end')
}
