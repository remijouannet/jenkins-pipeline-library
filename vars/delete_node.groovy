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
    def fcu_region = body.get('fcu_region', 'eu-west-2').toString()
    def fcu_endpoint = body.get('fcu_endpoint', "fcu.eu-west-2.outscale.com").toString()

    AmazonEC2 ec2 = manage.ec2Client(ak, sk, fcu_endpoint, fcu_region)
    
    def instance_id = manage.checkIfInstanceExist(ec2, job_name)
    println(instance_id)
    if (instance_id != null) {
        println("slave exists")
        manage.terminateInstance(ec2, instance_id)
        manage.deleteNode(instance_id)
    } else {
        println("slave is not exists")
    }
}
