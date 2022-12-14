package compiladorcool.syntactic;

import compiladorcool.Analyzer;
import compiladorcool.Error;
import compiladorcool.semantic.Node;
import compiladorcool.semantic.NodeType;
import compiladorcool.semantic.Parameter;
import compiladorcool.lexical.TokenType;
import compiladorcool.lexical.Token;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Queue;
import java.util.LinkedList;

public class SyntacticAnalyzer extends Analyzer{
    
    private final ArrayList<Error> errors;
    private final ArrayList<TokenType> firstExpr,firstExpr2;
    private final Queue<Node> syntacticTree;
    private final ArrayList<Node> nodesBuffer;
    private Token lastToken;
    private String currentClass;
    private boolean chainStarted=false;
    private int chainPos;
    private final ArrayList<Integer> nodesId = new ArrayList<>();
    
    public SyntacticAnalyzer(ArrayList<Token> tokens, ArrayList<Error> errors)
    {
        super(tokens);
        this.errors=errors;
        firstExpr = new ArrayList<>();
        firstExpr2 = new ArrayList<>();
        syntacticTree = new LinkedList<>();
        nodesBuffer = new ArrayList<>();
        addFirstExpr();
    }
    
    public Queue<Node> analyze()
    {
        program(0);
        dumpBuffer();
        return syntacticTree;
    }
    
    private void program(int nodeLvl)
    {
        try
        {
            do
            {
                _class(nodeLvl+1);
                match(TokenType.SEMICOLON);
            }
            while(lookAHead(1)!=null);
        }
        catch(SyntacticException e){nodesBuffer.clear();}
    }
    
    private void _class(int nodeLvl) throws SyntacticException
    {
        try
        {
            match(TokenType.CLASS);
            match(TokenType.TYPE_IDENTIFIER);
            currentClass = lastToken.getDescription();
            saveNode(NodeType.CLASS,nodeLvl,currentClass);
            if(nextTokenIs(TokenType.INHERITS))
            {
                match(TokenType.INHERITS);
                match(TokenType.TYPE_IDENTIFIER);
            }
            match(TokenType.OPEN_BRACES);
            while(nextTokenIs(TokenType.OBJECT_IDENTIFIER))
            {
                feature(nodeLvl+1);
                match(TokenType.SEMICOLON);
            }
            match(TokenType.CLOSE_BRACES);
        }
        catch(SyntacticException e){throw e;}    
    }
    
    private void feature(int nodeLvl) throws SyntacticException
    {
        try 
        {
            match(TokenType.OBJECT_IDENTIFIER);
            String id = lastToken.getDescription();
            if(nextTokenIs(TokenType.OPEN_PARENTHESES)) method(nodeLvl,id);
            else if(nextTokenIs(TokenType.COLON)) attribute(nodeLvl,id);
            else createError(TokenType.OPEN_PARENTHESES,TokenType.COLON);
        }
        catch(SyntacticException e){throw e;}
    }
    
    private void method(int nodeLvl,String id) throws SyntacticException
    {
        
        try
        {
            ArrayList<Parameter> params = new ArrayList<>();
            match(TokenType.OPEN_PARENTHESES);
            if(nextTokenIs(TokenType.OBJECT_IDENTIFIER))
            {
                params.add(formal());
                while(nextTokenIs(TokenType.COMMA))
                {
                    match(TokenType.COMMA);
                    params.add(formal());
                }
            }
            saveNode(NodeType.METHOD,nodeLvl,id,params.toArray(Parameter[]::new));
            match(TokenType.CLOSE_PARENTHESES);
            match(TokenType.COLON);
            match(TokenType.TYPE_IDENTIFIER);
            match(TokenType.OPEN_BRACES);
            expr(nodeLvl+1);
            match(TokenType.CLOSE_BRACES);
        }
        catch(SyntacticException e){throw e;}  
    }
    
