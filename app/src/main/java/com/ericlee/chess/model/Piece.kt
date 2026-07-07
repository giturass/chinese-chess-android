package com.ericlee.chess.model

enum class PieceType(val label: String) {
    KING("帅"),      // 帅/将
    ADVISOR("士"),   // 士
    ELEPHANT("象"),  // 相/象
    ROOK("车"),      // 车
    KNIGHT("马"),    // 马
    CANNON("炮"),    // 炮
    PAWN("兵")       // 兵/卒
}

enum class Side {
    RED, BLACK;

    fun opposite(): Side = if (this == RED) BLACK else RED

    fun kingChar(): String = if (this == RED) "帅" else "将"
    fun advisorChar(): String = "士"
    fun elephantChar(): String = if (this == RED) "相" else "象"
    fun rookChar(): String = "车"
    fun knightChar(): String = "马"
    fun cannonChar(): String = "炮"
    fun pawnChar(): String = if (this == RED) "兵" else "卒"

    fun pieceChar(type: PieceType): String = when (type) {
        PieceType.KING -> kingChar()
        PieceType.ADVISOR -> advisorChar()
        PieceType.ELEPHANT -> elephantChar()
        PieceType.ROOK -> rookChar()
        PieceType.KNIGHT -> knightChar()
        PieceType.CANNON -> cannonChar()
        PieceType.PAWN -> pawnChar()
    }
}

data class Piece(
    val side: Side,
    val type: PieceType,
    val row: Int,
    val col: Int
) {
    val char: String get() = side.pieceChar(type)

    companion object {
        fun initialPieces(): List<Piece> {
            val pieces = mutableListOf<Piece>()

            // Red pieces (bottom, rows 7-9)
            // Row 9 (back row)
            pieces.add(Piece(Side.RED, PieceType.ROOK, 9, 0))
            pieces.add(Piece(Side.RED, PieceType.KNIGHT, 9, 1))
            pieces.add(Piece(Side.RED, PieceType.ELEPHANT, 9, 2))
            pieces.add(Piece(Side.RED, PieceType.ADVISOR, 9, 3))
            pieces.add(Piece(Side.RED, PieceType.KING, 9, 4))
            pieces.add(Piece(Side.RED, PieceType.ADVISOR, 9, 5))
            pieces.add(Piece(Side.RED, PieceType.ELEPHANT, 9, 6))
            pieces.add(Piece(Side.RED, PieceType.KNIGHT, 9, 7))
            pieces.add(Piece(Side.RED, PieceType.ROOK, 9, 8))
            // Row 7 (cannon row)
            pieces.add(Piece(Side.RED, PieceType.CANNON, 7, 1))
            pieces.add(Piece(Side.RED, PieceType.CANNON, 7, 7))
            // Row 6 (pawn row)
            pieces.add(Piece(Side.RED, PieceType.PAWN, 6, 0))
            pieces.add(Piece(Side.RED, PieceType.PAWN, 6, 2))
            pieces.add(Piece(Side.RED, PieceType.PAWN, 6, 4))
            pieces.add(Piece(Side.RED, PieceType.PAWN, 6, 6))
            pieces.add(Piece(Side.RED, PieceType.PAWN, 6, 8))

            // Black pieces (top, rows 0-2)
            // Row 0 (back row)
            pieces.add(Piece(Side.BLACK, PieceType.ROOK, 0, 0))
            pieces.add(Piece(Side.BLACK, PieceType.KNIGHT, 0, 1))
            pieces.add(Piece(Side.BLACK, PieceType.ELEPHANT, 0, 2))
            pieces.add(Piece(Side.BLACK, PieceType.ADVISOR, 0, 3))
            pieces.add(Piece(Side.BLACK, PieceType.KING, 0, 4))
            pieces.add(Piece(Side.BLACK, PieceType.ADVISOR, 0, 5))
            pieces.add(Piece(Side.BLACK, PieceType.ELEPHANT, 0, 6))
            pieces.add(Piece(Side.BLACK, PieceType.KNIGHT, 0, 7))
            pieces.add(Piece(Side.BLACK, PieceType.ROOK, 0, 8))
            // Row 2 (cannon row)
            pieces.add(Piece(Side.BLACK, PieceType.CANNON, 2, 1))
            pieces.add(Piece(Side.BLACK, PieceType.CANNON, 2, 7))
            // Row 3 (pawn row)
            pieces.add(Piece(Side.BLACK, PieceType.PAWN, 3, 0))
            pieces.add(Piece(Side.BLACK, PieceType.PAWN, 3, 2))
            pieces.add(Piece(Side.BLACK, PieceType.PAWN, 3, 4))
            pieces.add(Piece(Side.BLACK, PieceType.PAWN, 3, 6))
            pieces.add(Piece(Side.BLACK, PieceType.PAWN, 3, 8))

            return pieces
        }
    }
}
