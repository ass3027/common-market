package com.helpme.commonmarket.user.dto

import java.time.LocalDateTime

class UserDto {
    data class Res(
        val id: String,
        val name: String,
        val email: String,
        val createDt: LocalDateTime,
        val updateDt: LocalDateTime
    )

    data class Req(
        val name: String,
        val email: String,
        val password: String
    )

    data class UpdateReq(
        val id: String,
        val name: String?,
        val email: String?,
        val password: String?
    )
}
