package org.davidnardya.vocalcoach

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform