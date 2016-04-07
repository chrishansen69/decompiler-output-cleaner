import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.ui.refactoring.RenameSupport;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.body.VariableDeclaratorId;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class FernCleaner {

    public static void main(final String[] args) throws Exception {
        // creates an input stream for the file to be parsed
        final BufferedReader r = new BufferedReader(new FileReader("F51.java"));
        
        StringBuilder s = new StringBuilder();
        while(true) {
            String a = r.readLine();
            if (a == null) break;
            s.append(a);
        }
        
        r.close();
        
        

        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setSource(s.toString().toCharArray());

        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        //ASTNode node = parser.createAST(null);
        final CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        
        cu.accept(new ASTVisitor() {
 
            Set<String> names = new HashSet<String>();
 
            public boolean visit(VariableDeclarationFragment node) {
                SimpleName name = node.getName();
                this.names.add(name.getIdentifier());
                System.out.println("Declaration of '"+name+"' at line"+cu.getLineNumber(name.getStartPosition()));
                return false; // do not continue to avoid usage info
            }
 
            public boolean visit(SimpleName node) {
                if (this.names.contains(node.getIdentifier())) {
                System.out.println("Usage of '" + node + "' at line " + cu.getLineNumber(node.getStartPosition()));
                }
                return true;
            }
 
        });

        // prints the changed compilation unit
        System.out.println(cu.toString());
    }

    /**
     * Simple visitor implementation for visiting MethodDeclaration nodes.
     */
    private static class MethodChangerVisitor extends VoidVisitorAdapter<Object> {

        // i think this is maybe once per method so an arraylist field here storing variable names miiight work?

        @Override
        public void visit(final MethodDeclaration n, final Object arg) {
            // this needs to be called otherwise it fucks up all other method calls
            super.visit(n, arg);
            
            // change the name of the method to upper case
            //n.setName(n.getName().toUpperCase());

            final List<Parameter> params = new ArrayList<>(n.getParameters());

            for (int i = 0; i < params.size(); i++) {
                final Parameter param = params.get(i);
                final VariableDeclaratorId varDec = param.getId(); //var name
                System.out.println(param.getType()); //turns out this works, since it extends node which is... just a blob of text... who would have guessed

                final String parameterName = varDec.getName();
                if (parameterName.startsWith("var")) {
                    varDec.setName(param.getType() //var type
                    .toString().toLowerCase() + parameterName.substring("var".length()) //TODO better numbering system - per type instead of per var
                    );
                }
            }
            n.setParameters(params);

            // create the new parameter
            //Parameter newArg = ASTHelper.createParameter(ASTHelper.INT_TYPE, "value");

            // add the parameter to the method
            //ASTHelper.addParameter(n, newArg);
        }

        @Override
        public void visit(final VariableDeclarationExpr n, final Object arg) {
            // this needs to be called otherwise it fucks up all other method calls
            super.visit(n, arg);
            
            //System.out.println("got to expr");
            final List<VariableDeclarator> vars = n.getVars();

            for (int i = 0; i < vars.size(); i++) {
                VariableDeclarator varHandle = vars.get(i);
                final VariableDeclaratorId varDec = varHandle.getId(); //var name
                final String varName = varDec.getName();
                System.out.println("name: " + varName);
                if (varName.startsWith("var")) {
                    varDec.setName(n.getType() //var type
                    .toString().toLowerCase() + varName.substring("var".length()) //TODO better numbering system - per type instead of per var
                    );
                }
            }
            n.setVars(vars);

            //for (VariableDeclarator vars: myVars) {
            //    System.out.println("Variable Name: "+vars.getId().getName());
            //}
        }
        
        // TODO possibly visit(final NameExpr ?
    }
}
