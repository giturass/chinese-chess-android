package com.ericlee.chess.model

data class EndgamePuzzle(
    val id: Int,
    val name: String,
    val difficulty: Int,
    val description: String,
    val pieces: List<PuzzlePiece>,
    val hint: String,
    val category: String = "经典杀法",
    val goal: String = "红先取胜"
)

data class PuzzlePiece(
    val side: String,
    val type: String,
    val row: Int,
    val col: Int
) {
    fun toPiece(): Piece {
        val sideEnum = if (side == "red") Side.RED else Side.BLACK
        val typeEnum = when (type) {
            "king" -> PieceType.KING
            "advisor" -> PieceType.ADVISOR
            "elephant" -> PieceType.ELEPHANT
            "rook" -> PieceType.ROOK
            "knight" -> PieceType.KNIGHT
            "cannon" -> PieceType.CANNON
            "pawn" -> PieceType.PAWN
            else -> PieceType.PAWN
        }
        return Piece(sideEnum, typeEnum, row, col)
    }
}
