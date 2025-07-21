package com.helpme.commonmarket.user.repository

import com.helpme.commonmarket.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long>
