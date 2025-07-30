package com.helpme.commonmarket.user.service

import com.helpme.commonmarket.user.dto.UserDto
import com.helpme.commonmarket.user.entity.User
import com.helpme.commonmarket.user.mapper.toDto
import com.helpme.commonmarket.user.mapper.toEntity
import com.helpme.commonmarket.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime
import java.util.Optional

class UserServiceTest {

    private val userRepository: UserRepository = mockk()
    private val passwordEncoder: PasswordEncoder = mockk()
    private val userService = UserService(userRepository, passwordEncoder)

    private val dummyUser = User(
        id = "1",
        name = "Test User",
        email = "test@example.com",
        password = "encoded_password",
        role = "USER",
        createDt = LocalDateTime.now(),
        updateDt = LocalDateTime.now()
    )

    private val dummyUserRes = dummyUser.toDto()

    @Test
    fun `getUserById should return a user when found`() {
        every { userRepository.findById("1") } returns Optional.of(dummyUser)

        val result = userService.getUserById("1")

        assertEquals(dummyUserRes, result)
    }

    @Test
    fun `getUserById should throw an exception when user not found`() {
        every { userRepository.findById(any()) } returns Optional.empty()

        val exception = assertThrows(IllegalArgumentException::class.java) {
            userService.getUserById("99")
        }
        assertEquals("User not found with id: 99", exception.message)
    }

    @Test
    fun `getAllUsers should return a page of users`() {
        val pageable = PageRequest.of(0, 10)
        val userList = listOf(dummyUser)
        val userPage = PageImpl(userList, pageable, 1)

        every { userRepository.findAll(pageable) } returns userPage

        val result = userService.getAllUsers(pageable)

        assertEquals(1, result.totalElements)
        assertEquals(dummyUserRes, result.content[0])
    }

    @Test
    fun `createUser should create a new user`() {
        val rawPassword = "new_password"
        val encodedPassword = "encoded_new_password"

        val userReq = UserDto.Req(
            id = "1",
            name = "New User",
            email = "new@example.com",
            password = rawPassword
        )
        val userToSave = userReq.toEntity()
        val savedUser = User(
            id = "2",
            name = userToSave.name,
            email = userToSave.email,
            password = encodedPassword,
            role = "USER",
            createDt = LocalDateTime.now(),
            updateDt = LocalDateTime.now()
        )

        every { passwordEncoder.encode(rawPassword) } returns encodedPassword
        every { userRepository.save(any<User>()) } returns savedUser

        val result = userService.createUser(userReq)

        assertEquals(savedUser.toDto(), result)
    }

    @Test
    fun `updateUser should update an existing user`() {
        val rawPassword = "updated_password"
        val encodedPassword = "encoded_updated_password"

        val userUpdateReq = UserDto.UpdateReq(
            id = "1",
            name = "Updated User",
            email = "updated@example.com",
            password = rawPassword
        )
        val updatedUserEntity = User(
            id = dummyUser.id,
            name = userUpdateReq.name!!,
            email = userUpdateReq.email!!,
            password = encodedPassword,
            role = dummyUser.role,
            createDt = dummyUser.createDt,
            updateDt = LocalDateTime.now().plusMinutes(1)
        )

        every { passwordEncoder.encode(rawPassword) } returns encodedPassword
        every { userRepository.findById("1") } returns Optional.of(dummyUser)
        every { userRepository.save(any<User>()) } returns updatedUserEntity

        val result = userService.updateUser(userUpdateReq)

        assertEquals(updatedUserEntity.toDto(), result)
    }

    @Test
    fun `deleteUser should delete an existing user`() {
        every { userRepository.existsById("1") } returns true
        every { userRepository.deleteById("1") } returns Unit

        userService.deleteUser("1")

        // Then - Successful deletion doesn't throw an exception
        // The fact that no exception was thrown indicates successful deletion
    }
}