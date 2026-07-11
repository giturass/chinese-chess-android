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
import java.io.OutputStreamWriter
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

    fun findBestMove(
        board: Board,
        depth: Int,
        positionCounts: Map<String, Int> = emptyMap()
    ): Move? = synchronized(lock) {
        val legalMoves = board.getAllLegalMoves(aiSide)
        if (legalMoves.isEmpty()) return@synchronized null

        val counts = positionCounts.withCurrentPosition(board, aiSide)
        val safeMoves = legalMoves.filterNot { move ->
            wouldCauseLongCheckLoss(board, move, aiSide, counts)
        }
        val candidateMoves = safeMoves.ifEmpty { legalMoves }

        ensureStarted()
        val bestMove = search(board, depth, candidateMoves)
        if (bestMove != null && candidateMoves.any { it.sameSquares(bestMove) }) {
            bestMove
        } else {
            candidateMoves.firstOrNull()
        }
    }

    fun close() = synchronized(lock) {
        runCatching { send("quit") }
        runCatching { writer?.close() }
        runCatching { process?.destroy() }
        writer = null
        process = null
        readerThread = null
        outputLines.clear()
    }

    fun getSearchInfo(): String = lastInfo

    private fun search(board: Board, depth: Int, searchMoves: List<Move>): Move? {
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

    private fun ensureStarted() {
        val existing = process
        if (existing != null && existing.isAlive && writer != null) return

        closeDeadProcess()
        val installed = PikafishInstaller.install(context)
        outputLines.clear()
        val started = ProcessBuilder(installed.binary.absolutePath)
            .directory(installed.workingDir)
            .redirectErrorStream(true)
            .start()
        process = started
        writer = BufferedWriter(OutputStreamWriter(started.outputStream))
        readerThread = Thread {
            started.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    outputLines.offer(line)
                    if (line.startsWith("info ")) {
                        lastInfo = line
                    }
                }
            }
        }.apply {
            name = "pikafish-uci-reader"
            isDaemon = true
            start()
        }

        send("uci")
        require(waitForLine(INIT_TIMEOUT_MS) { it == "uciok" } != null) {
            "Pikafish UCI 初始化失败"
        }
        send("setoption name Threads value ${engineThreads()}")
        send("setoption name Hash value 32")
        send("setoption name EvalFile value ${installed.evalFile.absolutePath}")
        send("isready")
        require(waitForLine(INIT_TIMEOUT_MS) { it == "readyok" } != null) {
            "Pikafish NNUE 初始化失败"
        }
        send("ucinewgame")
        send("isready")
        waitForLine(INIT_TIMEOUT_MS) { it == "readyok" }
    }

    private fun closeDeadProcess() {
        runCatching { writer?.close() }
        runCatching { process?.destroy() }
        writer = null
        process = null
        readerThread = null
    }

    private fun send(command: String) {
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
            val remaining = deadline - System.currentTimeMillis()
            val line = outputLines.poll(remaining.coerceAtMost(250L), TimeUnit.MILLISECONDS)
            if (line != null && predicate(line)) return line
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
        fun install(context: Context): InstalledPikafish {
            require(Build.SUPPORTED_64_BIT_ABIS.contains("arm64-v8a")) {
                "当前设备不支持 ARM64 Pikafish"
            }
            val binary = File(context.applicationInfo.nativeLibraryDir, PACKAGED_BINARY)
            require(binary.exists()) {
                "未找到 Pikafish ARM64 原生引擎"
            }
            val targetDir = File(context.noBackupFilesDir, "pikafish/$ENGINE_VERSION")
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            val evalFile = File(targetDir, EVAL_FILE)
            copyAsset(context, "engines/pikafish/$EVAL_FILE", evalFile)
            binary.setReadable(true, false)
            binary.setExecutable(true, false)
            return InstalledPikafish(targetDir, binary, evalFile)
        }

        private fun copyAsset(context: Context, assetPath: String, target: File) {
            if (target.exists() && target.length() > 0L) return
            val tmp = File(target.parentFile, "${target.name}.tmp")
            context.assets.open(assetPath).use { input ->
                tmp.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            if (target.exists()) {
                target.delete()
            }
            require(tmp.renameTo(target)) {
                "无法安装 Pikafish 资源：${target.name}"
            }
        }
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

    private companion object {
        const val ENGINE_VERSION = "2026-01-02"
        const val PACKAGED_BINARY = "libpikafish.so"
        const val EVAL_FILE = "pikafish.nnue"
        const val INIT_TIMEOUT_MS = 12_000L
        const val SEARCH_TIMEOUT_MS = 20_000L
    }
}
