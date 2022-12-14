package compiladorcool.semantic;
import compiladorcool.semantic.Method;
import java.util.HashMap;
import java.util.Set;

public class ClassDescriptor {
    private final String inherits;
    private final HashMap<String,Method> methods = new HashMap<>();
    private final HashMap<String,String> attributes = new HashMap<>();
    
    public ClassDescriptor(String inherits)
    {
        this.inherits=inherits;
        
    }
    
    public String getInherits() { return inherits; }
    public Method getMethod(String method) { return methods.get(method); }
    public String typeOf(String attribute) { return attributes.get(attribute); }
    public Set<String> getAttributesKey() { return attributes.keySet(); }
    public HashMap<String,String> getAttributes() { return attributes; }
    
    public boolean hasMethod(String method){ return methods.containsKey(method); }
    public boolean hasAttribute(String attribute) { return attributes.containsKey(attribute); }
 
    public void addMethod(String name, String type, String... params) { methods.put(name, new Method(type,params)); }
    public void addAttribute(String name, String type) { attributes.put(name, type); }
    
    
}
