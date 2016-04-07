import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class FernCleaner {

    public static void main(final String[] args) throws Exception {
        // creates an input stream for the file to be parsed
        final BufferedReader r = new BufferedReader(new FileReader("F51.java"));

        final StringBuilder s = new StringBuilder();
        String a = null;
        while (a != null) {
            a = r.readLine();
            s.append(a);
        }

        r.close();
        System.out.println("got here1");

        final ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setSource(s.toString().toCharArray());

        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        //ASTNode node = parser.createAST(null);
        final CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        
        System.out.println("got here");

        cu.accept(new ASTVisitor() {

            Set<String> names = new HashSet<String>();

            @Override
            public boolean visit(final VariableDeclarationFragment node) {
                final SimpleName name = node.getName();
                names.add(name.getIdentifier());
                System.out.println("Declaration of '" + name + "' at line" + cu.getLineNumber(name.getStartPosition()));
                return false; // do not continue to avoid usage info
            }

            @Override
            public boolean visit(final SimpleName node) {
                if (names.contains(node.getIdentifier())) {
                    System.out.println("Usage of '" + node + "' at line " + cu.getLineNumber(node.getStartPosition()));
                }
                return true;
            }

        });

        // prints the changed compilation unit
        System.out.println(cu.toString());
    }
}
