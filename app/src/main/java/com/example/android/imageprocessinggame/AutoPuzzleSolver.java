package com.example.android.imageprocessinggame;


import android.util.Log;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Random;

import static java.lang.Math.abs;

/**
 *  A simple util class used to solve a puzzle
 *  Created by Neo on 7/11/2017.
 */
class AutoPuzzleSolver {
    private static class MoveLink {
        int lastMove;
        MoveLink prev;
        int linkLength;
        boolean breakPoint; // To simulate breaking up moves into pieces
        MoveLink(int lastMove, MoveLink prev) {
            this.lastMove = lastMove;
            this.prev = prev;
            linkLength = (prev == null) ? 1 : prev.linkLength+1;
            breakPoint = false;
        }
    }

    /**
     * Helper method: convert internal structure MoveLink to more common array structure
     * @param moveLink structure to be converted
     * @return array representation of moveLink
     */
    private static int[] toArray(MoveLink moveLink) {
        int length = (moveLink == null) ? 0 : moveLink.linkLength;
        int [] retMoves = new int[length];
        int ixMove = length;
        while (moveLink != null) {
            retMoves[--ixMove] = moveLink.lastMove;
            moveLink = moveLink.prev;
        }
        return retMoves;
    }


    /**
     * recursiveLy move the puzzle based on moveLink (cycle detection enabled, note it doesn't optimize total reduction on cycles, otherwise needs DP)
     * @param puzzle current Puzzle
     * @param moveLink moveLink
     * @param cycleDict Dictionary for cycle detection
     */
    private static void recursivelyMove(Puzzle puzzle, MoveLink moveLink, HashMap<String, MoveLink> cycleDict) {
        if ((moveLink != null) && (!moveLink.breakPoint)) {
            recursivelyMove(puzzle, moveLink.prev, cycleDict);
            if (moveLink.prev != null) {
                moveLink.linkLength = moveLink.prev.linkLength+1; // since it reduces cycle, needs to check before proceeding
            }
            String key = Arrays.toString(puzzle.getBoardCopy());
            MoveLink cycleMove = cycleDict.get(key);
            if (cycleMove != null) {
                moveLink.prev = cycleMove.prev;
                moveLink.linkLength = cycleMove.linkLength;
            }
            cycleDict.put(key, moveLink);

            puzzle.move(moveLink.lastMove);
        }
    }

    /**
     * pieceWisely solve the puzzle
     * @param orig_puzzle: original puzzle to be solved
     * @return the sequence of positions to move in order to solve the puzzle
     */
     static int[] pieceWiseAutoSolve(Puzzle orig_puzzle) {
        orig_puzzle = new Puzzle(orig_puzzle);
        if (orig_puzzle.getCurrentStatus() == 1) {
            return new int[0];
        }
        HashMap<String,Boolean> existenceDict = new HashMap<>();
        HashMap<String, MoveLink> cycleDict = new HashMap<>();

        int searchNum = orig_puzzle.getBoardCopy().length*3750;

        MoveLink currMoves = simplify(orig_puzzle, null, searchNum,existenceDict);
        while (currMoves != null) {
            recursivelyMove(orig_puzzle, currMoves, cycleDict);
            if (orig_puzzle.getCurrentStatus() == 1) {
                return toArray(currMoves);
            }
            currMoves.breakPoint = true;
            currMoves = simplify(orig_puzzle, currMoves, searchNum, existenceDict);

        }

        Log.d("hello", "Due to Technical Difficulty, Memory is exhausted and no more progress can be made");
        throw new IllegalStateException("Due to Technical Difficulty, Memory is exhausted and no more progress can be made");
    }




    /**
     * Nice idea of simplify the puzzle if not enough memory (simplification comparison is based on heuristic) (Modified A* search, give up optimality for speed)
     * @param curr_puzzle Original puzzle
     * @param prev Prev link (if none then should be null)
     * @return the link of moves (continued from prev) required to simplify the current puzzle, null if no more progress to be made
     */
    private static MoveLink  simplify(Puzzle curr_puzzle, MoveLink prev, int maxSize, HashMap<String, Boolean> existenceDict) {
        // use Manhattan Distance as heuristic function
        if (curr_puzzle.getCurrentStatus() == 1) {
            return null;
        }
        int orig_width = curr_puzzle.getWidth();
        Comparator<ItemConfig> comparator = new ItemConfigComparator();
        PriorityQueue<ItemConfig> queue = new PriorityQueue<>(10, comparator);
        MoveLink currMinHMoves = null;
        int currMinH = totalManhattan(curr_puzzle.getBoardCopy(), orig_width);
        queue.add(new ItemConfig(curr_puzzle, currMinH, 0, prev));
        existenceDict.put(Arrays.toString(curr_puzzle.getBoardCopy()), true);
        int num_moves = 0;
        try {
            while (!queue.isEmpty()) {
                num_moves++;
                ItemConfig itemConfig = queue.remove();
                Puzzle currPuzzle = itemConfig.getPuzzle();
                MoveLink currPrevMoveLink = itemConfig.getMl();
                if ((currPuzzle.getCurrentStatus() == 1)){
                    return currPrevMoveLink;
                }

                if (num_moves>=maxSize) {
                    if (currMinHMoves == null) {
                        return currPrevMoveLink; //randomness????
                    }
                    return currMinHMoves;
                }


                int[] allMoves = currPuzzle.allPossibleMove();
                for (int curr_move : allMoves) {
                    if (curr_move != -1) {
                        Puzzle newPuzzle = new Puzzle(currPuzzle);
                        newPuzzle.move(curr_move);

                        int[] board = newPuzzle.getBoardCopy(); // Width and blank symbol kept constant, only board changes
                        //instead of creating another method for hashing, we will keep it inside the scope of current method

                        String hashKey = Arrays.toString(board);
                        if (existenceDict.get(hashKey) == null) {
                            if (new Random().nextInt(100) < 99 ) { //add randomness to avoid stuck in local min
                                existenceDict.put(hashKey, true);
                            }
                            MoveLink nextMoveLink = new MoveLink(curr_move, currPrevMoveLink);
                            int currH = totalManhattan(board, orig_width);
                            if (currH < currMinH) {
                                currMinH = currH;
                                currMinHMoves = nextMoveLink;
                            }
                            queue.add(new ItemConfig(newPuzzle, currH,
                                    nextMoveLink.linkLength, nextMoveLink));

                        }
                    }
                }
            }
        } catch (OutOfMemoryError e) {
            return null;
        }
        //preemptively end the search due to the richness of existenceDict, needs to clear the history
        //instead of clearing history of dictionary, might consider use integer and setting quiet high priority
        existenceDict.clear();
        return prev;
    }

