require 'pp'

h = {:a => {:b => 1, :c => 1}, :c => {:d => 1}}

def reverse_dependencies h
  result = Hash.new {|hash, k| hash[k] = []}
  h.each_pair do |k, v|
    v.keys.each do |v1|
      result[v1] << k
    end
  end
  result
end

pp h
puts "done"
pp (reverse_dependencies h)