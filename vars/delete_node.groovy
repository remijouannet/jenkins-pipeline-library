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

    echo "delete_node: Authentification"

    AmazonEC2 ec2 = manage.ec2Client(ak, sk, fcu_endpoint, fcu_region)
    
    def instance = manage.checkIfInstanceExist(ec2, job_name)

    if (instance != null) {
        echo "delete_node: Node already exists -> " + instance

        manage.terminateInstance(ec2, instance)
        echo "delete_node: the instance is now terminate -> " + instance

        manage.deleteNode(instance)
        echo "delete_node: the jenkins node is now delete -> " + instance
    } else {
        echo "delete_node: Node doesn't exist"
    }
}
