import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.body.VariableDeclaratorId;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class FernCleaner {

    public static void main(final String[] args) throws FileNotFoundException, ParseException, IOException {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
        }

        final JFileChooser chooser = new JFileChooser();
        final FileNameExtensionFilter filter = new FileNameExtensionFilter("Java source code", "java");
        chooser.setFileFilter(filter);
        chooser.setMultiSelectionEnabled(true);
        final int returnVal = chooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            for (final File f : chooser.getSelectedFiles()) {
                writeToFile(f);
            }
        } else {
            writeToFile(new File("./F51.java"));
        }

    }

    private static void writeToFile(final File f) throws FileNotFoundException, ParseException, IOException {
        final FileInputStream in = new FileInputStream(f);

        CompilationUnit cu;
        try {
            // parse the file
            cu = JavaParser.parse(in);
        } finally {
            in.close();
        }

        // visit and change the methods names and parameters
        new MethodChangerVisitor().visit(cu, null);

        // prints the changed compilation unit
        //System.out.println(cu.toString());

        f.delete();
        final BufferedWriter pw = new BufferedWriter(new PrintWriter(new FileOutputStream(f)));
        pw.write(cu.toString());
        pw.close();
    }

    /**
     * @author rafa1231518, borrowing as much as possible from the examples for JavaParser
     */
    private static class MethodChangerVisitor extends VoidVisitorAdapter<Object> {

        // i did all of this shit by trial and error, don't complain if something's seriously wrong
        
        // TODO: no support for constructors yet

        /**
         * Stores current variable names in the order {@literal <PREVIOUS, REPLACEMENT>}
         */
        private final HashMap<String, String> variablesMap = new HashMap<>();

        /**
         * Stores the amount of variables in the order {@literal <TYPE, AMOUNT>}
         */
        private final HashMap<String, Integer> lastVarIndex = new HashMap<>();

        /**
         * True if inside a method, enables the variable name changing
         */
        private boolean inMethod = false;

        /**
         * Short names for ints (i, j, k, l, i1, j1, k1, l1, ...) only these four are used to avoid name conflicts (for example, there should not be m, t, x, y, z...)
         */
        private final static String[] INT_SHORTNAMES = {
                "i", "j", "k", "l"
        };

        @Override
        public void visit(final MethodDeclaration n, final Object arg) {
            // this needs to be called otherwise it fucks up all other method calls
            super.visit(n, arg);

            // a new method, a fresh start
            variablesMap.clear();
            lastVarIndex.clear();

            final List<Parameter> params = new ArrayList<>(n.getParameters());

            for (final Parameter param : params) {
                final VariableDeclaratorId varDec = param.getId(); //var name
                System.out.println("type of parameter: " + param.getType()); //turns out this works, since it extends node which is... just a blob of text... who would have guessed

                final String parameterName = varDec.getName();
                if (parameterName.startsWith("var")) { //if it's fernflower output
                    varDec.setName(putIfNotExists(param, parameterName));
                }
            }
            n.setParameters(params);

            inMethod = true;
            visit(n.getBody(), arg);
            visit(n.getBody(), arg); //parse twice for the VariableDeclarationExpr calls to be valid for the NameExpr, don't ask me why tihs works :P
            inMethod = false;
        }

        @Override
        public void visit(final VariableDeclarationExpr n, final Object arg) {

            if (!inMethod)
                return;

            // this needs to be called otherwise it fucks up all other method calls
            super.visit(n, arg);

            //System.out.println("got to expr");
            final List<VariableDeclarator> vars = n.getVars();

            for (final VariableDeclarator varHandle : vars) {
                final VariableDeclaratorId varDec = varHandle.getId(); //var name
                final String varName = varDec.getName();
                System.out.println("declarated name: " + varName);
                if (varName.startsWith("var")) { //if it's fernflower output
                    varDec.setName(putIfNotExists(n, varName));
                }
            }
            n.setVars(vars);

        }

        @Override
        public void visit(final NameExpr n, final Object arg) {

            if (!inMethod)
                return;

            super.visit(n, arg);

            final String a = variablesMap.get(n.getName());
            if (a == null || n.getName().length() == 0)
                return;

            n.setName(a);

        }

        /**
         * Gives you an unique and nifty variable name and adds it to variablesMap if it's not there already.
         *
         * @param n The node containing the variable.
         * @param varName The variable's name (redundant, FIXME)
         * @return an unique and nifty variable name
         */
        private String putIfNotExists(final VariableDeclarationExpr n, final String varName) {
            return putIfNotExists(n, varName, n.getType().toString());
        }

        /**
         * Gives you an unique and nifty variable name and adds it to variablesMap if it's not there already.
         *
         * @param n The node containing the variable.
         * @param varName The variable's name (redundant, FIXME)
         * @return an unique and nifty variable name
         */
        private String putIfNotExists(final Parameter n, final String varName) {
            return putIfNotExists(n, varName, n.getType().toString());
        }

        /**
         * Gives you an unique and nifty variable name and adds it to variablesMap if it's not there already.
         *
         * @param n The node containing the variable.
         * @param varName The variable's name (redundant, FIXME)
         * @param type the output of <code>n.getType().toString()</code>, required for reasons
         * @return an unique and nifty variable name
         */
        private String putIfNotExists(final Node n, final String varName, final String type) {
            String a = variablesMap.get(varName);
            if (a == null) {
                a = fixArray(getTypeAndIncrement(type));
                variablesMap.put(varName, a);
            }
            return a;
        }

        /**
         * Fixes a string containing []
         * 
         * @param a string
         * @return a fixed string
         */
        private static String fixArray(final String a) {
            return a.replaceFirst("\\[\\]", "s").replace("[]", ""); //careful with array types when adding better numbering system because of 2-dimensional arrays
        }

        private String getTypeAndIncrement(final String type) {
            incrementIfNotExists(type);

            return getVarIndex(lastVarIndex.get(type), type.toLowerCase());
        }

        /**
         * Gives you a nifty name for a variable based on whatever type you give it<br>
         * <br>
         * <b>{@literal << WARNING >>}</b><br>
         * This method will likely not handle {@literal <?>} properly.<br>
         * This method will likely not handle two multi-dimensional arrays (one 1-dimensional, one 2-dimensional...) of the same type properly, since they will be considered different types. You'll need to fix {@link #fixArray(String)} yourself.
         * 
         * @param varIndex preferably use with {@link #lastVarIndex}
         * @param type the type of the variable (int, long, short, String, ExpandDong, etc...)
         * @return a nifty name for a variable
         */
        private String getVarIndex(final int varIndex, final String type) {
            if (type.equals("int")) {
                if (varIndex <= INT_SHORTNAMES.length)
                    return INT_SHORTNAMES[varIndex - 1];
                else
                    return INT_SHORTNAMES[(varIndex - 1) % INT_SHORTNAMES.length] + varIndex / INT_SHORTNAMES.length;
            } else if (varIndex == 1) {
                if (type.equals("byte"))
                    return "b";
                else if (type.equals("long"))
                    return "lo";
                else if (type.equals("short"))
                    return "s";
                else if (type.equals("float"))
                    return "f";
                else if (type.equals("double"))
                    return "d";
                else if (type.equals("boolean"))
                    return "flag";
                else if (type.equals("char"))
                    return "ch";
                else
                    return type;
            } else {
                if (type.equals("byte"))
                    return "b" + varIndex;
                else if (type.equals("long"))
                    return "lo" + varIndex;
                else if (type.equals("short"))
                    return "s" + varIndex;
                else if (type.equals("float"))
                    return "f" + varIndex;
                else if (type.equals("double"))
                    return "d" + varIndex;
                else if (type.equals("boolean"))
                    return "flag" + varIndex;
                else if (type.equals("char"))
                    return "ch" + varIndex;
                else
                    return type + varIndex;
            }
        }

        /**
         * Increments lastVarIndex. Meaningless code, really, but I don't care
         * 
         * @param type The type of the variable to be incremented
         */
        private void incrementIfNotExists(final String type) {
            final Integer vType = lastVarIndex.get(type);
            if (vType != null) {
                lastVarIndex.put(type, vType + 1);
            } else {
                lastVarIndex.put(type, 1);
            }
        }
    }
}
