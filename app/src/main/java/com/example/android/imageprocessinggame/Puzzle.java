package com.example.android.imageprocessinggame;


/**
 * Created by Neo on 7/10/2017.
 */

import java.util.Arrays;

import static java.lang.Math.abs;

/**
 *  class Puzzle
 *  To help convert a board containing 0 to width*height-1 (one unique number each block and vice versa) into sorted board in row major order
 *  Puzzle doesn't record any move, it is only a representation of current state of board
 */
public class Puzzle {
    // It is a hard design decision to use a fixed goal state instead of use a start and goal state and convert board use mathematical transformations.
    // In order to save memory, we will only store a board. There unfortunately exists method that asks for board copy.
    // Therefore, it is impossible to give a copy of non-transformed board given only a transformed one. So we will let user convert start and goal.

    private int[] board;
    private int posBlank; //blank symbol can be any number under current implementation
    private int width;
    private int status;

    /**
     * Get the current status (Advised to check after construction and before each move to know if puzzle is already solved)
     * @return the current status of puzzle, 0 stands for normal, 1 for solved
     */
    public int getCurrentStatus() {
        return status;
    }

    /**
     * Getter method for width
     * @return current width
     */
    public int getWidth() {
        return width;
    }


    /**
     * Getter method of blank position
     * @return the current position of blank symbol
     */
    public int getPosBlank() {
        return posBlank;
    }




    /**]
     * Helper method: Called by constructor to validate input
     */
    private void validate() {
        if (width < 0) {
            throw new IllegalArgumentException("Width can't be Negative");
        }
        if ((width == 0) || (board.length == 0)) {
            throw new IllegalArgumentException("Board size and width must be greater than 0");
        }
        if ((board.length % width != 0)) {
            throw new IllegalArgumentException("width * height must be equal to board's size");
        }
        if ((posBlank < 0) || (posBlank >= board.length)) {
            throw new IllegalArgumentException("position of blank symbol must be in range of 0 to width*height-1");
        }

        int [] copy = board.clone();
        Arrays.sort(copy);
        for (int i = 0; i < copy.length; i++) {
            if (copy[i] != i) {
                throw new IllegalArgumentException("Unable to create puzzle: puzzle must contain 0 to width*height-1");
            }
        }

        int counter = 0;
        for (int i = 0; i < board.length-1; i++) {
            for (int j = i; j < board.length; j++) {
                if ((i != posBlank) && (j != posBlank))
                    counter += (board[i] > board[j]) ? 1 : 0;
            }
        }
        boolean counterEven = (counter%2==0);
        if (width % 2 == 0) {
            // pos = width*y+x, y = pos / width
            int targetY = board[posBlank]/width, currentY = posBlank/width;
            int diffY = abs(targetY-currentY);
            counterEven = (diffY % 2 == 0) == counterEven;
        }
        if (!counterEven) {
            throw new IllegalArgumentException("Unable to create puzzle: puzzle passed is unsolvable");
        }
    }

    /**
     * Helper method
     * @return if the current puzzle is already solved
     */
    private boolean solved() {
        for (int i = 0; i < board.length; i++) {
            if (board[i] != i) {
                return false;
            }
        }
        return true;
    }

    /**
     * Constructor for puzzle class.
     * @param oldBoard: the original configuration of the board
     * @param oldBlank: the original position of the special blank
     * @param width: The width of the board
     * @throws IllegalArgumentException: if puzzle is invalid or impossible to solve
     */
    public Puzzle(int[] oldBoard, int oldBlank, int width) {
        status = 0;
        board = oldBoard.clone();
        posBlank = oldBlank;
        this.width = width;
        validate();
        if (solved()) {
            status = 1;
        }
    }

    /**
     * Copy constructor for Puzzle class
     * @param puzzle puzzle to be copied
     */
    public Puzzle(Puzzle puzzle) {
        this.board = puzzle.board.clone();
        this.posBlank = puzzle.posBlank;
        this.width = puzzle.width;
        this.status = puzzle.status;
    }

    /**
     * Getter for board
     * @return a copy of current board
     */
    public int[] getBoardCopy() {
        return board.clone();
    }

    /**
     * Helper method: return if the player is allowed to move the tile at position pos to blank pos in given config
     * @param pos: position of the title wanted to move
     * @return if the player can move at position
     */
    private boolean canMove(int pos) {
        int diff = abs(posBlank-pos);
        int diff_y = abs(posBlank/width-pos/width);
        return (diff==1 && diff_y==0) || (diff==width && diff_y== 1);
    }

    /**
     * Method: called when you want to move the tile at position pos
     * @param pos: the position you want to move (swap with blank)
     * @return if the move the success
     */
    public boolean move(int pos) {
        if (canMove(pos)) {
            int temp = board[pos];
            board[pos] = board[posBlank];
            board[posBlank] = temp;

            posBlank = pos;
            status = solved() ? 1 : 0;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Method: to give a hint of all possible move available
     * @return an arrays of size 4 symbolized position of move in order of left, right, up, down. -1 if not possible
     */
    public int[] allPossibleMove() {
        int[] return_val = new int [4];
        return_val[0] = ((posBlank % width != 0)? posBlank-1 : -1);
        return_val[1] = ((posBlank % width != width-1)? posBlank+1 : -1);
        return_val[2] = ((posBlank / width != 0)? posBlank-width : -1);
        return_val[3] = ((posBlank / width != (board.length/width)-1)? posBlank+width : -1);
        return return_val;
    }
}

