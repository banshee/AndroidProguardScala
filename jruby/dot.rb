require 'rubygems'
require 'pp'

def hash_to_dot_entries h
  result = []
  h.each_pair do |k, v|
    v.each do |v1|
      result << "#{k} -> #{v1}"
    end
  end
  result
end

h = {
  :a => %w(b c d),
  :b => %w(d e f)
}
r = hash_to_dot_entries h
puts "digraph {"
puts r.join ";\n"
puts "}"

#digraph foo {
#  bar -> snark
#}