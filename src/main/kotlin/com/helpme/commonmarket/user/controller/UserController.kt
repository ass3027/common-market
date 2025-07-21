package com.helpme.commonmarket.user.controller

import com.helpme.commonmarket.user.dto.UserDto
import com.helpme.commonmarket.user.service.UserService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/users")
class UserController(private val userService: UserService) {

    @GetMapping
    fun getAllUsers(@PageableDefault(size = 10, sort = ["id"]) pageable: Pageable): ResponseEntity<Page<UserDto.Res>> {
        val users = userService.getAllUsers(pageable)
        return ResponseEntity.ok(users)
    }

    @GetMapping("/{userId}")
    fun getUserById(@PathVariable userId: Long): ResponseEntity<UserDto.Res> {
        val user = userService.getUserById(userId)
        return ResponseEntity.ok(user)
    }

    @PostMapping
    fun createUser(@RequestBody userReq: UserDto.Req): ResponseEntity<UserDto.Res> {
        val createdUser = userService.createUser(userReq)
        return ResponseEntity(createdUser, HttpStatus.CREATED)
    }

    @PutMapping
    fun updateUser(@RequestBody userUpdateReq: UserDto.UpdateReq): ResponseEntity<UserDto.Res> {
        val updatedUser = userService.updateUser(userUpdateReq)
        return ResponseEntity.ok(updatedUser)
    }

    @DeleteMapping("/{userId}")
    fun deleteUser(@PathVariable userId: Long): ResponseEntity<Void> {
        userService.deleteUser(userId)
        return ResponseEntity.noContent().build()
    }
}