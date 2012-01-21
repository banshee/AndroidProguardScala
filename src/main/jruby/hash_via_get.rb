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

  def self.create_hash h
    result = {}
    require 'pp'
    h.keys.each do |k|
      result[k] = h.get(k)
    end
    result
  end
end
