package com.ericlee.chess.data

import com.ericlee.chess.model.EndgamePuzzle
import com.ericlee.chess.model.PuzzlePiece

object EndgameRepository {

    fun getPuzzles(): List<EndgamePuzzle> = listOf(
        createPuzzle1(),
        createPuzzle2(),
        createPuzzle3(),
        createPuzzle4(),
        createPuzzle5(),
        createPuzzle6(),
        createPuzzle7(),
        createPuzzle8(),
        createPuzzle9(),
        createPuzzle10()
    )

    private fun createPuzzle1() = EndgamePuzzle(
        id = 1,
        name = "双车错",
        difficulty = 1,
        description = "红方双车配合，连续将军取胜",
        pieces = listOf(
            PuzzlePiece("red", "king", 9, 4),
            PuzzlePiece("red", "rook", 7, 1),
            PuzzlePiece("red", "rook", 7, 7),
            PuzzlePiece("black", "king", 0, 4),
            PuzzlePiece("black", "advisor", 0, 3),
            PuzzlePiece("black", "advisor", 0, 5)
        ),
        solution = listOf("71_01", "77_07"),
        hint = "双车交替将军，注意控制将门"
    )

    private fun createPuzzle2() = EndgamePuzzle(
        id = 2,
        name = "马后炮",
        difficulty = 2,
        description = "经典杀法：马控将门，炮在马后将军",
        pieces = listOf(
            PuzzlePiece("red", "king", 9, 4),
            PuzzlePiece("red", "knight", 6, 3),
            PuzzlePiece("red", "cannon", 4, 3),
            PuzzlePiece("black", "king", 0, 4),
            PuzzlePiece("black", "advisor", 0, 3),
            PuzzlePiece("black", "advisor", 0, 5)
        ),
        solution = listOf("63_43"),
        hint = "马跳到将门位置，炮借助马做炮架将军"
    )

    private fun createPuzzle3() = EndgamePuzzle(
        id = 3,
        name = "铁门栓",
        difficulty = 2,
        description = "车控底线，炮在车后将军",
        pieces = listOf(
            PuzzlePiece("red", "king", 9, 4),
            PuzzlePiece("red", "rook", 8, 4),
            PuzzlePiece("red", "cannon", 6, 4),
            PuzzlePiece("black", "king", 0, 4),
            PuzzlePiece("black", "advisor", 0, 3),
            PuzzlePiece("black", "advisor", 0, 5),
            PuzzlePiece("black", "elephant", 0, 2)
        ),
        solution = listOf("64_04"),
        hint = "车控制将门，炮直线将军"
    )

    private fun createPuzzle4() = EndgamePuzzle(
        id = 4,
        name = "大刀剜心",
        difficulty = 3,
        description = "弃车砍士，制造杀机",
        pieces = listOf(
            PuzzlePiece("red", "king", 9, 4),
            PuzzlePiece("red", "rook", 7, 5),
            PuzzlePiece("red", "cannon", 5, 4),
            PuzzlePiece("red", "pawn", 6, 4),
            PuzzlePiece("black", "king", 0, 4),
            PuzzlePiece("black", "advisor", 0, 3),
            PuzzlePiece("black", "advisor", 0, 5),
            PuzzlePiece("black", "rook", 5, 0)
        ),
        solution = listOf("75_05", "05_04"),
        hint = "弃车砍中士，制造空头炮"
    )

    private fun createPuzzle5() = EndgamePuzzle(
        id = 5,
        name = "双马饮泉",
        difficulty = 3,
        description = "双马配合，交替将军取胜",
        pieces = listOf(
            PuzzlePiece("red", "king", 9, 4),
            PuzzlePiece("red", "knight", 6, 2),
            PuzzlePiece("red", "knight", 6, 6),
            PuzzlePiece("black", "king", 0, 4),
            PuzzlePiece("black", "advisor", 0, 3),
            PuzzlePiece("black", "advisor", 0, 5)
        ),
        solution = listOf("62_43", "66_45"),
        hint = "双马交替跳入将门"
    )

    private fun createPuzzle6() = EndgamePuzzle(
        id = 6,
        name = "天地炮",
        difficulty = 3,
        description = "双炮一上一下，配合车取胜",
        pieces = listOf(
            PuzzlePiece("red", "king", 9, 4),
            PuzzlePiece("red", "cannon", 0, 4),
            PuzzlePiece("red", "cannon", 5, 4),
            PuzzlePiece("red", "rook", 7, 0),
            PuzzlePiece("black", "king", 0, 4),
            PuzzlePiece("black", "advisor", 0, 3),
            PuzzlePiece("black", "advisor", 0, 5),
            PuzzlePiece("black", "rook", 5, 8)
        ),
        solution = listOf("70_00"),
        hint = "天地炮控制中线，车从侧翼进攻"
    )

    private fun createPuzzle7() = EndgamePuzzle(
        id = 7,
        name = "三兵闹士",
        difficulty = 2,
        description = "三个过河兵配合取胜",
        pieces = listOf(
            PuzzlePiece("red", "king", 9, 4),
            PuzzlePiece("red", "pawn", 3, 3),
            PuzzlePiece("red", "pawn", 3, 4),
            PuzzlePiece("red", "pawn", 3, 5),
            PuzzlePiece("black", "king", 0, 4),
            PuzzlePiece("black", "advisor", 0, 3),
            PuzzlePiece("black", "advisor", 0, 5)
        ),
        solution = listOf("33_03", "34_04"),
        hint = "三兵逐步逼近将门"
    )

    private fun createPuzzle8() = EndgamePuzzle(
        id = 8,
        name = "车炮抽杀",
        difficulty = 4,
        description = "车炮配合，抽将取胜",
        pieces = listOf(
            PuzzlePiece("red", "king", 9, 4),
            PuzzlePiece("red", "rook", 5, 0),
            PuzzlePiece("red", "cannon", 5, 4),
            PuzzlePiece("black", "king", 0, 4),
            PuzzlePiece("black", "advisor", 0, 3),
            PuzzlePiece("black", "advisor", 0, 5),
            PuzzlePiece("black", "rook", 3, 8)
        ),
        solution = listOf("50_00"),
        hint = "车沉底将军，炮借助车抽将"
    )

    private fun createPuzzle9() = EndgamePuzzle(
        id = 9,
        name = "卧槽马",
        difficulty = 3,
        description = "马跳卧槽位，配合其他子力取胜",
        pieces = listOf(
            PuzzlePiece("red", "king", 9, 4),
            PuzzlePiece("red", "knight", 4, 6),
            PuzzlePiece("red", "rook", 7, 0),
            PuzzlePiece("black", "king", 0, 4),
            PuzzlePiece("black", "advisor", 0, 3),
            PuzzlePiece("black", "advisor", 0, 5),
            PuzzlePiece("black", "elephant", 0, 2)
        ),
        solution = listOf("46_27"),
        hint = "马跳卧槽位控制将门"
    )

    private fun createPuzzle10() = EndgamePuzzle(
        id = 10,
        name = "海底捞月",
        difficulty = 5,
        description = "经典残局：车炮对车，巧胜",
        pieces = listOf(
            PuzzlePiece("red", "king", 9, 4),
            PuzzlePiece("red", "rook", 4, 4),
            PuzzlePiece("red", "cannon", 2, 4),
            PuzzlePiece("black", "king", 0, 4),
            PuzzlePiece("black", "rook", 3, 0)
        ),
        solution = listOf("44_04"),
        hint = "利用炮做架子，车控制将门"
    )
}
