require 'java'

class HashViaGet
  def self.create_hash h
    result = {}
    require 'pp'
    h.keys.each do |k|
      result[k] = h.get(k)
    end
    result
  end
end
