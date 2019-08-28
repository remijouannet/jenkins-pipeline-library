#!/usr/bin/env groovy

def call(body) {
    def b = new com.remijouannet.manageNode()
    def a = b.get_current_instance_id()
    println(a)
    println(body.toString())
    echo "test aa"
}
