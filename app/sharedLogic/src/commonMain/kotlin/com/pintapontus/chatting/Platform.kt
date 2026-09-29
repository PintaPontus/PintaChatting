package com.pintapontus.chatting

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform