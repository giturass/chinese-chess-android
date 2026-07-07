package com.xiaomi.chess.model

class Board(val pieces: MutableList<Piece> = Piece.initialPieces().toMutableList()) {

    fun getPiece(row: Int, col: Int): Piece? =
        pieces.find { it.row == row && it.col == col }

    fun getPiecesBySide(side: Side): List<Piece> =
        pieces.filter { it.side == side }

    fun findKing(side: Side): Piece? =
        pieces.find { it.side == side && it.type == PieceType.KING }

    fun makeMove(move: Move): Piece? {
        val captured = getPiece(move.toRow, move.toCol)
        pieces.removeAll { it.row == move.toRow && it.col == move.toCol }
        val idx = pieces.indexOfFirst { it.row == move.fromRow && it.col == move.fromCol }
        if (idx >= 0) {
            pieces[idx] = pieces[idx].copy(row = move.toRow, col = move.toCol)
        }
        return captured
    }

    fun undoMove(move: Move) {
        val idx = pieces.indexOfFirst { it.row == move.toRow && it.col == move.toCol }
        if (idx >= 0) {
            pieces[idx] = pieces[idx].copy(row = move.fromRow, col = move.fromCol)
        }
        if (move.captured != null) {
            pieces.add(move.captured)
        }
    }

    fun copy(): Board {
        val newBoard = Board(mutableListOf())
        newBoard.pieces.clear()
        newBoard.pieces.addAll(pieces.map { it.copy() })
        return newBoard
    }

    fun isInsidePalace(row: Int, col: Int, side: Side): Boolean {
        val colOk = col in 3..5
        return if (side == Side.RED) {
            colOk && row in 7..9
        } else {
            colOk && row in 0..2
        }
    }

    fun isOnBoard(row: Int, col: Int): Boolean =
        row in 0..9 && col in 0..8

    fun isCrossRiver(row: Int, side: Side): Boolean =
        if (side == Side.RED) row <= 4 else row >= 5

    fun countPiecesBetweenInRow(row: Int, col1: Int, col2: Int): Int {
        val minC = minOf(col1, col2)
        val maxC = maxOf(col1, col2)
        var count = 0
        for (c in (minC + 1) until maxC) {
            if (getPiece(row, c) != null) count++
        }
        return count
    }

    fun countPiecesBetweenInCol(col: Int, row1: Int, row2: Int): Int {
        val minR = minOf(row1, row2)
        val maxR = maxOf(row1, row2)
        var count = 0
        for (r in (minR + 1) until maxR) {
            if (getPiece(r, col) != null) count++
        }
        return count
    }

    fun kingsAreFacing(): Boolean {
        val redKing = findKing(Side.RED) ?: return false
        val blackKing = findKing(Side.BLACK) ?: return false
        if (redKing.col != blackKing.col) return false
        return countPiecesBetweenInCol(redKing.col, redKing.row, blackKing.row) == 0
    }

    fun isInCheck(side: Side): Boolean {
        val king = findKing(side) ?: return true
        val opponent = side.opposite()
        for (piece in getPiecesBySide(opponent)) {
            if (canPieceAttack(piece, king.row, king.col)) return true
        }
        return false
    }

    private fun canPieceAttack(piece: Piece, targetRow: Int, targetCol: Int): Boolean {
        val dr = targetRow - piece.row
        val dc = targetCol - piece.col
        return when (piece.type) {
            PieceType.KING -> {
                isInsidePalace(targetRow, targetCol, piece.side) &&
                    (kotlin.math.abs(dr) + kotlin.math.abs(dc) == 1) &&
                    !isKingExposedAfterMove(piece, targetRow, targetCol)
            }
            PieceType.ADVISOR -> {
                isInsidePalace(targetRow, targetCol, piece.side) &&
                    kotlin.math.abs(dr) == 1 && kotlin.math.abs(dc) == 1
            }
            PieceType.ELEPHANT -> {
                val baseRow = if (piece.side == Side.RED) 9 else 0
                val eyeRow = piece.row + dr / 2
                val eyeCol = piece.col + dc / 2
                targetRow in if (piece.side == Side.RED) 5..9 else 0..4 &&
                    kotlin.math.abs(dr) == 2 && kotlin.math.abs(dc) == 2 &&
                    getPiece(eyeRow, eyeCol) == null
            }
            PieceType.ROOK -> {
                (dr == 0 || dc == 0) && dr + dc != 0 &&
                    if (dr == 0) countPiecesBetweenInRow(piece.row, piece.col, targetCol) == 0
                    else countPiecesBetweenInCol(piece.col, piece.row, targetRow) == 0
            }
            PieceType.KNIGHT -> {
                val absDr = kotlin.math.abs(dr)
                val absDc = kotlin.math.abs(dc)
                if (!((absDr == 2 && absDc == 1) || (absDr == 1 && absDc == 2))) return false
                val legRow = if (absDr == 2) piece.row + dr / 2 else piece.row
                val legCol = if (absDc == 2) piece.col + dc / 2 else piece.col
                getPiece(legRow, legCol) == null
            }
            PieceType.CANNON -> {
                if (dr != 0 && dc != 0) return false
                val between = if (dr == 0) countPiecesBetweenInRow(piece.row, piece.col, targetCol)
                else countPiecesBetweenInCol(piece.col, piece.row, targetRow)
                val target = getPiece(targetRow, targetCol)
                if (target == null) between == 0
                else between == 1
            }
            PieceType.PAWN -> {
                val forward = if (piece.side == Side.RED) -1 else 1
                val crossed = isCrossRiver(piece.row, piece.side)
                if (crossed) {
                    (dr == forward && dc == 0) || (dr == 0 && kotlin.math.abs(dc) == 1)
                } else {
                    dr == forward && dc == 0
                }
            }
        }
    }

    private fun isKingExposedAfterMove(piece: Piece, toRow: Int, toCol: Int): Boolean {
        val savedRow = piece.row
        val savedCol = piece.col
        val target = getPiece(toRow, toCol)

        val idx = pieces.indexOfFirst { it === piece }
        pieces[idx] = piece.copy(row = toRow, col = toCol)
        if (target != null) pieces.removeAll { it.row == toRow && it.col == toCol }

        val facing = kingsAreFacing()

        pieces[idx] = piece.copy(row = savedRow, col = savedCol)
        if (target != null) pieces.add(target)

        return facing
    }

    fun isCheckmate(side: Side): Boolean {
        if (!isInCheck(side)) return false
        return getAllLegalMoves(side).isEmpty()
    }

    fun isStalemate(side: Side): Boolean {
        if (isInCheck(side)) return false
        return getAllLegalMoves(side).isEmpty()
    }

    fun getAllLegalMoves(side: Side): List<Move> {
        val moves = mutableListOf<Move>()
        for (piece in getPiecesBySide(side)) {
            moves.addAll(getLegalMovesForPiece(piece))
        }
        return moves
    }

    fun getLegalMovesForPiece(piece: Piece): List<Move> {
        val candidates = getCandidateMoves(piece)
        return candidates.filter { move ->
            val captured = getPiece(move.toRow, move.toCol)
            makeMove(move)
            val inCheck = isInCheck(piece.side)
            undoMove(move)
            !inCheck
        }
    }

    fun getCandidateMoves(piece: Piece): List<Move> {
        val moves = mutableListOf<Move>()
        when (piece.type) {
            PieceType.KING -> {
                val dirs = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
                for ((dr, dc) in dirs) {
                    val nr = piece.row + dr
                    val nc = piece.col + dc
                    if (isInsidePalace(nr, nc, piece.side)) {
                        val target = getPiece(nr, nc)
                        if (target == null || target.side != piece.side) {
                            moves.add(Move(piece.row, piece.col, nr, nc, target))
                        }
                    }
                }
                // Flying general rule
                val oppKing = findKing(piece.side.opposite())
                if (oppKing != null && oppKing.col == piece.col) {
                    if (countPiecesBetweenInCol(piece.col, piece.row, oppKing.row) == 0) {
                        moves.add(Move(piece.row, piece.col, oppKing.row, oppKing.col, oppKing))
                    }
                }
            }
            PieceType.ADVISOR -> {
                val dirs = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
                for ((dr, dc) in dirs) {
                    val nr = piece.row + dr
                    val nc = piece.col + dc
                    if (isInsidePalace(nr, nc, piece.side)) {
                        val target = getPiece(nr, nc)
                        if (target == null || target.side != piece.side) {
                            moves.add(Move(piece.row, piece.col, nr, nc, target))
                        }
                    }
                }
            }
            PieceType.ELEPHANT -> {
                val dirs = listOf(-2 to -2, -2 to 2, 2 to -2, 2 to 2)
                val validRowRange = if (piece.side == Side.RED) 5..9 else 0..4
                for ((dr, dc) in dirs) {
                    val nr = piece.row + dr
                    val nc = piece.col + dc
                    val eyeR = piece.row + dr / 2
                    val eyeC = piece.col + dc / 2
                    if (nr in validRowRange && nc in 0..8 &&
                        getPiece(eyeR, eyeC) == null
                    ) {
                        val target = getPiece(nr, nc)
                        if (target == null || target.side != piece.side) {
                            moves.add(Move(piece.row, piece.col, nr, nc, target))
                        }
                    }
                }
            }
            PieceType.ROOK -> {
                val dirs = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
                for ((dr, dc) in dirs) {
                    var r = piece.row + dr
                    var c = piece.col + dc
                    while (isOnBoard(r, c)) {
                        val target = getPiece(r, c)
                        if (target == null) {
                            moves.add(Move(piece.row, piece.col, r, c))
                        } else {
                            if (target.side != piece.side) {
                                moves.add(Move(piece.row, piece.col, r, c, target))
                            }
                            break
                        }
                        r += dr
                        c += dc
                    }
                }
            }
            PieceType.KNIGHT -> {
                val jumps = listOf(
                    -2 to -1, -2 to 1, 2 to -1, 2 to 1,
                    -1 to -2, -1 to 2, 1 to -2, 1 to 2
                )
                for ((dr, dc) in jumps) {
                    val nr = piece.row + dr
                    val nc = piece.col + dc
                    if (!isOnBoard(nr, nc)) continue
                    val legR = if (kotlin.math.abs(dr) == 2) piece.row + dr / 2 else piece.row
                    val legC = if (kotlin.math.abs(dc) == 2) piece.col + dc / 2 else piece.col
                    if (getPiece(legR, legC) != null) continue
                    val target = getPiece(nr, nc)
                    if (target == null || target.side != piece.side) {
                        moves.add(Move(piece.row, piece.col, nr, nc, target))
                    }
                }
            }
            PieceType.CANNON -> {
                val dirs = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
                for ((dr, dc) in dirs) {
                    var r = piece.row + dr
                    var c = piece.col + dc
                    var jumped = false
                    while (isOnBoard(r, c)) {
                        val target = getPiece(r, c)
                        if (!jumped) {
                            if (target == null) {
                                moves.add(Move(piece.row, piece.col, r, c))
                            } else {
                                jumped = true
                            }
                        } else {
                            if (target != null) {
                                if (target.side != piece.side) {
                                    moves.add(Move(piece.row, piece.col, r, c, target))
                                }
                                break
                            }
                        }
                        r += dr
                        c += dc
                    }
                }
            }
            PieceType.PAWN -> {
                val forward = if (piece.side == Side.RED) -1 else 1
                val crossed = isCrossRiver(piece.row, piece.side)

                // Forward
                val nr = piece.row + forward
                if (isOnBoard(nr, piece.col)) {
                    val target = getPiece(nr, piece.col)
                    if (target == null || target.side != piece.side) {
                        moves.add(Move(piece.row, piece.col, nr, piece.col, target))
                    }
                }

                // Sideways after crossing river
                if (crossed) {
                    for (dc in listOf(-1, 1)) {
                        val nc = piece.col + dc
                        if (isOnBoard(piece.row, nc)) {
                            val target = getPiece(piece.row, nc)
                            if (target == null || target.side != piece.side) {
                                moves.add(Move(piece.row, piece.col, piece.row, nc, target))
                            }
                        }
                    }
                }
            }
        }
        return moves
    }
}
