#!/usr/bin/env groovy
import com.amazonaws.auth.*
import com.amazonaws.client.builder.*
import com.amazonaws.services.ec2.model.*
import com.amazonaws.services.ec2.*

def call(body) {
    def manage = new com.remijouannet.manageNode()

    def job_name = body.get('job_name').toString()
    def ak = body.get('ak').toString()
    def sk = body.get('sk').toString()
    def prefix_name = 'euw2-hy-jenkins-slave-'

    AmazonEC2 ec2 = manage.ec2Client(ak, sk, "fcu.eu-west-2.outscale.com", "eu-west-2")
    
    def instance_id = manage.check_if_instance_exist(ec2, job_name)
    println(instance_id)
    if (instance_id != null) {
        println("slave exists")
        manage.terminate_instance(ec2, instance_id)
        manage.deleteNode(instance_id)
    } else {
        println("slave is not exists")
    }
    
    println('end')
}
