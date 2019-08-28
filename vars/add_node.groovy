#!/usr/bin/env groovy
import com.amazonaws.auth.*
import com.amazonaws.client.builder.*
import com.amazonaws.services.ec2.model.*
import com.amazonaws.services.ec2.*
import com.amazonaws.waiters.Waiter
import com.amazonaws.waiters.WaiterParameters
import com.amazonaws.waiters.WaiterTimedOutException

def call(body) {
    def manage = new com.remijouannet.manageNode()

    def instance_type = body.get('instance_type').toString()
    def disk_size = body.get('disk_size').toInteger()
    def job_name = body.get('job_name').toString()
    def ak = body.get('ak').toString()
    def sk = body.get('sk').toString()
    def prefix_name = 'euw2-hy-jenkins-slave-'

    AmazonEC2 ec2 = manage.ec2Client(ak, sk, "fcu.eu-west-2.outscale.com", "eu-west-2")
    
    def ami = manage.find_ami(ec2)
    def subnet = manage.get_current_subnet(ec2, manage.get_current_instance_id())
    def zone = manage.get_current_zone(ec2, manage.get_current_instance_id())
    def keyname = manage.get_current_keyname(ec2, manage.get_current_instance_id())
    
    println("ami : " + ami)
    println("subnet : " + subnet)
    println("zone : " + zone)
    println("job_name : " + job_name)
    
    if (manage.check_if_instance_exist(ec2, job_name)) {
        println("slave already exists")
    } else {
        if (disk_size < 10){
            def slave = manage.run_instance(ec2, ami, subnet, instance_type, prefix_name, zone, keyname, job_name, 10)
            println(slave.instanceId.toString())
            println(slave.privateIpAddress.toString())
            createNode(job_name, slave.instanceId.toString(), slave.privateIpAddress.toString())
        }else{
            def slave = manage.run_instance(ec2, ami, subnet, instance_type, prefix_name, zone, keyname, job_name, disk_size)
            println(slave.instanceId.toString())
            println(slave.privateIpAddress.toString())
            createNode(job_name, slave.instanceId.toString(), slave.privateIpAddress.toString())
        }
    }

}
