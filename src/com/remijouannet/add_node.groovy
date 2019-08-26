package com.remijouannet.add_node

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
        return true
    } else {
        return false
    }
}

def run_instance(AmazonEC2 ec2, String ami, String subnet, String instance_type, String prefix_name, String zone, String keyname, String job_name, Integer disk_size){
    BlockDeviceMapping deviceMapping = new BlockDeviceMapping()
            .withDeviceName("/dev/xvdb")
            .withEbs(new EbsBlockDevice()
                    .withVolumeType("standard")
                    .withVolumeSize(disk_size)
                    .withDeleteOnTermination(true))

    def userdata = '''#!/bin/bash -x
hostname_tag=$(curl http://169.254.169.254/latest/meta-data/tags/Name)
hostnamectl set-hostname $hostname_tag
mkfs.xfs -f /dev/xvdb
mkdir -p /var/lib/docker
mount /dev/xvdb /var/lib/docker
systemctl start docker
'''
    String userdata_encoded = userdata.bytes.encodeBase64().toString()

    RunInstancesRequest instanceRequest = new RunInstancesRequest()
            .withInstanceType(instance_type)
            .withBlockDeviceMappings(deviceMapping)
            .withImageId(ami)
            .withPlacement(new Placement(zone))
            .withInstanceInitiatedShutdownBehavior(ShutdownBehavior.Terminate)
            .withSubnetId(subnet)
            .withKeyName(keyname)
            .withUserData(userdata_encoded)
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

def main3(){
    def env = binding.build.environment
    def instance_type = env.instance_type
    def disk_size = env.disk_size.toInteger()
    def job_name = env.job_name
    
    def prefix_name = 'euw2-hy-jenkins-slave-'
    
    
    BasicAWSCredentials creds = new BasicAWSCredentials(env.AWS_ACCESS_KEY, env.AWS_SECRET_KEY)
    AwsClientBuilder.EndpointConfiguration endpoint = new AwsClientBuilder.EndpointConfiguration("fcu.eu-west-2.outscale.com", "eu-west-2")
    AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard()
			    .withEndpointConfiguration(endpoint)
			    .withCredentials(new AWSStaticCredentialsProvider(creds))
			    .build()
    
    def ami = find_ami(ec2)
    def subnet = get_current_subnet(ec2, get_current_instance_id())
    def zone = get_current_zone(ec2, get_current_instance_id())
    def keyname = get_current_keyname(ec2, get_current_instance_id())
    
    println("ami : " + ami)
    println("subnet : " + subnet)
    println("zone : " + zone)
    println("job_name : " + job_name)
    
    if (check_if_instance_exist(ec2, job_name)) {
        println("slave already exists")
    } else {
        if (disk_size < 10){
            def slave = run_instance(ec2, ami, subnet, instance_type, prefix_name, zone, keyname, job_name, 10)
            println(slave.instanceId.toString())
            println(slave.privateIpAddress.toString())
            createNode(job_name, slave.instanceId.toString(), slave.privateIpAddress.toString())
        }else{
            def slave = run_instance(ec2, ami, subnet, instance_type, prefix_name, zone, keyname, job_name, disk_size)
            println(slave.instanceId.toString())
            println(slave.privateIpAddress.toString())
            createNode(job_name, slave.instanceId.toString(), slave.privateIpAddress.toString())
        }
    }
}
