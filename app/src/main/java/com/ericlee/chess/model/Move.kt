package com.ericlee.chess.model

data class Move(
    val fromRow: Int,
    val fromCol: Int,
    val toRow: Int,
    val toCol: Int,
    val captured: Piece? = null,
    val side: Side? = null
) {
    override fun toString(): String {
        val from = "(${fromRow},${fromCol})"
        val to = "(${toRow},${toCol})"
        val cap = if (captured != null) " capture=${captured.char}" else ""
        val mover = if (side != null) " side=$side" else ""
        return "$from->$to$cap$mover"
    }
}