    private void attribute(int nodeLvl,String id) throws SyntacticException
    {
        saveNode(NodeType.ATTRIBUTE,nodeLvl,id);
        try
        {
            match(TokenType.COLON);
            match(TokenType.TYPE_IDENTIFIER);
            if(nextTokenIs(TokenType.ASSIGN))
            {
                match(TokenType.ASSIGN);
                expr(nodeLvl+1);
            }
        }
        catch(SyntacticException e){throw e;}
    }
    
    private Parameter formal() throws SyntacticException
    {   
        try
        {
            match(TokenType.OBJECT_IDENTIFIER);
            String name = lastToken.getDescription();
            match(TokenType.COLON);
            match(TokenType.TYPE_IDENTIFIER);
            String type = lastToken.getDescription();
            return new Parameter(name,type);
        }
        catch(SyntacticException e){throw e;}
    }
    
    private void expr(int nodeLvl) throws SyntacticException
    {
        //System.out.println("EXPR"+" "+lookAHead(1).getDescription());
        //boolean bufferIsEmpty = nodesBuffer.isEmpty();
        try
        {
            int bufferPos = nodesBuffer.size();
            //if(!nextTokenIn(firstExpr2,1)) dumpBuffer();
            if(nextTokenIn(firstExpr,1))
            {
                Token tk = lookAHead(1);
                switch(tk.getType())
                {
                    case OBJECT_IDENTIFIER -> id(nodeLvl);
                    case IF -> _if(nodeLvl);
                    case WHILE -> _while(nodeLvl);
                    case OPEN_BRACES -> block(nodeLvl);
                    case LET -> let(nodeLvl);
                    case CASE -> _case(nodeLvl);
                    case NEW -> { match(TokenType.NEW); match(TokenType.TYPE_IDENTIFIER); saveNode(NodeType.NEW,nodeLvl,lastToken.getDescription()); }
                    case ISVOID -> { saveNode(NodeType.ISVOID,nodeLvl);match(TokenType.ISVOID); expr(nodeLvl+1); }
                    case COMPLEMENT -> { saveNode(NodeType.COMPLEMENT,nodeLvl); match(TokenType.COMPLEMENT); expr(nodeLvl+1); }
                    case NOT -> { saveNode(NodeType.NOT,nodeLvl); match(TokenType.NOT); expr(nodeLvl+1); }
                    case OPEN_PARENTHESES -> { saveNode(NodeType.AMONG_PARENTHESES,nodeLvl); match(TokenType.OPEN_PARENTHESES); expr(nodeLvl+1); match(TokenType.CLOSE_PARENTHESES); }
                    case INTEGER -> { saveNode(NodeType.INTEGER,nodeLvl,tk.getDescription()); match(TokenType.INTEGER); }
                    case STRING -> { saveNode(NodeType.STRING,nodeLvl,tk.getDescription()); match(TokenType.STRING); }
                    case TRUE -> { saveNode(NodeType.BOOL,nodeLvl,tk.getDescription()); match(TokenType.TRUE); }
                    case FALSE -> { saveNode(NodeType.BOOL,nodeLvl,tk.getDescription()); match(TokenType.FALSE); }

                }
                
            }
            else createError(TokenType.EXPR);       
            while(nextTokenIn(firstExpr2,1)) expr2(nodeLvl,bufferPos);
        }
        catch(SyntacticException e){throw e;}
    }
    