    /**
     * automatically solve sliding puzzle using A* search
     * @param orig_puzzle original puzzle passed
     * @return the moves required to solve the current puzzle
     */
    static int[]  autoSolve(Puzzle orig_puzzle) {
        // use Manhattan Distance as heuristic function
        int orig_width = orig_puzzle.getWidth();
        Comparator<ItemConfig> comparator = new ItemConfigComparator();
        PriorityQueue<ItemConfig> queue = new PriorityQueue<>(10, comparator);
        queue.add(new ItemConfig(orig_puzzle, totalManhattan(orig_puzzle.getBoardCopy(), orig_width), 0, null));
        HashMap<String,Boolean> existenceDict = new HashMap<>(); //avoids loops and repetitions
        while (!queue.isEmpty()) {
            ItemConfig itemConfig = queue.remove();
            Puzzle currPuzzle = itemConfig.getPuzzle();
            MoveLink currPrevMoveLink = itemConfig.getMl();
            if (currPuzzle.getCurrentStatus() == 1) {
                return toArray(currPrevMoveLink);
            }

            int[] allMoves = currPuzzle.allPossibleMove();
            for (int curr_move : allMoves) {
                if (curr_move != -1) {
                    Puzzle newPuzzle = new Puzzle(currPuzzle);
                    newPuzzle.move(curr_move);

                    int [] board = newPuzzle.getBoardCopy(); // Width and blank symbol kept constant, only board changes
                    //instead of creating another method for hashing, we will keep it inside the scope of current method

                    String hashKey = Arrays.toString(board);
                    if (existenceDict.get(hashKey) == null) {
                        existenceDict.put(hashKey, true);
                        MoveLink nextMoveLink = new MoveLink(curr_move, currPrevMoveLink);
                        queue.add(new ItemConfig(newPuzzle, totalManhattan(board, orig_width),
                                nextMoveLink.linkLength, nextMoveLink ));
                    }
                }
            }
        }
        throw new IllegalStateException("Unfortunately current state is inconsistent and puzzle can't be automatically solved, plz report this error! " +
                "Current puzzle is "+Arrays.toString(orig_puzzle.getBoardCopy())  +"With blank "+ orig_puzzle.getPosBlank());
    }

    /**
     * helper method for totalManhattan
     * @param firstPos: the first position on board
     * @param secondPos: the second position on board
     * @param width: the width of the board
     * @return the manhattan distance between firstPos and secondPos
     */
    private static int manhattan(int firstPos, int secondPos, int width) {
        int firstPosX = firstPos % width, firstPosY = firstPos / width;
        int secondPosX = secondPos % width, secondPosY = secondPos / width;
        return abs(firstPosX-secondPosX) + abs(firstPosY-secondPosY);
    }

    /**
     * helper method for auto_solve
     * @param config: the current board config
     * @param width: the width of the board
     * @return the total Manhattan distance between config and goal
     */
    private static int totalManhattan(int [] config, int width) {
        int distance = 0;
        for (int i = 0; i < config.length; i++) {
            distance += manhattan(i, config[i], width);
        }
        return distance;
    }

    private static class ItemConfigComparator implements Comparator<ItemConfig>
    {
        @Override
        public int compare(ItemConfig o1, ItemConfig o2) {
            return o1.getF()-o2.getF();
        }
    }

    /**
     * Helper class ItemConfig
     * A simple one time write structure used for storage in queue
     */
    private static class ItemConfig {
        private Puzzle puzzle;
        private int f;
        private MoveLink ml;
        ItemConfig(Puzzle puzzleParam, int hParam, int gParam, MoveLink moveLink) {
            puzzle = puzzleParam;
            f = hParam+gParam;
            ml = moveLink;
        }

        MoveLink getMl() {return ml;}
        int getF() {
            return f;
        }
        Puzzle getPuzzle() {
            return puzzle;
        }
    }
}
