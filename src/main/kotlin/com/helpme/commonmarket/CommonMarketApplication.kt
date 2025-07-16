package com.helpme.commonmarket

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CommonMarketApplication

fun main(args: Array<String>) {
    System.setProperty("user.timezone", "Asia/Seoul");
    runApplication<CommonMarketApplication>(*args)
}
