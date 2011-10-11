require 'java'

# You'll need to replace GEM_HOME with your own gem directory
ENV['GEM_HOME'] = "/Users/james/jrubygems"

# Replace with the output of pp $LOAD_PATH from jirb
$LOAD_PATH.concat ["/Users/james/bin/s3sync",
  "/Library/Frameworks/JRuby.framework/Versions/1.6.4/lib/ruby/site_ruby/1.8",
  "/Library/Frameworks/JRuby.framework/Versions/1.6.4/lib/ruby/site_ruby/shared",
  "/Library/Frameworks/JRuby.framework/Versions/1.6.4/lib/ruby/1.8",
  "."]

require 'rubygems'
require 'htmlentities'

# My FilterCallback class does the actual work of filtering and returning the string
class FilterCallback
  include  Java::ComRestphoneJrubyeclipse::IJrubyFilter
  def do_filter selection
    # Notice that we're OK with just throwing an exception here if the selection doesn't respond to
    # get_text.  That leaves the previous good selection in the text box.
    result = HTMLEntities.new.encode selection.get_text
    "<pre>\n#{result.strip}\n</pre>"
  end
end

# eclipse_callback is a special name.  Define an #eclipse_callback method in your file, and the plugin
# will call it to create an Java::ComRestphoneJrubyeclipse::IJrubyFilter object.  It must return
# an object that implements the Java::ComRestphoneJrubyeclipse::IJrubyFilter interface.
def eclipse_callback
  FilterCallback.new
end
