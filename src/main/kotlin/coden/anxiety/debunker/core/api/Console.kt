package coden.anxiety.debunker.core.api

import java.io.Closeable

interface Console: Closeable {
    fun start()
    fun stop()
    override fun close() { stop() }
}