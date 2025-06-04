package com.axians.eaf.iam

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class IamServiceApplication

fun main(args: Array<String>) {
    runApplication<IamServiceApplication>(*args)
}
