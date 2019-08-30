# jenkins-pipeline-libraries

# Requirements

* Install this plugin -> https://plugins.jenkins.io/aws-java-sdk
* Add as a Global Pipeline Library
* Having an AMI with your jenkins SSH key
* add your Outscale AK/SK in jenkins secret


# Parameters

- add_node
    - instance_type
    - disk_size
    - job_name
    - ak
    - sk
    - fcu_region
    - fcu_endpoint
    - prefix_name
** 
- delete_node
    - ak
    - sk
    - job_name

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
