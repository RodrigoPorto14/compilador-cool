package compiladorcool.semantic;

public class Node {
    private final NodeType type;
    private int level;
    private String value;
    private Parameter[] params;
    
    public Node(NodeType type, int level)
    {
        this.type=type;
        this.level=level;
    }
    
    public Node(NodeType type, int level, String value)
    {
        this.type=type;
        this.level=level;
        this.value=value;
    }
    
    public Node(NodeType type, int level, String value,Parameter[] params)
    {
        this.type=type;
        this.level=level;
        this.value=value;
        this.params=params;
    }
    
    public NodeType getType(){return type;}
    public int getLevel(){return level;}
    public String getValue(){return value;}
    public void setLevel(int level){ this.level=level; }
    public Parameter[] getParams(){ return params; }
    public void setParams(Parameter... params){ this.params=params; }
    public void setValue(String value) { this.value=value; }
    
}
