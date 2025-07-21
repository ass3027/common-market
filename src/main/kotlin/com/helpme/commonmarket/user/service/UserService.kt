package com.helpme.commonmarket.user.service

import com.helpme.commonmarket.user.dto.UserDto
import com.helpme.commonmarket.user.mapper.toDto
import com.helpme.commonmarket.user.mapper.toEntity
import com.helpme.commonmarket.user.mapper.toEntity as updateToEntity
import com.helpme.commonmarket.user.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @Transactional(readOnly = true)
    fun getUserById(id: Long): UserDto.Res {
        val user = userRepository.findById(id).orElseThrow {
            IllegalArgumentException("User not found with id: $id")
        }
        return user.toDto()
    }

    @Transactional(readOnly = true)
    fun getAllUsers(pageable: Pageable): Page<UserDto.Res> {
        return userRepository.findAll(pageable).map { it.toDto() }
    }

    fun createUser(userReq: UserDto.Req): UserDto.Res {
        val user = userReq.toEntity()
        user.password = passwordEncoder.encode(user.password)
        val savedUser = userRepository.save(user)
        return savedUser.toDto()
    }

    fun updateUser(userUpdateReq: UserDto.UpdateReq): UserDto.Res {
        val user = userRepository.findById(userUpdateReq.id).orElseThrow {
            IllegalArgumentException("User not found with id: ${userUpdateReq.id}")
        }

        userUpdateReq.name?.let { user.name = it }
        userUpdateReq.email?.let { user.email = it }
        userUpdateReq.password?.let { user.password = passwordEncoder.encode(it) }

        val updatedUser = userRepository.save(user)
        return updatedUser.toDto()
    }

    fun deleteUser(id: Long) {
        if (!userRepository.existsById(id)) {
            throw IllegalArgumentException("User not found with id: $id")
        }
        userRepository.deleteById(id)
    }
}
