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

        bdd = createBDDFromRules(size);

        //System.out.println("Final BDD:");
        //fact.printTable(bdd);
    }

    private BDD createBDDFromRules(int n) {
        // TODO: Not enough to avoid a resize with n = 8 as the method is now...
        // But it works, hooray!
        fact = JFactory.init(200000, 200000);
        fact.setVarNum(n * n);

        // not necessary, but makes the rest more readable

        // the main BDD being built up as we go along
        BDD build = fact.one();

        // adding the one-queen-in-each-column rule
        BDD oneInEach = nQueensRule();
        build.andWith(oneInEach);

        // BDD with diagonal rule for all variables
        BDD diagonal = fact.one();
        // attempt at enforcing the diagonal restriction rule
        for(int r = 0; r < size; r++) {
            for(int c = 0; c < size; c++) {
                diagonal.andWith(diagonalRule(c, r));
            }
            //System.out.println();
        }

        // add diagonal rules into main BDD
        build.andWith(diagonal);

        // BDD with horizontal rule for all variables
        BDD horizontal = fact.one();
        // attempt at enforcing the horizontal restriction rule
        for(int r = 0; r < size; r++) {
            for(int c = 0; c < size; c++) {
                horizontal.andWith(horizontalRule(c, r));
            }
            //System.out.println();
        }

        // add horizontal rules into main BDD
        build.andWith(horizontal);

        BDD vertical = fact.one();
        // attempt at enforcing the vertical restriction rule
        for(int r = 0; r < size; r++) {
            for(int c = 0; c < size; c++) {
                vertical.andWith(verticalRule(c, r));
            }
            //System.out.println();
        }

        // add vertical rules into main BDD
        build.andWith(vertical);
        return build;
    }

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

            // combine rules for each column into one BDD
            oneInEachColumn.andWith(column);
        }

        return oneInEachColumn;
    }

    private BDD diagonalRule(int c, int r) {
        //System.out.println("Generating diagonal rule for ["+c+","+r+"]" + " Var: " + convertToVarID(c, r));

        // set [c,r] to be true
        BDD bdd = fact.ithVar(convertToVarID(c, r));

        int left = c-1;
        int right = c+1;
        int up = r-1;
        int down = r+1;

        // at most size-1 variables in any given direction should be negated
        // this is definitely not cache efficient, but correctness first,
        // then efficiency later (if time permits!)
        for(int i = 0; i < size; i++) {

            if(left >= 0 && up >= 0) {
                bdd.andWith(fact.nithVar(convertToVarID(left, up)));
                //System.out.print(String.format("LU: %d,%d ", left, up));
            }

            if(left >=0  && down < size) {
                bdd.andWith(fact.nithVar(convertToVarID(left, down)));
                //System.out.print(String.format("LD: %d,%d ", left, down));
            }

            if(right < size && up >= 0) {
                bdd.andWith(fact.nithVar(convertToVarID(right, up)));
                //System.out.print(String.format("RU: %d,%d ", right, up));
            }

            if(right < size && down < size) {
                bdd.andWith(fact.nithVar(convertToVarID(right, down)));
                //System.out.print(String.format("RD: %d,%d ", right, down));
            }

            left--;
            right++;
            up--;
            down++;

        }

        // either all of the above is true, or [c,r] is false
        bdd.orWith(fact.nithVar(convertToVarID(c, r)));

        //System.out.println("\nThe resulting BDD:");
        //fact.printTable(bdd);
        //System.out.println();

        return bdd;
    }

    private BDD horizontalRule(int c, int r) {
        //System.out.println("Generating horizontal rule for ["+c+","+r+"]" + " Var: " + convertToVarID(c, r));

        // set [c,r] to be true
        BDD bdd = fact.ithVar(convertToVarID(c, r));

        // set all other in same row to be false
        //System.out.print("Set these to false: ");
        for(int i = 0; i < size; i++) {
            if(i != c) {
                //System.out.print(convertToVarID(i, r) + " ");
                bdd.andWith(fact.nithVar(convertToVarID(i, r)));
            }
        }

        // either all of the above is true or [c,r] is false
        bdd.orWith(fact.nithVar(convertToVarID(c, r)));

        //System.out.println("\nThe resulting BDD:");
        //fact.printTable(bdd);
        //System.out.println();

        return bdd;
    }

    private BDD verticalRule(int c, int r) {
        //System.out.println("Generating vertical rule for ["+c+","+r+"]" + " Var: " + convertToVarID(c, r));

        // set [c,r] to be true
        BDD bdd = fact.ithVar(convertToVarID(c, r));

        // set all other in same col to be false
        //System.out.print("Set these to false: ");
        for(int i = 0; i < size; i++) {
            if(i != r) {
                //System.out.print(convertToVarID(c, i) + " ");
                bdd.andWith(fact.nithVar(convertToVarID(c, i)));
            }
        }

        // either all of the above is true, or [c,r] is false
        bdd.orWith(fact.nithVar(convertToVarID(c, r)));

        //System.out.println("\nThe resulting BDD:");
        //fact.printTable(bdd);
        //System.out.println();

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
     * Convert the board position [c, r] to the corresponding variable
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
                if(isPositionInvalid(c, r)) board[c][r] = -1; // set an x
                else if(mustThereBeAQueen(c, r)) board[c][r] = 1; // set a queen
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
