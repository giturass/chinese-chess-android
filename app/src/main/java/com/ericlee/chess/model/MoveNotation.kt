package com.ericlee.chess.model

private val chineseNumbers = listOf("一", "二", "三", "四", "五", "六", "七", "八", "九")

fun Move.toChineseNotation(): String {
    val movingSide = side ?: Side.RED
    val type = pieceType ?: PieceType.PAWN
    val pieceName = movingSide.pieceChar(type)
    val fromFile = fileName(fromCol, movingSide)
    val action = actionName(movingSide)
    val amount = if (action == "平" || type in listOf(PieceType.KNIGHT, PieceType.ADVISOR, PieceType.ELEPHANT)) {
        fileName(toCol, movingSide)
    } else {
        chineseNumbers[(kotlin.math.abs(toRow - fromRow) - 1).coerceIn(0, 8)]
    }
    val sideName = if (movingSide == Side.RED) "红" else "黑"
    return "$sideName $pieceName$fromFile$action$amount"
}

fun List<Move>.toMoveLines(): List<String> = mapIndexed { index, move ->
    "${index + 1}. ${move.toChineseNotation()}"
}

private fun Move.actionName(side: Side): String = when {
    fromCol != toCol && fromRow == toRow -> "平"
    side == Side.RED && toRow < fromRow -> "进"
    side == Side.BLACK && toRow > fromRow -> "进"
    else -> "退"
}

private fun fileName(col: Int, side: Side): String {
    val file = if (side == Side.RED) 9 - col else col + 1
    return chineseNumbers[(file - 1).coerceIn(0, 8)]
}
