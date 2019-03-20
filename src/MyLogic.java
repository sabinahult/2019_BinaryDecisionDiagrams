import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.JFactory;

/**
 * @author Sabina Hult
 * Solving the n-queens puzzle with the assistance of binary decision diagrams
 * as part of the course Intelligent Systems Programming, ITU 2019
 * @version 18.3.2019
 */

public class MyLogic implements IQueensLogic {
    // only square boards allowed
    private int size;
    // possible values: 0 (empty), 1 (queen), -1 (no queen allowed)
    private int[][] board;

    private BDDFactory fact;
    private BDD bdd;

    @Override
    public void initializeBoard(int size) {
        this.size = size;
        board = new int[size][size];
        bdd = buildBDDFromRules(size);
    }

    private BDD buildBDDFromRules(int n) {
        // initialize the factory with the parameters given in the assignment
        fact = JFactory.init(2000000, 200000);
        fact.setVarNum(n * n);

        // start with a BDD corresponding to True
        BDD bdd = fact.one();
        // add the one-queen-in-each-column rule
        bdd.andWith(nQueensRule());

        // for each variable add diagonal rule, horizontal rule and vertical rule
        for(int i = 0; i < size; i++) {
            for(int j = 0; j < size; j++) {
                bdd.andWith(diagonalRule(i, j));
                bdd.andWith(horizontalRule(i, j));
                bdd.andWith(verticalRule(i, j));
            }
        }

        System.out.println("Satisfying assignments: " + bdd.satCount());
        System.out.println("Number of nodes: " + bdd.nodeCount());
        return bdd;
    }

    /**
     * Returns the BDD corresponding to for all columns
     * exactly one row is true, else false
     */
    private BDD nQueensRule() {
        BDD oneInEachColumn = fact.one();

        // for every column
        for(int c = 0; c < size; c++) {
            // either it's false
            BDD column = fact.zero();
            for(int r = 0; r < size; r++) {
                // or one of the variables in the column are true
                column.orWith(fact.ithVar(convertToVarID(c, r)));
            }

            // the bdd for each column has to be true in order for the combined bdd to be true
            oneInEachColumn.andWith(column);
        }

        return oneInEachColumn;
    }

    /**
     * Returns the BDD corresponding to if x[c,r] then
     * not all other variables in both diagonals, else false
     */
    private BDD diagonalRule(int c, int r) {
        // set [c,r] to be true
        BDD bdd = fact.ithVar(convertToVarID(c, r));

        int left = c-1;
        int right = c+1;
        int up = r-1;
        int down = r+1;

        // at most size-1 variables in any given direction should be negated
        for(int i = 0; i < size; i++) {

            // fan out in both diagonals from [c,r] and set the variables to false
            if(left >= 0 && up >= 0) bdd.andWith(fact.nithVar(convertToVarID(left, up)));
            if(left >=0  && down < size) bdd.andWith(fact.nithVar(convertToVarID(left, down)));
            if(right < size && up >= 0) bdd.andWith(fact.nithVar(convertToVarID(right, up)));
            if(right < size && down < size) bdd.andWith(fact.nithVar(convertToVarID(right, down)));

            left--;
            right++;
            up--;
            down++;

        }

        // either all of the above is true, or [c,r] is false
        bdd.orWith(fact.nithVar(convertToVarID(c, r)));
        return bdd;
    }

    /**
     * Returns the BDD corresponding to if [c,r] then
     * not all other variables in the same same row, else false
     */
    private BDD horizontalRule(int c, int r) {
        // set [c,r] to be true
        BDD bdd = fact.ithVar(convertToVarID(c, r));

        // set all other in same row to be false
        for(int i = 0; i < size; i++) {
            if(i != c) {
                bdd.andWith(fact.nithVar(convertToVarID(i, r)));
            }
        }

        // either all of the above is true or [c,r] is false
        bdd.orWith(fact.nithVar(convertToVarID(c, r)));
        return bdd;
    }

    /**
     * Returns the BDD corresponding to if [c,r] then
     * not all other variables in the same column, else false
     */
    private BDD verticalRule(int c, int r) {
        // set [c,r] to be true
        BDD bdd = fact.ithVar(convertToVarID(c, r));

        // set all other in same col to be false
        for(int i = 0; i < size; i++) {
            if(i != r) {
                bdd.andWith(fact.nithVar(convertToVarID(c, i)));
            }
        }

        // either all of the above is true, or [c,r] is false
        bdd.orWith(fact.nithVar(convertToVarID(c, r)));
        return bdd;
    }

    @Override
    public int[][] getBoard() {
        return board;
    }

    @Override
    public void insertQueen(int column, int row) {
        // only insert if the position if empty
        if(board[column][row] == 0) {
            // add queen on the board
            board[column][row] = 1;

            // restrict corresponding variable to true
            bdd.restrictWith(fact.ithVar(convertToVarID(column, row)));
            updateBoard();
        }
    }

    /**
     * Convert the board position [c,r] to the corresponding variable
     * in the BDD
     */
    private int convertToVarID(int column, int row) {
        return row * size + column;
    }

    /**
     * Update the board such that positions that are rendered invalid after placing
     * a queen get's value -1
     */
    private void updateBoard() {
        for(int r = 0; r < size; r++) {
            for(int c = 0; c < size; c++) {
                // set an x?
                if(isPositionInvalid(c, r)) board[c][r] = -1;
                    // set a queen?
                else if(mustThereBeAQueen(c, r)) board[c][r] = 1;
            }
        }
    }

    /**
     * Check if adding a queen to [c,r] will make the bdd unsatisfiable
     */
    private boolean isPositionInvalid(int col, int row) {
        // place a queen at [col, row]
        BDD test = bdd.restrict(fact.ithVar(convertToVarID(col, row)));
        // does that make the bdd unsatisfiable?
        return test.isZero();
    }

    /**
     * Check if NOT adding a queen at [c,r] makes the BDD unsatisfiable
     */
    private boolean mustThereBeAQueen(int col, int row) {
        // do not place a queen at [col, row]
        BDD test = bdd.restrict(fact.nithVar(convertToVarID(col, row)));
        // does that make the bdd unsatisfiable?
        return test.isZero();
    }
}
