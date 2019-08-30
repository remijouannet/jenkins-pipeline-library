# jenkins-pipeline-libraries

# Requirements

* Install this plugin -> https://plugins.jenkins.io/aws-java-sdk
* Add as a Global Pipeline Library
* Having an AMI with your jenkins SSH key
* add your Outscale AK/SK in jenkins secret


# Behavior
## add_node
It will search a AMI named 'jenkins-node-centos7' and run it with a second disk,
the instance will be run in the same VPC subnet of your jenkins master

## delete_node
it will delete the node by searching the tag job_name

# Parameters

- add_node:
    - instance_type: instance type for the new node, m4.large by default
    - disk_size: disk size for the 2nd disk, 10Gb by default (/dev/xvdb)
    - job_name: value for the tag 'job_name'
    - ak: Access Key
    - sk: Secret Key
    - fcu_region: FCU Region
    - fcu_endpoint: FCU custom endpoint
    - prefix_name: prefix for the tag Name
** 
- delete_node: it will terminate the instance
    - ak: Access Key
    - sk: Secret Key
    - job_name: it will delete the first instance it find with this value in the tag job_name

# Jenkins Scripted Pipeline Example

```groovy
stage('Run') {
    withCredentials([usernamePassword(credentialsId: 'aws-creds', usernameVariable: "AWS_ACCESS_KEY", passwordVariable: "AWS_SECRET_KEY")]) {
        add_node(job_name: env.JOB_NAME, ak: env.AWS_ACCESS_KEY, sk: env.AWS_SECRET_KEY)
    }
}
stage('Build'){
    node(env.JOB_NAME){
        sh(script: 'systemd-analyze blame')
    }
}
stage('Delete') {
    withCredentials([usernamePassword(credentialsId: 'aws-creds', usernameVariable: "AWS_ACCESS_KEY", passwordVariable: "AWS_SECRET_KEY")]) {
        delete_node(job_name: env.JOB_NAME, ak: env.AWS_ACCESS_KEY, sk: env.AWS_SECRET_KEY)
    }
}
```
