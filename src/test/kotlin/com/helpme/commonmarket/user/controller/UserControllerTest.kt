package com.helpme.commonmarket.user.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.helpme.commonmarket.config.JacksonConfig
import com.helpme.commonmarket.user.dto.UserDto
import com.helpme.commonmarket.user.service.UserService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@WebMvcTest(UserController::class)
class UserControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val userService: UserService
) {

    @TestConfiguration
    class ControllerTestConfig {
        @Bean
        fun userService() = mockk<UserService>()

        @Bean
        fun objectMapper() = JacksonConfig().objectMapper()
    }

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val fixedDateTime = LocalDateTime.of(2023, 1, 15, 10, 30, 0)

    private val dummyUserRes = UserDto.Res(
        id = 1L,
        name = "Test User",
        email = "test@example.com",
        createDt = fixedDateTime,
        updateDt = fixedDateTime
    )

    @Test
    fun `GET users should return a page of users`() {
        val pageable = PageRequest.of(0, 10)
        val userPage = PageImpl(listOf(dummyUserRes), pageable, 1)

        every { userService.getAllUsers(any()) } returns userPage

        mockMvc.perform(get("/users"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].id").value(dummyUserRes.id))
            .andExpect(jsonPath("$.content[0].name").value(dummyUserRes.name))
            .andExpect(jsonPath("$.content[0].createDt").value(fixedDateTime.format(formatter)))
            .andExpect(jsonPath("$.content[0].updateDt").value(fixedDateTime.format(formatter)))

        verify(exactly = 1) { userService.getAllUsers(any()) }
    }

    @Test
    fun `GET users by userId should return a single user`() {
        every { userService.getUserById(1L) } returns dummyUserRes

        mockMvc.perform(get("/users/1"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(dummyUserRes.id))
            .andExpect(jsonPath("$.name").value(dummyUserRes.name))
            .andExpect(jsonPath("$.createDt").value(fixedDateTime.format(formatter)))
            .andExpect(jsonPath("$.updateDt").value(fixedDateTime.format(formatter)))

        verify(exactly = 1) { userService.getUserById(1L) }
    }

    @Test
    fun `POST users should create a new user`() {
        val userReq = UserDto.Req(
            name = "New User",
            email = "new@example.com",
            password = "password"
        )

        every { userService.createUser(any()) } returns dummyUserRes

        mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(userReq)))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(dummyUserRes.id))
            .andExpect(jsonPath("$.name").value(dummyUserRes.name))
            .andExpect(jsonPath("$.createDt").value(fixedDateTime.format(formatter)))
            .andExpect(jsonPath("$.updateDt").value(fixedDateTime.format(formatter)))

        verify(exactly = 1) { userService.createUser(any()) }
    }

    @Test
    fun `PUT users should update an existing user`() {
        val userUpdateReq = UserDto.UpdateReq(
            id = 1L,
            name = "Updated User",
            email = "updated@example.com",
            password = "new_password"
        )

        every { userService.updateUser(any()) } returns dummyUserRes

        mockMvc.perform(put("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(userUpdateReq)))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(dummyUserRes.id))
            .andExpect(jsonPath("$.name").value(dummyUserRes.name))
            .andExpect(jsonPath("$.createDt").value(fixedDateTime.format(formatter)))
            .andExpect(jsonPath("$.updateDt").value(fixedDateTime.format(formatter)))

        verify(exactly = 1) { userService.updateUser(any()) }
    }

    @Test
    fun `DELETE users userId는 기존 사용자를 삭제해야 한다`() {
        every { userService.deleteUser(1L) } returns Unit

        mockMvc.perform(delete("/users/1"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isNoContent)

        verify(exactly = 1) { userService.deleteUser(1L) }
    }
}