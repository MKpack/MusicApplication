package com.example.musicapplication.ui.auth.validation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidatorsTest {

    @Test
    fun email_validAddresses() {
        listOf("music@example.com", "a.b+c@mail.qq.com", " user@163.com ").forEach {
            assertTrue(it, AuthValidators.isEmailValid(it))
        }
    }

    @Test
    fun email_invalidAddresses() {
        listOf("", "music", "music@", "@example.com", "music@example", "mu sic@example.com", "music@example.c").forEach {
            assertFalse(it, AuthValidators.isEmailValid(it))
        }
    }
}