    private void expr2(int nodeLvl,int bufferPos) throws SyntacticException
    {
        //System.out.println("EXPR2"+" "+lookAHead(1).getDescription());
        try
        {
            if(nextTokenIs(TokenType.AT))
            {
                match(TokenType.AT);
                match(TokenType.TYPE_IDENTIFIER);
                String type = lastToken.getDescription();
                match(TokenType.DOT);
                match(TokenType.OBJECT_IDENTIFIER);
                String name = lastToken.getDescription();
                upBuffer(1,bufferPos);
                Node node = saveNodeAt(NodeType.DISPATCH,nodeLvl,bufferPos);
                node.setParams(new Parameter(name,type));
                methodCall(nodeLvl);          
                //fixPrecedence(NodeType.DISPATCH,nodeLvl,bufferPos);
                //if(comparePrecedence(TokenType.AT)) {System.out.println("ENTROUU"); dumpBuffer();}
            }
            else if(nextTokenIs(TokenType.DOT))
            {
                match(TokenType.DOT);
                match(TokenType.OBJECT_IDENTIFIER);
                String name = lastToken.getDescription();
                upBuffer(1,bufferPos);
                Node node = saveNodeAt(NodeType.DISPATCH,nodeLvl,bufferPos);
                node.setParams(new Parameter(name,currentClass));
                methodCall(nodeLvl);
                //fixPrecedence(NodeType.DISPATCH,nodeLvl,bufferPos);
                //if(comparePrecedence(TokenType.DOT)) {System.out.println("ENTROUU"); dumpBuffer();}
                
            }
            else
            {
               boolean enter = false;
               Token tk = lookAHead(1);
               TokenType tokenType = tk.getType();
               NodeType type;
               match(tokenType);
               if(tokenType==TokenType.ADD || tokenType==TokenType.SUB) type = NodeType.ARITHMETIC;
               else if(tokenType==TokenType.MULT || tokenType==TokenType.DIV) type = NodeType.ARITHMETIC2;
               else if(tokenType==TokenType.LT || tokenType==TokenType.LTE) type = NodeType.COMPARE;
               else type = NodeType.EQUAL;
               upBuffer(1,bufferPos);
               saveNodeAt(type,nodeLvl,tk.getDescription(),bufferPos);
               if(!chainStarted) {chainPos=bufferPos; enter=true; chainStarted=true; }
               expr(nodeLvl+1);
               if(enter && chainStarted) chainStarted=false;
               fixPrecedence(type,nodeLvl,tk.getDescription(),bufferPos);
               //if(comparePrecedence(tk)) {System.out.println("ENTROUU"); dumpBuffer();}
            }           
        }
        catch(SyntacticException e){throw e;}
    }
    
    private void id(int nodeLvl) throws SyntacticException
    {
        try
        {
            Token tk = lookAHead(1);
            match(TokenType.OBJECT_IDENTIFIER);
            if(nextTokenIs(TokenType.OPEN_PARENTHESES)) 
            { 
                Node node = saveNode(NodeType.METHOD_CALL,nodeLvl); 
                node.setParams(new Parameter(tk.getDescription(),currentClass)); 
                methodCall(nodeLvl); 
            }
            else if(nextTokenIs(TokenType.ASSIGN)) { saveNode(NodeType.ASSIGNMENT,nodeLvl,tk.getDescription()); assignment(nodeLvl); }
            else saveNode(NodeType.ID,nodeLvl,tk.getDescription());
        }
        catch(SyntacticException e){throw e;}
    }
    
     private void methodCall(int nodeLvl) throws SyntacticException
    {
        try
        {
            match(TokenType.OPEN_PARENTHESES);
            if(nextTokenIn(firstExpr,1))
            {
                expr(nodeLvl+1);
                while(nextTokenIs(TokenType.COMMA))
                {
                    match(TokenType.COMMA);
                    expr(nodeLvl+1);
                }
            }
            match(TokenType.CLOSE_PARENTHESES); 
        }
        catch(SyntacticException e){throw e;}
    }
    
    private void assignment(int nodeLvl) throws SyntacticException
    {
        try
        {
            match(TokenType.ASSIGN);
            expr(nodeLvl+1);
        }
        catch(SyntacticException e){throw e;}
    }
    
    private void _if(int nodeLvl) throws SyntacticException
    {
        saveNode(NodeType.IF,nodeLvl);
        try
        {
            match(TokenType.IF);
            expr(nodeLvl+1);
            match(TokenType.THEN);
            expr(nodeLvl+1);
            match(TokenType.ELSE);
            expr(nodeLvl+1);
            match(TokenType.FI);
        }
        catch(SyntacticException e){throw e;} 
    }
    
