

import org.jruby.Ruby;
import org.jruby.RubyObject;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.javasupport.JavaUtil;
import org.jruby.RubyClass;


public class HashViaGet extends RubyObject  {
    private static final Ruby __ruby__ = Ruby.getGlobalRuntime();
    private static final RubyClass __metaclass__;

    static {
        String source = new StringBuilder("require 'java'\n" +
            "\n" +
            "class HashViaGet\n" +
            "  def self.create_hash h\n" +
            "    result = {}\n" +
            "    require 'pp'\n" +
            "    h.keys.each do |k|\n" +
            "      result[k] = h.get(k)\n" +
            "    end\n" +
            "    result\n" +
            "  end\n" +
            "end\n" +
            "").toString();
        __ruby__.executeScript(source, "src/main/jruby/hash_via_get.rb");
        RubyClass metaclass = __ruby__.getClass("HashViaGet");
        metaclass.setRubyStaticAllocator(HashViaGet.class);
        if (metaclass == null) throw new NoClassDefFoundError("Could not load Ruby class: HashViaGet");
        __metaclass__ = metaclass;
    }

    /**
     * Standard Ruby object constructor, for construction-from-Ruby purposes.
     * Generally not for user consumption.
     *
     * @param ruby The JRuby instance this object will belong to
     * @param metaclass The RubyClass representing the Ruby class of this object
     */
    private HashViaGet(Ruby ruby, RubyClass metaclass) {
        super(ruby, metaclass);
    }

    /**
     * A static method used by JRuby for allocating instances of this object
     * from Ruby. Generally not for user comsumption.
     *
     * @param ruby The JRuby instance this object will belong to
     * @param metaclass The RubyClass representing the Ruby class of this object
     */
    public static IRubyObject __allocate__(Ruby ruby, RubyClass metaClass) {
        return new HashViaGet(ruby, metaClass);
    }
        
    /**
     * Default constructor. Invokes this(Ruby, RubyClass) with the classloader-static
     * Ruby and RubyClass instances assocated with this class, and then invokes the
     * no-argument 'initialize' method in Ruby.
     *
     * @param ruby The JRuby instance this object will belong to
     * @param metaclass The RubyClass representing the Ruby class of this object
     */
    public HashViaGet() {
        this(__ruby__, __metaclass__);
        RuntimeHelpers.invoke(__ruby__.getCurrentContext(), this, "initialize");
    }

    
    public static Object create_hash(Object h) {
        IRubyObject ruby_h = JavaUtil.convertJavaToRuby(__ruby__, h);
        IRubyObject ruby_result = RuntimeHelpers.invoke(__ruby__.getCurrentContext(), __metaclass__, "create_hash", ruby_h);
        return (Object)ruby_result.toJava(Object.class);

    }

}
