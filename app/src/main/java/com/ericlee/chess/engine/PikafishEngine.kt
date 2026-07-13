package com.ericlee.chess.engine

import android.content.Context
import android.os.Build
import com.ericlee.chess.model.Board
import com.ericlee.chess.model.Move
import com.ericlee.chess.model.Piece
import com.ericlee.chess.model.PieceType
import com.ericlee.chess.model.Side
import java.io.BufferedWriter
import java.io.File
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class PikafishEngine(
    private val context: Context,
    private val aiSide: Side = Side.BLACK
) {
    private val lock = Any()
    private val outputLines = LinkedBlockingQueue<String>()
    private var process: Process? = null
    private var writer: BufferedWriter? = null
    private var readerThread: Thread? = null
    private var lastInfo = "Pikafish UCI"
    @Volatile private var closed = false

    fun findBestMove(
        board: Board,
        depth: Int,
        positionCounts: Map<String, Int> = emptyMap()
    ): Move? = synchronized(lock) {
        if (closed) return@synchronized null
        val legalMoves = board.getAllLegalMoves(aiSide)
        if (legalMoves.isEmpty()) return@synchronized null

        val counts = positionCounts.withCurrentPosition(board, aiSide)
        val safeMoves = legalMoves.filterNot { move ->
            wouldCauseLongCheckLoss(board, move, aiSide, counts)
        }
        val candidateMoves = safeMoves.ifEmpty { legalMoves }

        if (!ensureStarted()) return@synchronized null
        val bestMove = search(board, depth, candidateMoves)
        if (bestMove != null && candidateMoves.any { it.sameSquares(bestMove) }) {
            bestMove
        } else {
            candidateMoves.firstOrNull()
        }
    }

    fun close() {
        shutdownNow()
    }

    fun requestStop() {
        closed = true
        outputLines.offer("")
        runCatching { process?.destroy() }
        readerThread?.interrupt()
    }

    fun shutdownNow() {
        closed = true
        val currentWriter = writer
        val currentProcess = process
        val currentReaderThread = readerThread
        writer = null
        process = null
        readerThread = null
        outputLines.offer("")
        runCatching {
            currentWriter?.write("stop")
            currentWriter?.newLine()
            currentWriter?.flush()
        }
        runCatching {
            currentWriter?.write("quit")
            currentWriter?.newLine()
            currentWriter?.flush()
        }
        runCatching { currentWriter?.close() }
        runCatching { currentProcess?.destroy() }
        runCatching { currentProcess?.destroyForcibly() }
        runCatching { currentProcess?.inputStream?.close() }
        currentReaderThread?.interrupt()
    }

    fun getSearchInfo(): String = lastInfo

    private fun search(board: Board, depth: Int, searchMoves: List<Move>): Move? {
        if (closed) return null
        clearOutput()
        send("position fen ${PikafishNotation.toFen(board, aiSide)}")
        val moveList = searchMoves.joinToString(separator = " ") { PikafishNotation.toUci(it) }
        send("go depth ${engineDepth(depth)} searchmoves $moveList")

        val line = waitForLine(timeoutMs = SEARCH_TIMEOUT_MS) { it.startsWith("bestmove ") }
            ?: return null
        val bestMoveText = line.split(' ').getOrNull(1).orEmpty()
        if (bestMoveText == "none" || bestMoveText == "(none)") return null
        return PikafishNotation.fromUci(bestMoveText)
    }

    private fun ensureStarted(): Boolean {
        if (closed) return false
        val existing = process
        if (existing != null && existing.isAlive && writer != null) return true

        closeDeadProcess()
        if (closed) return false
        val installed = PikafishInstaller.install(context) { closed }
        if (closed) return false
        outputLines.clear()
        val started = ProcessBuilder(installed.binary.absolutePath)
            .directory(installed.workingDir)
            .redirectErrorStream(true)
            .start()
        if (closed) {
            runCatching { started.destroy() }
            runCatching { started.destroyForcibly() }
            return false
        }
        process = started
        writer = BufferedWriter(OutputStreamWriter(started.outputStream))
        readerThread = Thread {
            try {
                started.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        outputLines.offer(line)
                        if (line.startsWith("info ")) {
                            lastInfo = line
                        }
                    }
                }
            } catch (_: IOException) {
                // Shutdown closes the process pipe while this thread may be blocked in read().
            }
        }.apply {
            name = "pikafish-uci-reader"
            isDaemon = true
            start()
        }

        send("uci")
        val uciReady = waitForLine(INIT_TIMEOUT_MS) { it == "uciok" }
        if (closed) {
            closeDeadProcess()
            return false
        }
        require(uciReady != null) {
            "Pikafish UCI 初始化失败"
        }
        send("setoption name Threads value ${engineThreads()}")
        send("setoption name Hash value 32")
        send("setoption name EvalFile value ${installed.evalFile.absolutePath}")
        send("isready")
        val evalReady = waitForLine(INIT_TIMEOUT_MS) { it == "readyok" }
        if (closed) {
            closeDeadProcess()
            return false
        }
        require(evalReady != null) {
            "Pikafish NNUE 初始化失败"
        }
        send("ucinewgame")
        send("isready")
        waitForLine(INIT_TIMEOUT_MS) { it == "readyok" }
        if (closed) {
            closeDeadProcess()
            return false
        }
        return true
    }

    private fun closeDeadProcess() {
        runCatching { writer?.close() }
        runCatching { process?.destroy() }
        writer = null
        process = null
        readerThread = null
    }

    private fun send(command: String) {
        if (closed) return
        val out = writer ?: error("Pikafish 尚未启动")
        out.write(command)
        out.newLine()
        out.flush()
    }

    private fun clearOutput() {
        while (outputLines.poll() != null) {
            // Drop stale info from a previous search.
        }
    }

    private fun waitForLine(timeoutMs: Long, predicate: (String) -> Boolean): String? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (closed) return null
            val remaining = deadline - System.currentTimeMillis()
            val line = outputLines.poll(remaining.coerceAtMost(250L), TimeUnit.MILLISECONDS)
            if (line != null && predicate(line)) return line
            if (closed) return null
            if (process?.isAlive == false) return null
        }
        return null
    }

    private fun engineDepth(difficulty: Int): Int = when (difficulty.coerceIn(1, 4)) {
        1 -> 4
        2 -> 6
        3 -> 8
        else -> 10
    }

    private fun engineThreads(): Int =
        Runtime.getRuntime().availableProcessors().coerceIn(1, 4)

    private fun Map<String, Int>.withCurrentPosition(board: Board, side: Side): Map<String, Int> {
        if (isNotEmpty()) return this
        return mapOf(board.positionKey(side) to 1)
    }

    private fun wouldCauseLongCheckLoss(
        board: Board,
        move: Move,
        side: Side,
        positionCounts: Map<String, Int>
    ): Boolean {
        val actualMove = move.copy(captured = board.makeMove(move))
        val targetSide = side.opposite()
        val repeated = (positionCounts[board.positionKey(targetSide)] ?: 0) >= 2
        val givesCheck = board.isInCheck(targetSide)
        board.undoMove(actualMove)
        return repeated && givesCheck
    }

    private fun Move.sameSquares(other: Move): Boolean =
        fromRow == other.fromRow &&
            fromCol == other.fromCol &&
            toRow == other.toRow &&
            toCol == other.toCol

    private data class InstalledPikafish(
        val workingDir: File,
        val binary: File,
        val evalFile: File
    )

    private object PikafishInstaller {
        private val installLock = Any()

        fun install(
            context: Context,
            isCancelled: () -> Boolean
        ): InstalledPikafish = synchronized(installLock) {
            require(Build.SUPPORTED_64_BIT_ABIS.contains("arm64-v8a")) {
                "当前设备不支持 ARM64 Pikafish"
            }
            val binary = File(context.applicationInfo.nativeLibraryDir, PACKAGED_BINARY)
            require(binary.exists()) {
                "未找到 Pikafish ARM64 原生引擎"
            }
            val targetDir = evalDirectory(context)
            require(targetDir.exists() || targetDir.mkdirs()) {
                "无法创建 Pikafish 资源目录"
            }
            val evalFile = File(targetDir, EVAL_FILE)
            val validationFile = File(targetDir, "$EVAL_FILE.sha256")
            val alreadyValidated = evalFile.length() == EVAL_FILE_SIZE &&
                validationFile.readTextOrEmpty() == EVAL_FILE_SHA256
            if (!alreadyValidated) {
                if (evalFile.length() == EVAL_FILE_SIZE && evalFile.sha256() == EVAL_FILE_SHA256) {
                    validationFile.writeText(EVAL_FILE_SHA256)
                } else {
                    downloadEvalFile(evalFile, validationFile, isCancelled)
                }
            }
            InstalledPikafish(targetDir, binary, evalFile)
        }

        private fun evalDirectory(context: Context): File =
            File(context.noBackupFilesDir, "pikafish/$ENGINE_VERSION")

        private fun downloadEvalFile(
            target: File,
            validationFile: File,
            isCancelled: () -> Boolean
        ) {
            val tmp = File(target.parentFile, "${target.name}.tmp")
            target.delete()
            validationFile.delete()
            tmp.delete()
            try {
                val connection = (URL(EVAL_FILE_URL).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = DOWNLOAD_CONNECT_TIMEOUT_MS
                    readTimeout = DOWNLOAD_READ_TIMEOUT_MS
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/octet-stream")
                }
                try {
                    val responseCode = connection.responseCode
                    if (responseCode !in 200..299) {
                        throw IOException("NNUE 下载失败：HTTP $responseCode")
                    }
                    val expectedLength = connection.contentLengthLong
                    if (expectedLength > 0L && expectedLength != EVAL_FILE_SIZE) {
                        throw IOException("NNUE 文件大小不匹配")
                    }
                    connection.inputStream.buffered().use { input ->
                        tmp.outputStream().buffered().use { output ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            while (true) {
                                if (isCancelled()) throw IOException("NNUE 下载已取消")
                                val count = input.read(buffer)
                                if (count < 0) break
                                output.write(buffer, 0, count)
                            }
                        }
                    }
                } finally {
                    connection.disconnect()
                }
                if (tmp.length() != EVAL_FILE_SIZE || tmp.sha256() != EVAL_FILE_SHA256) {
                    throw IOException("NNUE 文件校验失败")
                }
                require(tmp.renameTo(target)) {
                    "无法安装 Pikafish 资源：${target.name}"
                }
                validationFile.writeText(EVAL_FILE_SHA256)
            } catch (error: Throwable) {
                tmp.delete()
                throw IllegalStateException(
                    if (isCancelled()) "NNUE 下载已取消" else "首次使用需下载 AI 数据，请检查网络后重试",
                    error
                )
            }
        }

        private fun File.sha256(): String {
            if (!isFile) return ""
            val digest = MessageDigest.getInstance("SHA-256")
            inputStream().buffered().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
        }

        private fun File.readTextOrEmpty(): String =
            runCatching { readText().trim() }.getOrDefault("")
    }

    private object PikafishNotation {
        fun toFen(board: Board, sideToMove: Side): String {
            val rows = (0..9).joinToString("/") { row ->
                buildString {
                    var empty = 0
                    for (col in 0..8) {
                        val piece = board.getPiece(row, col)
                        if (piece == null) {
                            empty++
                        } else {
                            if (empty > 0) {
                                append(empty)
                                empty = 0
                            }
                            append(piece.fenChar())
                        }
                    }
                    if (empty > 0) append(empty)
                }
            }
            val side = if (sideToMove == Side.RED) "w" else "b"
            return "$rows $side - - 0 1"
        }

        fun toUci(move: Move): String =
            "${file(move.fromCol)}${rank(move.fromRow)}${file(move.toCol)}${rank(move.toRow)}"

        fun fromUci(text: String): Move? {
            if (text.length < 4) return null
            val fromCol = text[0] - 'a'
            val fromRow = 9 - text[1].digitToIntOrNull().orInvalid()
            val toCol = text[2] - 'a'
            val toRow = 9 - text[3].digitToIntOrNull().orInvalid()
            if (fromRow !in 0..9 || toRow !in 0..9 || fromCol !in 0..8 || toCol !in 0..8) {
                return null
            }
            return Move(fromRow, fromCol, toRow, toCol)
        }

        private fun Piece.fenChar(): Char {
            val char = when (type) {
                PieceType.KING -> 'k'
                PieceType.ADVISOR -> 'a'
                PieceType.ELEPHANT -> 'b'
                PieceType.ROOK -> 'r'
                PieceType.KNIGHT -> 'n'
                PieceType.CANNON -> 'c'
                PieceType.PAWN -> 'p'
            }
            return if (side == Side.RED) char.uppercaseChar() else char
        }

        private fun file(col: Int): Char = ('a'.code + col).toChar()

        private fun rank(row: Int): Int = 9 - row

        private fun Int?.orInvalid(): Int = this ?: -100
    }

    companion object {
        fun prefetchEvalFile(context: Context, isCancelled: () -> Boolean = { false }) {
            runCatching {
                PikafishInstaller.install(context.applicationContext, isCancelled)
            }
        }

        private const val ENGINE_VERSION = "2026-01-02"
        private const val PACKAGED_BINARY = "libpikafish.so"
        private const val EVAL_FILE = "pikafish.nnue"
        private const val EVAL_FILE_URL =
            "https://gitfly.qzz.io/https://github.com/giturass/Pikafish/releases/download/1.0/pikafish.nnue"
        private const val EVAL_FILE_SIZE = 53_212_941L
        private const val EVAL_FILE_SHA256 =
            "c4026370d7516d9b0f668447f9ca1931241538bdc689cde6fec6a991ac4d5f77"
        private const val DOWNLOAD_CONNECT_TIMEOUT_MS = 15_000
        private const val DOWNLOAD_READ_TIMEOUT_MS = 120_000
        private const val INIT_TIMEOUT_MS = 12_000L
        private const val SEARCH_TIMEOUT_MS = 20_000L
    }
}
