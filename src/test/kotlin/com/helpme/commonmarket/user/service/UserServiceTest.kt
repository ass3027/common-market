package com.helpme.commonmarket.user.service

import com.helpme.commonmarket.user.dto.UserDto
import com.helpme.commonmarket.user.entity.User
import com.helpme.commonmarket.user.mapper.toDto
import com.helpme.commonmarket.user.mapper.toEntity
import com.helpme.commonmarket.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime
import java.util.Optional

class UserServiceTest {

    private val userRepository: UserRepository = mockk()
    private val passwordEncoder: PasswordEncoder = BCryptPasswordEncoder()
    private val userService = UserService(userRepository, passwordEncoder)

    private val dummyUser = User(
        id = 1L,
        name = "Test User",
        email = "test@example.com",
        password = passwordEncoder.encode("password"),
        createDt = LocalDateTime.now(),
        updateDt = LocalDateTime.now()
    )

    private val dummyUserRes = dummyUser.toDto()

    @Test
    fun `getUserById should return a user when found`() {
        every { userRepository.findById(1L) } returns Optional.of(dummyUser)

        val result = userService.getUserById(1L)

        assertEquals(dummyUserRes, result)
        verify(exactly = 1) { userRepository.findById(1L) }
    }

    @Test
    fun `getUserById should throw an exception when user not found`() {
        every { userRepository.findById(any()) } returns Optional.empty()

        val exception = assertThrows(IllegalArgumentException::class.java) {
            userService.getUserById(99L)
        }
        assertEquals("User not found with id: 99", exception.message)
        verify(exactly = 1) { userRepository.findById(99L) }
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
        verify(exactly = 1) { userRepository.findAll(pageable) }
    }

    @Test
    fun `createUser should create a new user`() {
        val rawPassword = "new_password"
        val encodedPassword = passwordEncoder.encode(rawPassword)

        val userReq = UserDto.Req(
            name = "New User",
            email = "new@example.com",
            password = rawPassword
        )
        val userToSave = userReq.toEntity()
        val savedUser = userToSave.copy(id = 2L, password = encodedPassword, createDt = LocalDateTime.now(), updateDt = LocalDateTime.now())

        every { userRepository.save(any<User>()) } returns savedUser
        every { passwordEncoder.encode(rawPassword) } returns encodedPassword

        val result = userService.createUser(userReq)

        assertEquals(savedUser.toDto(), result)
        verify(exactly = 1) { userRepository.save(any<User>()) }
        verify(exactly = 1) { passwordEncoder.encode(rawPassword) }
    }

    @Test
    fun `updateUser should update an existing user`() {
        val rawPassword = "updated_password"
        val encodedPassword = passwordEncoder.encode(rawPassword)

        val userUpdateReq = UserDto.UpdateReq(
            id = 1L,
            name = "Updated User",
            email = "updated@example.com",
            password = rawPassword
        )
        val updatedUserEntity = dummyUser.copy(
            name = userUpdateReq.name!!,
            email = userUpdateReq.email!!,
            password = encodedPassword,
            updateDt = LocalDateTime.now().plusMinutes(1)
        )

        every { userRepository.findById(1L) } returns Optional.of(dummyUser)
        every { userRepository.save(any<User>()) } returns updatedUserEntity
        every { passwordEncoder.encode(rawPassword) } returns encodedPassword

        val result = userService.updateUser(userUpdateReq)

        assertEquals(updatedUserEntity.toDto(), result)
        verify(exactly = 1) { userRepository.findById(1L) }
        verify(exactly = 1) { userRepository.save(any<User>()) }
        verify(exactly = 1) { passwordEncoder.encode(rawPassword) }
    }

    @Test
    fun `deleteUser should delete an existing user`() {
        every { userRepository.existsById(1L) } returns true
        every { userRepository.deleteById(1L) } returns Unit

        userService.deleteUser(1L)

        verify(exactly = 1) { userRepository.existsById(1L) }
        verify(exactly = 1) { userRepository.deleteById(1L) }
    }
}