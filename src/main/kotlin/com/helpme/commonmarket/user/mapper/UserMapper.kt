package com.helpme.commonmarket.user.mapper

import com.helpme.commonmarket.user.dto.UserDto
import com.helpme.commonmarket.user.entity.User
import java.time.LocalDateTime

fun User.toDto(): UserDto.Res {
    return UserDto.Res(
        id = this.id,
        name = this.name,
        email = this.email,
        createDt = this.createDt,
        updateDt = this.updateDt
    )
}

fun UserDto.Req.toEntity(): User {
    return User(
        id = this.id,
        name = this.name,
        email = this.email,
        password = this.password,
        createDt = LocalDateTime.now(),
        updateDt = LocalDateTime.now()
    )
}

fun UserDto.UpdateReq.toEntity(existingUser: User): User {
    return existingUser.apply {
        name = this@toEntity.name ?: name
        email = this@toEntity.email ?: email
        // Password will be handled in UserService
        updateDt = LocalDateTime.now()
    }
}