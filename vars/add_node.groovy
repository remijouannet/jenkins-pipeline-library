#!/usr/bin/env groovy

def call(String c, String d) {
    def b = new com.remijouannet.add_node()
    def a = b.get_current_instance_id()
    println(a)
    println(c)
    println(d)
    echo "test aa"
}