    private void _while(int nodeLvl) throws SyntacticException
    {
        saveNode(NodeType.WHILE,nodeLvl);
        try
        {
            match(TokenType.WHILE);
            expr(nodeLvl+1);
            match(TokenType.LOOP);
            expr(nodeLvl+1);
            match(TokenType.POOL);
        }
        catch(SyntacticException e){throw e;} 
    }
    
    private void block(int nodeLvl) throws SyntacticException
    {
        saveNode(NodeType.SEQUENCE,nodeLvl);
        try
        {
            match(TokenType.OPEN_BRACES);
            do
            {
                expr(nodeLvl+1);
                match(TokenType.SEMICOLON);
            }
            while(nextTokenIn(firstExpr,1));
            match(TokenType.CLOSE_BRACES);
        }
        catch(SyntacticException e){throw e;} 
    }
    
    private void let(int nodeLvl) throws SyntacticException
    {
        ArrayList<Parameter> params = new ArrayList<>();
        int assignments=0;
        Node node = saveNode(NodeType.LET,nodeLvl);
        boolean secondLoop = false;
        try
        {
            match(TokenType.LET);
            do
            {
                if(secondLoop) match(TokenType.COMMA);
                params.add(formal());
                if(nextTokenIs(TokenType.ASSIGN))
                {
                    assignments++;
                    match(TokenType.ASSIGN);
                    expr(nodeLvl+1);
                }
                secondLoop=true;
            }            
            while(nextTokenIs(TokenType.COMMA));
            node.setParams(params.toArray(Parameter[]::new));
            node.setValue(""+assignments);
            match(TokenType.IN);
            expr(nodeLvl+1);
        }
        catch(SyntacticException e){throw e;}
    }
    
    private void _case(int nodeLvl) throws SyntacticException
    {
        saveNode(NodeType.CASE,nodeLvl);
        try
        {
            match(TokenType.CASE);
            expr(nodeLvl+1);
            match(TokenType.OF);
            do
            { 
                match(TokenType.OBJECT_IDENTIFIER);
                match(TokenType.COLON);
                match(TokenType.TYPE_IDENTIFIER);
                match(TokenType.ARROW);
                expr(nodeLvl+1);
                match(TokenType.SEMICOLON);
            }while(nextTokenIs(TokenType.OBJECT_IDENTIFIER));
            match(TokenType.ESAC);   
        }
        catch(SyntacticException e){throw e;} 
    }
    
    // ================================================================================================================================================ //
    //                                                             METODOS AUXILIARES                                                                   //
    // ================================================================================================================================================ //
    
    @Override
    public Token nextToken()
    {
        if(tokenId<tokens.size()) 
        {
            lastToken = tokens.get(tokenId);
            tokenId++;
        }
        return lastToken;
    }
    
    private boolean nextTokenIs(TokenType type) {return lookAHead(1)!=null && lookAHead(1).getType()==type;}
    
    private boolean nextTokenIn(ArrayList<TokenType> typeArray,int k) {return lookAHead(k)!=null && typeArray.contains(lookAHead(k).getType());}
    
    private void match(TokenType type) throws SyntacticException
    {
        if(nextTokenIs(type)) nextToken();
        else
        {
            createError(type);
            throw new SyntacticException();
        }
    }
    
    private void createError(TokenType... types)
    {
        if(lastToken!=null) errors.add(new SyntacticError(lastToken.getDescription(),lastToken.getRow(),types));
        else errors.add(new SyntacticError("inicio do arquivo",1,types));
        ignoreUntil(TokenType.SEMICOLON);       
    }
     
    private void ignoreUntil(TokenType type)
    {  
        while(lookAHead(1)!=null && lookAHead(1).getType()!=type) nextToken();
    }
    
