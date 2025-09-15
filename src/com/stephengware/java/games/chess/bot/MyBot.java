package com.stephengware.java.games.chess.bot;

import com.stephengware.java.games.chess.state.*;

import com.stephengware.java.games.chess.Game.*;

import com.stephengware.java.games.chess.state.State.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyBot extends Bot {

    private final Map<Long, TTEntry> transTable = new HashMap<>();
    private int nodeCount;
    private static final int MAX_NODES = 50000;

    private static final int PAWN_VALUE = 100;
    private static final int KNIGHT_VALUE = 320;
    private static final int BISHOP_VALUE = 330;
    private static final int ROOK_VALUE = 500;
    private static final int QUEEN_VALUE = 900;
    // private static final int KING_VALUE = 20000;

    private static final int MAX_DEPTH = 3;
    private static final int INFINITY = 9999999;

    // Piece-square tables (PST)
    private static final int[][] PAWN_PST = {
            { 0, 0, 0, 0, 0, 0, 0, 0 },
            { 50, 50, 50, 50, 50, 50, 50, 50 },
            { 10, 10, 20, 30, 30, 20, 10, 10 },
            { 5, 5, 10, 25, 25, 10, 5, 5 },
            { 0, 0, 0, 20, 20, 0, 0, 0 },
            { 5, -5, -10, 0, 0, -10, -5, 5 },
            { 5, 10, 10, -20, -20, 10, 10, 5 },
            { 0, 0, 0, 0, 0, 0, 0, 0 }
    };
    private static final int[][] PAWN_ENDGAME_PST = {
            { 0, 0, 0, 0, 0, 0, 0, 0 }, // rank 0 (home rank, should never happen)
            { 10, 10, 10, 10, 10, 10, 10, 10 }, // rank 1
            { 20, 20, 20, 20, 20, 20, 20, 20 }, // rank 2
            { 30, 30, 30, 30, 30, 30, 30, 30 }, // rank 3
            { 50, 50, 50, 50, 50, 50, 50, 50 }, // rank 4
            { 70, 70, 70, 70, 70, 70, 70, 70 }, // rank 5
            { 90, 90, 90, 90, 90, 90, 90, 90 }, // rank 6
            { 0, 0, 0, 0, 0, 0, 0, 0 } // rank 7 (promotion achieved)
    };

    private static final int[] PASSED_PAWN_BONUS = { 0, 5, 10, 20, 40, 60, 80, 0 };

    // ... (keep other PSTs as-is, no changes needed)
    private static final int[][] KNIGHT_PST = {
            { -50, -40, -30, -30, -30, -30, -40, -50 },
            { -40, -20, 0, 0, 0, 0, -20, -40 },
            { -30, 0, 10, 15, 15, 10, 0, -30 },
            { -30, 5, 15, 20, 20, 15, 5, -30 },
            { -30, 0, 15, 20, 20, 15, 0, -30 },
            { -30, 5, 10, 15, 15, 10, 5, -30 },
            { -40, -20, 0, 5, 5, 0, -20, -40 },
            { -50, -40, -30, -30, -30, -30, -40, -50 }
    };

    private static final int[][] BISHOP_PST = {
            { -20, -10, -10, -10, -10, -10, -10, -20 },
            { -10, 0, 0, 0, 0, 0, 0, -10 },
            { -10, 0, 5, 10, 10, 5, 0, -10 },
            { -10, 5, 5, 10, 10, 5, 5, -10 },
            { -10, 0, 10, 10, 10, 10, 0, -10 },
            { -10, 10, 10, 10, 10, 10, 10, -10 },
            { -10, 5, 0, 0, 0, 0, 5, -10 },
            { -20, -10, -10, -10, -10, -10, -10, -20 }
    };

    private static final int[][] ROOK_PST = {
            { 0, 0, 0, 0, 0, 0, 0, 0 },
            { 5, 10, 10, 10, 10, 10, 10, 5 },
            { -5, 0, 0, 0, 0, 0, 0, -5 },
            { -5, 0, 0, 0, 0, 0, 0, -5 },
            { -5, 0, 0, 0, 0, 0, 0, -5 },
            { -5, 0, 0, 0, 0, 0, 0, -5 },
            { -5, 0, 0, 0, 0, 0, 0, -5 },
            { 0, 0, 0, 5, 5, 0, 0, 0 }
    };

    private static final int[][] QUEEN_PST = {
            { -20, -10, -10, -5, -5, -10, -10, -20 },
            { -10, 0, 0, 0, 0, 0, 0, -10 },
            { -10, 0, 5, 5, 5, 5, 0, -10 },
            { -5, 0, 5, 5, 5, 5, 0, -5 },
            { 0, 0, 5, 5, 5, 5, 0, -5 },
            { -10, 5, 5, 5, 5, 5, 0, -10 },
            { -10, 0, 5, 0, 0, 0, 0, -10 },
            { -20, -10, -10, -5, -5, -10, -10, -20 }
    };

    // private static final int[][] KING_PST = {
    // { -30, -40, -40, -50, -50, -40, -40, -30 },
    // { -30, -40, -40, -50, -50, -40, -40, -30 },
    // { -30, -40, -40, -50, -50, -40, -40, -30 },
    // { -30, -40, -40, -50, -50, -40, -40, -30 },
    // { -20, -30, -30, -40, -40, -30, -30, -20 },
    // { -10, -20, -20, -20, -20, -20, -20, -10 },
    // { 20, 20, 0, 0, 0, 0, 20, 20 },
    // { 20, 30, 10, 0, 0, 10, 30, 20 }
    // };

    // Opening/midgame king PST
    private static final int[][] KING_MID_PST = {
            { -30, -40, -40, -50, -50, -40, -40, -30 },
            { -30, -40, -40, -50, -50, -40, -40, -30 },
            { -30, -40, -40, -50, -50, -40, -40, -30 },
            { -30, -40, -40, -50, -50, -40, -40, -30 },
            { -20, -30, -30, -40, -40, -30, -30, -20 },
            { -10, -20, -20, -20, -20, -20, -20, -10 },
            { 20, 20, 0, 0, 0, 0, 20, 20 },
            { 20, 30, 10, 0, 0, 10, 30, 20 }
    };

    // Endgame king PST
    private static final int[][] KING_END_PST = {
            { -50, -40, -30, -20, -20, -30, -40, -50 },
            { -30, -20, -10, 0, 0, -10, -20, -30 },
            { -30, -10, 20, 30, 30, 20, -10, -30 },
            { -30, -10, 30, 40, 40, 30, -10, -30 },
            { -30, -10, 30, 40, 40, 30, -10, -30 },
            { -30, -10, 20, 30, 30, 20, -10, -30 },
            { -30, -30, 0, 0, 0, 0, -30, -30 },
            { -50, -40, -30, -20, -20, -30, -40, -50 }
    };

    public MyBot() {
        super("MyBot");
    }

    // private static class TTEntry {
    // final int value;
    // final int depth;

    // TTEntry(int value, int depth) {
    // this.value = value;
    // this.depth = depth;
    // }
    // }
    enum BoundType {
        EXACT, LOWER, UPPER
    }

    class TTEntry {
        int value;
        int depth;
        BoundType flag;

        TTEntry(int value, int depth, BoundType flag) {
            this.value = value;
            this.depth = depth;
            this.flag = flag;
        }
    }

    @Override
    protected State chooseMove(State root) {

        return iterativeDeepeningSearch(root);
    }

    private State iterativeDeepeningSearch(State root) {
        State bestMove = null;
        Player botPlayer = root.player;
        // System.out.println("Bot is playing as: " + botPlayer);

        for (int depth = 1; depth <= MAX_DEPTH; depth++) {
            nodeCount = 0; // reset node counter for this depth
            State currentBest = null;
            int bestValue = -INFINITY;

            boolean completed = true; // track if we completed the search at this depth

            for (State child : root.next()) {
                if (nodeCount > MAX_NODES) {
                    completed = false;
                    break; // stop if node limit reached
                }
                // System.out.println("Evaluating move at depth " + depth + ":");
                // System.out.println(child.board.toString());

                int value = minimax(child, depth - 1, false, botPlayer, -INFINITY, INFINITY);

                // System.out.println("Depth " + depth + " evaluated move: " + child + " with
                // eval: " + value);

                if (value > bestValue || currentBest == null) {
                    bestValue = value;
                    currentBest = child;
                }
            }

            // Only update the bestMove if this depth completed fully
            if (completed && currentBest != null) {
                bestMove = currentBest;
                // System.out.println("Depth " + depth + " completed. Best move: " + bestMove +
                // " with eval: " + bestValue);
            }

            if (!completed)
                break; // stop deeper searches if node limit hit
        }

        return bestMove;
    }

    // private int minimax(State state, int depth, boolean maximizingPlayer, Player
    // botPlayer, int alpha, int beta) {
    // nodeCount++;

    // // Terminal or depth cutoff
    // if (nodeCount > MAX_NODES || depth == 0 || state.over) {
    // return evaluateState(state, botPlayer);
    // }

    // // Lookup in TT
    // long hash = computeStateHash(state);
    // TTEntry entry = transTable.get(hash);
    // if (entry != null && entry.depth >= depth) {
    // return entry.value; // reuse stored evaluation
    // }

    // int result = alphaBetaPrune(state, depth, maximizingPlayer, botPlayer, alpha,
    // beta);

    // // Store in TT (replace only if deeper)
    // if (entry == null || depth > entry.depth) {
    // transTable.put(hash, new TTEntry(result, depth));
    // }  

    // return result;
    // }

    private int minimax(State state, int depth, boolean maximizingPlayer, Player botPlayer, int alpha, int beta) {
        nodeCount++;

        // --- Terminal condition ---
        if (nodeCount > MAX_NODES || depth == 0 || state.over) {
            return evaluateState(state, botPlayer);
        }

        long hash = computeStateHash(state);
        TTEntry entry = transTable.get(hash);

        int originalAlpha = alpha;
        int originalBeta = beta;

        // --- TT probe ---
        if (entry != null && entry.depth >= depth) {
            if (entry.flag == BoundType.EXACT) {
                return entry.value;
            } else if (entry.flag == BoundType.LOWER) {
                alpha = Math.max(alpha, entry.value);
            } else if (entry.flag == BoundType.UPPER) {
                beta = Math.min(beta, entry.value);
            }

            if (alpha >= beta) {
                return entry.value; // cutoff
            }
        }

        int bestValue = maximizingPlayer ? -INFINITY : INFINITY;

        // --- Search children ---
        for (State child : state.next()) {
            int eval = minimax(child, depth - 1, !maximizingPlayer, botPlayer, alpha, beta);

            if (maximizingPlayer) {
                bestValue = Math.max(bestValue, eval);
                alpha = Math.max(alpha, eval);
            } else {
                bestValue = Math.min(bestValue, eval);
                beta = Math.min(beta, eval);
            }

            if (beta <= alpha)
                break; // alpha-beta cutoff
        }

        // --- Store in TT ---
        BoundType flag;
        if (bestValue <= originalAlpha) {
            flag = BoundType.UPPER;
        } else if (bestValue >= originalBeta) {
            flag = BoundType.LOWER;
        } else {
            flag = BoundType.EXACT;
        }

        if (entry == null || depth >= entry.depth) {
            transTable.put(hash, new TTEntry(bestValue, depth, flag));
        }

        return bestValue;
    }

    //
    private long computeStateHash(State state) {
        long hash = 0;
        int i = 0;
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                Piece piece = state.board.getPieceAt(file, rank);
                if (piece != null)
                    hash ^= piece.toString().hashCode() * 31L * (i + 1);
                i++;
            }
        }
        // Encode side to move only
        hash ^= (state.player == Player.WHITE ? 1L : 2L) * 1000003L;
        return hash;
    }

    private int evaluateState(State state, Player botPlayer) {
        // --- 1. Game-over states ---
        if (state.board.getKing(botPlayer) == null)
            return -INFINITY; // Bot lost
        if (state.board.getKing(botPlayer.other()) == null)
            return INFINITY; // Opponent lost

        int score = 0;

        // --- 2. Material + PST + center control ---
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                Piece piece = state.board.getPieceAt(file, rank);
                if (piece == null)
                    continue;

                int value = getPieceValue(piece);


                int pst = getPieceSquareValue(piece, state, botPlayer);

                // Bonus for being close to center (weighted stronger than PST)
                int centerBonus = 14 - (Math.abs(piece.file - 3) + Math.abs(piece.rank - 3));

                if (piece.player == botPlayer) {
                    score += value + pst + centerBonus * 2;
                } else {
                    score -= value + pst + centerBonus * 2;
                }
            }
        }

        // --- 3. Mobility heuristic ---
        int ourMoves = 0;
        int theirMoves = 0;
        for (State child : state.next()) {
            if (state.player == botPlayer)
                ourMoves++;
            else
                theirMoves++;
        }
        score += (ourMoves - theirMoves) * 5;

        // --- 4. Endgame king activity heuristic ---
        score += ForceKingToCornerEndgameEval(state, botPlayer, botPlayer);
        score -= ForceKingToCornerEndgameEval(state, botPlayer.other(), botPlayer);

        return score;
    }

    private int getPieceSquareValue(Piece piece, State state, Player botPlayer) {
        int file = piece.file;
        int rank = piece.rank;
        if (file < 0 || file > 7 || rank < 0 || rank > 7)
            return 0;

        // Flip rank for black pieces to match PST orientation
        if (piece.player == Player.BLACK)
            rank = 7 - rank;

        String pieceStr = piece.toString().toLowerCase();

        // --- Pawn ---
        if (pieceStr.contains("pawn") || pieceStr.contains("p")) {
            int pstValue = PAWN_PST[rank][file]; // default midgame PST

            double endgameWeight = getEndgameWeight(state, botPlayer);

            // Smooth interpolation instead of hard switch
            pstValue = (int) (PAWN_PST[rank][file] * (1 - endgameWeight)
                    + PAWN_ENDGAME_PST[rank][file] * endgameWeight);

            if (isPassedPawn(piece, state)) {
                pstValue += PASSED_PAWN_BONUS[rank];
            }

            return pstValue;
        }

        // --- Knight ---
        if (pieceStr.contains("knight") || pieceStr.contains("n"))
            return KNIGHT_PST[rank][file];

        // --- Bishop ---
        if (pieceStr.contains("bishop") || pieceStr.contains("b"))
            return BISHOP_PST[rank][file];

        // --- Rook ---
        if (pieceStr.contains("rook") || pieceStr.contains("r"))
            return ROOK_PST[rank][file];

        // --- Queen ---
        if (pieceStr.contains("queen") || pieceStr.contains("q"))
            return QUEEN_PST[rank][file];

        // --- King ---
        if (pieceStr.contains("king") || pieceStr.contains("k")) {
            double endgameWeight = getEndgameWeight(state, botPlayer);
            return (int) (KING_MID_PST[rank][file] * (1 - endgameWeight)
                    + KING_END_PST[rank][file] * endgameWeight);
        }

        return 0; // fallback
    }

    private int getPieceValue(Piece piece) {
        String pieceStr = piece.toString().toLowerCase();
        if (pieceStr.contains("pawn") || pieceStr.contains("p"))
            return PAWN_VALUE;
        if (pieceStr.contains("knight") || pieceStr.contains("n"))
            return KNIGHT_VALUE;
        if (pieceStr.contains("bishop") || pieceStr.contains("b"))
            return BISHOP_VALUE;
        if (pieceStr.contains("rook") || pieceStr.contains("r"))
            return ROOK_VALUE;
        if (pieceStr.contains("queen") || pieceStr.contains("q"))
            return QUEEN_VALUE;
        return 0;
    }

    /**
     * Count all pieces on the board.
     */
    private int countPieces(State state) {
        return state.board.countPieces();
    }

    /**
     * Count all queens on the board.
     */
    private int countQueens(State state) {
        int count = 0;
        for (Piece piece : state.board) {
            if (piece instanceof Queen)
                count++;
        }
        return count;
    }

    /**
     * Returns a weight for endgame transition: 0 = midgame, 1 = endgame.
     */
    // private double getEndgameWeight(State state) {
    // Player botPlayer = state.player; // or pass as parameter if needed
    // int enemyPieces = 0;
    // //
    // for (Piece piece : state.board) {
    // if (piece.player != botPlayer) {
    // enemyPieces++;
    // }
    // }

    // // int queens = 0;
    // // for (Piece piece : state.board) {
    // // if (piece.player != botPlayer && piece instanceof Queen) {
    // // queens++;
    // // }
    // // }

    // double weight = 0.0;

    // // Heuristic: endgame starts when enemy pieces <= 6 OR no enemy queens
    // if (enemyPieces <= 6 ) {
    // weight = 1.0;
    // System.out.println("Endgame detected: enemyPieces=" + enemyPieces + ",
    // queens=" + queens);
    // } else if (enemyPieces <= 12) {
    // // gradual transition
    // weight = (12 - enemyPieces) / 6.0; // maps 12->0, 6->1
    // }

    // return Math.min(1.0, Math.max(0.0, weight));
    // }

    private double getEndgameWeight(State state, Player botPlayer) {
        if (state == null || state.board == null)
            return 0.0;

        int enemyPieces = 0;
        int enemyQueens = 0;
        Player enemy = botPlayer.other();

        // Count enemy pieces and queens
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                Piece piece = state.board.getPieceAt(file, rank);
                if (piece == null || piece.player != enemy)
                    continue;

                enemyPieces++;
                if (piece.toString().toLowerCase().contains("q"))
                    enemyQueens++;
                // System.out.println("Enemy queen number:" + enemyQueens);
            }
        }

        double weight = 0.0;

        // Endgame heuristic
        if (enemyPieces <= 6 || enemyQueens == 0) {
            weight = 1.0;
        } else if (enemyPieces <= 12) {
            // Gradual transition
            weight = (12 - enemyPieces) / 6.0;
        }

        return Math.min(1.0, Math.max(0.0, weight));
    }

    private int ForceKingToCornerEndgameEval(State state, Player player, Player botPlayer) {
        if (!isEndGame(state, botPlayer))
            return 0; // only in endgame

        King king = state.board.getKing(player);
        if (king == null)
            return 0;

        int rank = king.rank;
        int file = king.file;

        // Corners: (0,0), (0,7), (7,0), (7,7)
        int minDistToCorner = Math.min(
                Math.min(rank + file, rank + (7 - file)),
                Math.min((7 - rank) + file, (7 - rank) + (7 - file)));

        // Closer to corner = higher bonus
        return (7 - minDistToCorner) * 10; // scale factor
    }

    private boolean isPassedPawn(Piece pawn, State state) {
        if (pawn == null || !pawn.toString().toLowerCase().contains("pawn"))
            return false;

        int file = pawn.file;
        int rank = pawn.rank;
        Player enemy = pawn.player.other();

        int direction = (pawn.player == Player.WHITE) ? 1 : -1;

        // Check all enemy pawns in front on the same and adjacent files
        for (int r = rank + direction; r >= 0 && r < 8; r += direction) {
            for (int f = file - 1; f <= file + 1; f++) {
                if (f < 0 || f > 7)
                    continue;
                Piece p = state.board.getPieceAt(f, r);
                if (p != null && p.player == enemy && p.toString().toLowerCase().contains("pawn")) {
                    return false; // blocked by enemy pawn
                }
            }
        }

        return true; // no enemy pawns ahead
    }

    /**
     * Detect if game is midgame.
     */
    private boolean isMidGame(State state, Player botPlayer) {
        return getEndgameWeight(state, botPlayer) < 0.5;
    }

    /**
     * Detect if game is endgame.
     */
    private boolean isEndGame(State state, Player botPlayer) {
        return getEndgameWeight(state, botPlayer) >= 0.5;
    }

}