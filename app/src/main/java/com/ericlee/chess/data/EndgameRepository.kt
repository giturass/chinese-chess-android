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
        createPuzzle10(),
        createPuzzle11(),
        createPuzzle12(),
        createPuzzle13(),
        createPuzzle14(),
        createPuzzle15(),
        createPuzzle16()
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
        hint = "双车交替将军，注意控制将门",
        category = "基础杀法",
        goal = "红先速胜"
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
        hint = "马跳到将门位置，炮借助马做炮架将军",
        category = "基础杀法",
        goal = "红先成杀"
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
        hint = "车控制将门，炮直线将军",
        category = "基础杀法",
        goal = "红先取胜"
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
        hint = "弃车砍中士，制造空头炮",
        category = "弃子攻杀",
        goal = "红先破士"
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
        hint = "双马交替跳入将门",
        category = "马炮杀法",
        goal = "红先取势"
    )

    private fun createPuzzle6() = EndgamePuzzle(
        id = 6,
        name = "天地炮",
        difficulty = 3,
        description = "双炮一上一下，配合车取胜",
        pieces = listOf(
            PuzzlePiece("red", "king", 9, 4),
            PuzzlePiece("red", "cannon", 2, 4),
            PuzzlePiece("red", "cannon", 5, 4),
            PuzzlePiece("red", "rook", 7, 0),
            PuzzlePiece("black", "king", 0, 4),
            PuzzlePiece("black", "advisor", 0, 3),
            PuzzlePiece("black", "advisor", 0, 5),
            PuzzlePiece("black", "rook", 5, 8)
        ),
        solution = listOf("70_00"),
        hint = "天地炮控制中线，车从侧翼进攻",
        category = "车炮配合",
        goal = "红先攻杀"
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
        hint = "三兵逐步逼近将门",
        category = "兵卒残局",
        goal = "红先推进"
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
        hint = "车沉底将军，炮借助车抽将",
        category = "车炮配合",
        goal = "红先抽杀"
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
        hint = "马跳卧槽位控制将门",
        category = "马炮杀法",
        goal = "红先入局"
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
        hint = "利用炮做架子，车控制将门",
        category = "经典残局",
        goal = "红先巧胜"
    )

    private fun createPuzzle11() = EndgamePuzzle(
        id = 11,
        name = "空头炮",
        difficulty = 2,
        description = "炮镇中路，配合仕帅控制将门",
        pieces = listOf(
            PuzzlePiece("red", "king", 9, 4),
            PuzzlePiece("red", "advisor", 9, 3),
            PuzzlePiece("red", "cannon", 4, 4),
            PuzzlePiece("red", "pawn", 2, 4),
            PuzzlePiece("black", "king", 0, 4),
            PuzzlePiece("black", "advisor", 0, 3),
            PuzzlePiece("black", "advisor", 0, 5),
            PuzzlePiece("black", "rook", 5, 8)
        ),
        solution = listOf("44_14"),
        hint = "先让炮占中线，借中兵成炮架压住黑将",
        category = "车炮配合",
        goal = "红先压将"
    )

    private fun createPuzzle12() = EndgamePuzzle(
        id = 12,
        name = "挂角马",
        difficulty = 3,
        description = "马挂九宫角，限制黑将腾挪",
        pieces = listOf(
            PuzzlePiece("red", "king", 9, 4),
            PuzzlePiece("red", "knight", 4, 2),
            PuzzlePiece("red", "rook", 7, 4),
            PuzzlePiece("black", "king", 0, 4),
            PuzzlePiece("black", "advisor", 1, 5),
            PuzzlePiece("black", "elephant", 2, 6)
        ),
        solution = listOf("42_21"),
        hint = "马向九宫角靠近，车守中路形成钳制",
        category = "马炮杀法",
        goal = "红先入局"
    )

    private fun createPuzzle13() = EndgamePuzzle(
        id = 13,
        name = "双炮叠影",
        difficulty = 4,
        description = "双炮轮番借子，打开中路杀机",
        pieces = listOf(
            PuzzlePiece("red", "king", 9, 4),
            PuzzlePiece("red", "cannon", 3, 1),
            PuzzlePiece("red", "cannon", 5, 4),
            PuzzlePiece("red", "rook", 8, 0),
            PuzzlePiece("black", "king", 0, 4),
            PuzzlePiece("black", "advisor", 0, 3),
            PuzzlePiece("black", "advisor", 0, 5),
            PuzzlePiece("black", "pawn", 2, 4)
        ),
        solution = listOf("31_01"),
        hint = "侧炮先入底线，迫使黑方九宫受限",
        category = "车炮配合",
        goal = "红先逼宫"
    )

    private fun createPuzzle14() = EndgamePuzzle(
        id = 14,
        name = "兵临城下",
        difficulty = 3,
        description = "过河兵贴近九宫，配合老帅取胜",
        pieces = listOf(
            PuzzlePiece("red", "king", 9, 4),
            PuzzlePiece("red", "pawn", 2, 3),
            PuzzlePiece("red", "pawn", 2, 5),
            PuzzlePiece("red", "rook", 6, 4),
            PuzzlePiece("black", "king", 0, 4),
            PuzzlePiece("black", "advisor", 0, 3),
            PuzzlePiece("black", "advisor", 0, 5)
        ),
        solution = listOf("23_13"),
        hint = "先进边兵，逼黑将进入车的控制线",
        category = "兵卒残局",
        goal = "红先逼和转胜"
    )

    private fun createPuzzle15() = EndgamePuzzle(
        id = 15,
        name = "弃炮取势",
        difficulty = 4,
        description = "先弃一炮破防，再用车马收网",
        pieces = listOf(
            PuzzlePiece("red", "king", 9, 4),
            PuzzlePiece("red", "rook", 6, 6),
            PuzzlePiece("red", "knight", 5, 3),
            PuzzlePiece("red", "cannon", 3, 4),
            PuzzlePiece("black", "king", 0, 4),
            PuzzlePiece("black", "advisor", 0, 3),
            PuzzlePiece("black", "advisor", 0, 5),
            PuzzlePiece("black", "rook", 4, 8)
        ),
        solution = listOf("34_04"),
        hint = "炮先撞中士，引开防守后车马跟进",
        category = "弃子攻杀",
        goal = "红先破防"
    )

    private fun createPuzzle16() = EndgamePuzzle(
        id = 16,
        name = "车马冷着",
        difficulty = 5,
        description = "车马残局，先走静着限制黑车活动",
        pieces = listOf(
            PuzzlePiece("red", "king", 9, 4),
            PuzzlePiece("red", "rook", 5, 4),
            PuzzlePiece("red", "knight", 6, 6),
            PuzzlePiece("black", "king", 0, 4),
            PuzzlePiece("black", "rook", 2, 8),
            PuzzlePiece("black", "advisor", 0, 3),
            PuzzlePiece("black", "elephant", 2, 2)
        ),
        solution = listOf("66_45"),
        hint = "马先占中腹，车保中线，别急于将军",
        category = "经典残局",
        goal = "红先谋势"
    )
}
