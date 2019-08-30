#!/usr/bin/env groovy
import com.amazonaws.auth.*
import com.amazonaws.client.builder.*
import com.amazonaws.services.ec2.model.*
import com.amazonaws.services.ec2.*

def call(body) {
    def manage = new com.remijouannet.manageNode()

    def instance_type = body.get('instance_type', 'm4.large').toString()
    def disk_size = body.get('disk_size', '10').toInteger()
    def job_name = body.get('job_name').toString()
    def ak = body.get('ak').toString()
    def sk = body.get('sk').toString()
    def fcu_region = body.get('fcu_region', 'eu-west-2').toString()
    def fcu_endpoint = body.get('fcu_endpoint', "fcu.eu-west-2.outscale.com").toString()
    def prefix_name = body.get('prefix_name', 'euw2-hy-jenkins-slave-').toString()

    AmazonEC2 ec2 = manage.ec2Client(ak, sk, fcu_endpoint, fcu_region)
    
    def ami = manage.findAmi(ec2)
    def instanceid = manage.getCurrentInstanceId()
    def subnet = manage.getCurrentSubnet(ec2, instanceid)
    def zone = manage.getCurrentZone(ec2, instanceid)
    def keyname = manage.getCurrentKeyname(ec2, instanceid)
    
    println("ami : " + ami)
    println("subnet : " + subnet)
    println("zone : " + zone)
    println("job_name : " + job_name)
    
    if (manage.checkIfInstanceExist(ec2, job_name) != null) {
        println("slave already exists")
    } else {
        if (disk_size < 10){
            disk_size = 10
        }
        def node = manage.runInstance(ec2, ami, subnet, instance_type, prefix_name, zone, keyname, job_name, disk_size)

        println(node.instanceId.toString())
        println(node.privateIpAddress.toString())

        manage.createNode(job_name, node.instanceId.toString(), node.privateIpAddress.toString())
    }

}
