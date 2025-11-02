package com.ampairs.tally

import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.util.LinkedList
import java.util.Queue


class ReplacingInputStream(inputStream: InputStream?, search: String, replacement: String) :
    FilterInputStream(inputStream) {
    private val inQueue: Queue<Int>
    private val outQueue: Queue<Int>
    private val search: ByteArray
    private val replacement: ByteArray

    init {
        inQueue = LinkedList()
        outQueue = LinkedList()
        this.search = search.toByteArray()
        this.replacement = replacement.toByteArray()
    }

    private val isMatchFound: Boolean
        private get() {
            val iterator: Iterator<Int> = inQueue.iterator()
            for (b in search) {
                if (!iterator.hasNext() || b.toInt() != iterator.next()) {
                    return false
                }
            }
            return true
        }

    @Throws(IOException::class)
    private fun readAhead() {
        // Work up some look-ahead.
        while (inQueue.size < search.size) {
            val next = super.read()
            inQueue.offer(next)
            if (next == -1) {
                break
            }
        }
    }

    @Throws(IOException::class)
    override fun read(): Int {
        // Next byte already determined.
        while (outQueue.isEmpty()) {
            readAhead()
            if (isMatchFound) {
                for (a in search) {
                    inQueue.remove()
                }
                for (b in replacement) {
                    outQueue.offer(b.toInt())
                }
            } else {
                outQueue.add(inQueue.remove())
            }
        }
        return outQueue.remove()
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray): Int {
        return read(b, 0, b.size)
    }

    // copied straight from InputStream inplementation, just needed to to use `read()` from this class
    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (b == null) {
            throw NullPointerException()
        } else if (off < 0 || len < 0 || len > b.size - off) {
            throw IndexOutOfBoundsException()
        } else if (len == 0) {
            return 0
        }
        var c = read()
        if (c == -1) {
            return -1
        }
        b[off] = c.toByte()
        var i = 1
        try {
            while (i < len) {
                c = read()
                if (c == -1) {
                    break
                }
                b[off + i] = c.toByte()
                i++
            }
        } catch (ee: IOException) {
        }
        return i
    }
}