    private void addFirstExpr()
    {
        TokenType[] firstExprArray = {TokenType.OBJECT_IDENTIFIER,TokenType.IF,TokenType.WHILE,TokenType.OPEN_BRACES,TokenType.LET,
                                      TokenType.CASE,TokenType.NEW,TokenType.ISVOID,TokenType.COMPLEMENT,TokenType.NOT,
                                      TokenType.OPEN_PARENTHESES,TokenType.INTEGER,TokenType.STRING,TokenType.TRUE,TokenType.FALSE};
        
        TokenType[] firstExprArray2 = {TokenType.ADD,TokenType.SUB,TokenType.MULT,TokenType.DIV,TokenType.LT,TokenType.LTE,TokenType.EQUAL,TokenType.AT,TokenType.DOT};
        
        firstExpr.addAll(Arrays.asList(firstExprArray));
        firstExpr2.addAll(Arrays.asList(firstExprArray2));
    }
    
    private void addNodesId(int pos, int lvl)
    {
        nodesId.add(pos);
        pos++;
        lvl++;
        while(nodesBuffer.size()>pos && nodesBuffer.get(pos).getLevel()>=lvl)
        {
            if(nodesBuffer.get(pos).getLevel()==lvl) addNodesId(pos,lvl);
            pos++;
        }  
    }
    
    private void upNodes(int add)
    {
        for(var id : nodesId) nodesBuffer.get(id).setLevel(nodesBuffer.get(id).getLevel()+add);
        nodesId.clear();
    }
    
    private int nextNodePos(int pos, int lvl)
    {
        while(nodesBuffer.size()>pos)
        {
            if(nodesBuffer.get(pos).getLevel()==lvl+1) return pos;
            pos++;
        }
        return 0;
    }
    
    private void fixPrecedence(NodeType type,int nodeLvl,String nodeValue,int pos)
    {
        //System.out.println(nodeValue);
        HashMap<NodeType,Integer> precedence = new HashMap<>();
        precedence.put(NodeType.EQUAL,1); precedence.put(NodeType.COMPARE,1);
        precedence.put(NodeType.ARITHMETIC,2);
        precedence.put(NodeType.ARITHMETIC2,3);
        
        for(int i=pos-1;i>=chainPos;i--)
        {
            Node node = nodesBuffer.get(i);
            //System.out.println(type+" "+node.getType());
            if(node.getLevel()==nodeLvl-1 && precedence.containsKey(node.getType()) && precedence.get(type) <= precedence.get(node.getType()))
            {
                addNodesId(i+1,node.getLevel()+1);
                upNodes(1);
                node.setLevel(node.getLevel()+1);
                addNodesId(nextNodePos(pos+2,nodeLvl),nodeLvl+1);
                upNodes(-1);
                saveNodeAt(type,nodeLvl-1,nodeValue,i);
                nodesBuffer.remove(++pos);
                pos=i;
                nodeLvl--;
            }  
        }  
    }
         
    private Node saveNode(NodeType nodeType, int nodeLvl) { Node node = new Node(nodeType,nodeLvl); nodesBuffer.add(node); return node;}
    private void saveNode(NodeType nodeType, int nodeLvl, String nodeValue) { nodesBuffer.add(new Node(nodeType,nodeLvl,nodeValue)); }
    private void saveNode(NodeType nodeType, int nodeLvl, String nodeValue, Parameter[] params) { nodesBuffer.add(new Node(nodeType,nodeLvl,nodeValue,params)); }
    private Node saveNodeAt(NodeType nodeType, int nodeLvl, int pos){ Node node = new Node(nodeType,nodeLvl) ; nodesBuffer.add(pos, node); return node; }
    private void saveNodeAt(NodeType nodeType, int nodeLvl, String nodeValue, int pos){ nodesBuffer.add(pos, new Node(nodeType,nodeLvl,nodeValue)); }
    private void upBuffer(int lvl,int pos)
    {
        for(int i=pos;i<nodesBuffer.size();i++) nodesBuffer.get(i).setLevel(nodesBuffer.get(i).getLevel()+lvl);
    }
    private void dumpBuffer()
    {
        syntacticTree.addAll(nodesBuffer);
        nodesBuffer.clear();
    }
}
