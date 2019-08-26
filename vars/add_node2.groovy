#!/usr/bin/env groovy

def call() {
    def b = new com.remijouannet.add_node()
    def a = b.get_current_instance_id()
    println(a)
    echo "test aa"
}
