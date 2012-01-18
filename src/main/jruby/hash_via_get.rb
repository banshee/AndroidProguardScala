require 'java'

class HashViaGet
  def initialize obj
    @obj = obj
  end

  def [] key
    case @obj
    when Hash, HashViaGet
      @obj[key]
    when Java::ScalaCollectionMutable::HashMap, Java::ScalaCollectionImmutable::HashMap::HashTrieMap
      result = @obj.get(key)
      result.get rescue result
    end
  end
end
