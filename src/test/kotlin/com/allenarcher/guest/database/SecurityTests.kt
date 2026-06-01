package com.allenarcher.guest.database

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
class SecurityTests {

    @Autowired lateinit var mockMvc: MockMvc

    @Test
    fun `unauthenticated GET is denied`() {
        mockMvc.perform(get("/stays?from=2026-01-01&to=2026-01-10"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `unauthenticated POST is denied`() {
        mockMvc.perform(
            post("/stays/upsert")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]")
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `reader can access GET endpoints`() {
        mockMvc.perform(
            get("/stays?from=2026-01-01&to=2026-01-10")
                .with(httpBasic("reader", "changeme"))
        ).andExpect(status().isOk)
    }

    @Test
    fun `reader cannot access POST endpoints`() {
        mockMvc.perform(
            post("/stays/upsert")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]")
                .with(httpBasic("reader", "changeme"))
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `admin can access GET endpoints`() {
        mockMvc.perform(
            get("/stays?from=2026-01-01&to=2026-01-10")
                .with(httpBasic("admin", "changeme"))
        ).andExpect(status().isOk)
    }

    @Test
    fun `admin can access POST endpoints`() {
        mockMvc.perform(
            post("/stays/upsert")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]")
                .with(httpBasic("admin", "changeme"))
        ).andExpect(status().isOk)
    }

    @Test
    fun `form login with remember-me sets cookie`() {
        mockMvc.perform(
            post("/login")
                .param("username", "admin")
                .param("password", "changeme")
                .param("remember-me", "on")
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(cookie().exists("remember-me"))
    }

    @Test
    fun `remember-me cookie authenticates without session`() {
        val loginResult = mockMvc.perform(
            post("/login")
                .param("username", "admin")
                .param("password", "changeme")
                .param("remember-me", "on")
        ).andReturn()

        val rememberMeCookie = loginResult.response.getCookie("remember-me")!!

        mockMvc.perform(
            get("/stays?from=2026-01-01&to=2026-01-10")
                .cookie(rememberMeCookie)
        ).andExpect(status().isOk)
    }
}
