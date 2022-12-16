package compiladorcool.codegeneration;

import java.util.ArrayList;

public class BrilProgram {
    
    private ArrayList<Func> functions;
    
    public BrilProgram()
    {
        functions = new ArrayList<>();
    }
    
    public ArrayList<Func> getFunctions() { return functions; }
}
