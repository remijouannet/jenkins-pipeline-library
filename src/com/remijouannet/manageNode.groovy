#!/usr/bin/env groovy

package com.remijouannet

import jenkins.model.*
import hudson.model.*
import hudson.slaves.*
import hudson.plugins.sshslaves.*
import hudson.plugins.sshslaves.verifiers.NonVerifyingKeyVerificationStrategy
import hudson.model.Node.*
import java.util.ArrayList
import hudson.slaves.EnvironmentVariablesNodeProperty.Entry
import org.jenkinsci.plugins.scriptsecurity.scripts.*
import com.amazonaws.auth.*
import com.amazonaws.client.builder.*
import com.amazonaws.services.ec2.model.*
import com.amazonaws.services.ec2.*
import com.amazonaws.waiters.Waiter
import com.amazonaws.waiters.WaiterParameters
import com.amazonaws.waiters.WaiterTimedOutException

def get_current_instance_id(){
    def get = new URL("http://169.254.169.254/latest/meta-data/instance-id").getText();
    return get
}

def get_current_subnet(AmazonEC2 ec2, String id){
    DescribeInstancesRequest req = new DescribeInstancesRequest().withInstanceIds([id])
    DescribeInstancesResult res = ec2.describeInstances(req)
    return res.reservations[0].instances[0].subnetId.toString()
}

def get_current_zone(AmazonEC2 ec2, String id){
    DescribeInstancesRequest req = new DescribeInstancesRequest().withInstanceIds([id])
    DescribeInstancesResult res = ec2.describeInstances(req)
    return res.reservations[0].instances[0].placement.availabilityZone.toString()
}

def get_current_keyname(AmazonEC2 ec2, String id){
    DescribeInstancesRequest req = new DescribeInstancesRequest().withInstanceIds([id])
    DescribeInstancesResult res = ec2.describeInstances(req)
    return res.reservations[0].instances[0].keyName.toString()
}

def find_ami(AmazonEC2 ec2){
    DescribeImagesRequest req = new DescribeImagesRequest()
            .withOwners("self")
            .withFilters(new Filter()
                    .withName("name")
                    .withValues(["jenkins-slave-centos7"]))

    DescribeImagesResult res = ec2.describeImages(req)
    return res.images[0].imageId.toString()
}

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

def run_instance(AmazonEC2 ec2, String ami, String subnet, String instance_type, String prefix_name, String zone, String keyname, String job_name, Integer disk_size){
    BlockDeviceMapping deviceMapping = new BlockDeviceMapping()
            .withDeviceName("/dev/xvdb")
            .withEbs(new EbsBlockDevice()
                    .withVolumeType("gp2")
                    .withVolumeSize(disk_size)
                    .withDeleteOnTermination(true))

    RunInstancesRequest instanceRequest = new RunInstancesRequest()
            .withInstanceType(instance_type)
            .withBlockDeviceMappings(deviceMapping)
            .withImageId(ami)
            .withPlacement(new Placement(zone))
            .withInstanceInitiatedShutdownBehavior(ShutdownBehavior.Terminate)
            .withSubnetId(subnet)
            .withKeyName(keyname)
            .withMinCount(1)
            .withMaxCount(1)

    RunInstancesResult runInstancesResult = ec2.runInstances(instanceRequest)
    String instanceid = runInstancesResult.reservation.instances[0].instanceId.toString()
    Waiter waiter = ec2.waiters().instanceRunning();
    try{
        waiter.run(new WaiterParameters<>(new DescribeInstancesRequest().withInstanceIds(instanceid)))
    }
    catch(WaiterTimedOutException e){
        //Failed to transition into desired state even after polling
    }

    CreateTagsRequest tag = new CreateTagsRequest()
            .withResources(instanceid)
            .withTags(new Tag("job_name", job_name))
            .withTags(new Tag("Name", prefix_name + instanceid))
    ec2.createTags(tag)

    return runInstancesResult.reservation.instances[0]
}

def createNode(String job_name, String name,String host){
    launcher = new SSHLauncher(host, 22, 'jenkins-key')
    launcher.setSshHostKeyVerificationStrategy(new NonVerifyingKeyVerificationStrategy())

    DumbSlave dumb1 = new DumbSlave(name,"/var/lib/jenkins", launcher)
    dumb1.setMode(Node.Mode.EXCLUSIVE)
    dumb1.setLabelString(job_name)
    dumb1.setNumExecutors(2)
    dumb1.setRetentionStrategy(RetentionStrategy.INSTANCE)

    Jenkins.instance.addNode(dumb1)
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

def ec2Client(String ak, String sk, String endpoint, String region) {

    BasicAWSCredentials creds = new BasicAWSCredentials(ak, sk)
    AwsClientBuilder.EndpointConfiguration endpointconf = new AwsClientBuilder.EndpointConfiguration(endpoint, region)
    AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard()
            .withEndpointConfiguration(endpointconf)
            .withCredentials(new AWSStaticCredentialsProvider(creds))
            .build()

    return ec2
}

return this;
